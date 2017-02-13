import java.io.File

import akka.actor.{ActorSystem, Props}
import akka.routing.{Broadcast, RoundRobinPool}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.qimia.xmlLoader.actor.{DoneMsg, PostsBatch, SaveBatchCsvActor}
import com.qimia.xmlLoader.model.{Post, PostsBatchMsg}
import com.qimia.xmlLoader.util.{Arguments, StopWordsBloomFilter}
import org.scalatest._

class ActorSystemTests extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  var posts: PostsBatchMsg = _

  override def beforeAll(){
    initArguments
    StopWordsBloomFilter.init(Arguments.stopWordsPath)
    posts = new PostsBatchMsg
    posts.addPost(new Post("1", "title", "body", "tags"))
  }

  "Save Actor Test" must {
    "Send Batch to Actor" in {
      val actorRef = TestActorRef[SaveBatchCsvActor]

      actorRef ! PostsBatch(posts)
      expectMsg(DoneMsg)
      cleanOutput
    }
  }

  "Actor Pool Test" must {
    "Send batches to pool" in {
      val router = system.actorOf(RoundRobinPool(3).props(Props(new SaveBatchCsvActor)), name = "savePool")

      router ! Broadcast(PostsBatch(posts))
      assert(3 == receiveN(3).length);
      cleanOutput
    }
  }

  "Save Actor Test" must {
    "Output file created" in {
      val actorRef = TestActorRef[SaveBatchCsvActor]

      actorRef ! PostsBatch(posts)
      expectMsg(DoneMsg)

      val files = getListOfFiles(Arguments.outputPath)
      assert(files.size > 0)
      cleanOutput
    }
  }

  "Save Actor Test" must {
    "validate final result" in {
      val actorRef = TestActorRef[SaveBatchCsvActor]

      actorRef ! PostsBatch(posts)
      expectMsg(DoneMsg)

      val files = getListOfFiles(Arguments.outputPath)
      val csvOutput:String = io.Source.fromFile(files(0)).mkString.replace("\n", "").replace("\r", "")
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
    val files = getListOfFiles(Arguments.outputPath)
    files.foreach(_.delete())
  }

  def initArguments: Unit ={
    val rootResources = getClass.getResource("/").getPath
    val dir = new File(rootResources + "output");
    if (!dir.exists())
      dir.mkdir();
    Arguments.outputPath = getClass.getResource("output/").getPath
    Arguments.stopWordsPath = getClass.getResource("stopwords.txt").getPath
  }
}
