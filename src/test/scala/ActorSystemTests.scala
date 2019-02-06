import java.io.File
import java.nio.file.Files

import akka.actor.{ActorSystem, Props}
import akka.routing.{Broadcast, RoundRobinPool}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.qimia.xmlLoader.actor.{DoneMsg, PostsBatch, SaveBatchCsvActor}
import com.qimia.xmlLoader.model.{Post, PostsBatchMsg}
import com.qimia.xmlLoader.util.{Config, FileLoadBalance, StopWordsBloomFilter}
import org.scalatest._

class ActorSystemTests extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  var posts: PostsBatchMsg = _

  var config:Config = Config()

  override def beforeAll(){
    initArguments
    FileLoadBalance.init(config)
    StopWordsBloomFilter.init(config.stopWordsPath)
    posts = new PostsBatchMsg
    posts.addPost(new Post("1", "title", "body", "tags"))
  }

  "Output " must {"have 1828 rows" in{
    /**
      * This works on the 3dprinting stackoverflow
      */
    com.qimia.xmlLoader.XmlLoader.run(config)
    Thread.sleep(10000)
    val numRows = new File(config.outputPath).listFiles
        .map(io.Source.fromFile(_).getLines.size)
      .sum
    numRows shouldBe 1828

  }}

  "Save Actor Test" must {
    "Send Batch to Actor" in {
      val actorRef = TestActorRef(Props(new SaveBatchCsvActor(config)))

      actorRef ! PostsBatch(posts)
      expectMsg(DoneMsg)
      cleanOutput
    }
  }

  "Actor Pool Test" must {
    "Send batches to pool" in {
      val router = system.actorOf(RoundRobinPool(3).props(Props(new SaveBatchCsvActor(config))), name = "savePool")

      router ! Broadcast(PostsBatch(posts))
      assert(3 == receiveN(3).length)
      cleanOutput
    }
  }

  "Save Actor Test" must {
    "create output file" in {
      val actorRef = TestActorRef(Props(new SaveBatchCsvActor(config)))

      actorRef ! PostsBatch(posts)
      expectMsg(DoneMsg)

      val files = getListOfFiles(config.outputPath)
      assert(files.nonEmpty)
      cleanOutput
    }
  }

  "Save Actor Test" must {
    "validate final result" in {
      val actorRef = TestActorRef(Props(new SaveBatchCsvActor(config)))

      actorRef ! PostsBatch(posts)
      expectMsg(DoneMsg)

      val files = getListOfFiles(config.outputPath)
      val csvOutput:String = io.Source.fromFile(files.head).mkString.replace("\n", "").replace("\r", "")
      assert(csvOutput.equals("1|title|body|tags"))
      cleanOutput
    }
  }


  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  def getListOfFiles(dir: String):List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

  def cleanOutput: Unit ={
    val files = getListOfFiles(config.outputPath)
    files.foreach(_.delete())
  }

  def initArguments: Unit ={
    val rootResources = getClass.getResource("/").getPath
    val outputDir = Files.createTempDirectory("output")
    val dir = outputDir.toFile
    println(s"Output directory ${outputDir.toAbsolutePath.toString}")
    config = config.copy(
      inputPath = rootResources+"../classes/example/",
      outputPath = outputDir.toAbsolutePath.toString,
      stopWordsPath = getClass.getResource("stopwords.txt").getPath)
  }
}
