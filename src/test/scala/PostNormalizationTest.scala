import java.io.File

import com.qimia.xmlLoader.actor.SaveBatchCsvActor
import com.qimia.xmlLoader.model.Post
import com.qimia.xmlLoader.util.{ Config, StopWordsBloomFilter}
import org.scalatest.{BeforeAndAfter, FunSuite}

class PostNormalizationTest extends FunSuite with BeforeAndAfter {
  var post:Post =_

  var config = Config()

  before {
    initArguments
    StopWordsBloomFilter.init(config.stopWordsPath)
    fillTestData
  }

  test("MightContain") {
    val expectedBody:String = "4|setting,form's,opacity,decimal,double|track-bar,change,form's,opacity,code,pre,code,decimal,trans,trackbar1.value,this.opacity," +
      "trans,code,pre,build,error,blockquote,implicitly,convert,type,decimal,double,blockquote,making,code,trans,code,code,double,code,control,work,code,worked,fine," +
      "vb.net,past|c#,winforms,type-conversion,decimal,opacity";

    SaveBatchCsvActor.normalize(post)

    assert(String.join("|", post.id, post.title, post.body, post.tags).equals(expectedBody))
  }

  def fillTestData: Unit ={
    post = new Post("4",
      "When setting a form's opacity should I use a decimal or double?",
      "I want to use a track-bar to change a form's opacity.</p><p>This is my code:</p><pre><code>decimal trans = trackBar1.Value / 5000;" +
      "this.Opacity = trans;</code></pre><p>When I try to build it, I get this error:</p><blockquote><p>Cannot implicitly convert type 'decimal' to 'double'.</p></blockquote> " +
      "<p>I tried making <code>trans</code> a <code>double</code>, but then the control doesn't work. This code has worked fine for me in VB.NET in the past.",
      "<c#><winforms><type-conversion><decimal><opacity>")
  }

  def initArguments: Unit ={
    val rootResources = getClass.getResource("/").getPath
    val dir = new File(rootResources + "output");
    if (!dir.exists())
      dir.mkdir();
    config = config.copy(
      outputPath = getClass.getResource("output/").getPath,
      stopWordsPath = getClass.getResource("stopwords.txt").getPath)
  }
}
