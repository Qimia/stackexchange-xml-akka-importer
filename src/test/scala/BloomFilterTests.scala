import java.io.File

import com.google.common.base.Charsets
import com.google.common.hash.{BloomFilter, Funnels}
import com.qimia.xmlLoader.util.Config
import org.scalatest.{BeforeAndAfter, FunSuite}

class BloomFilterTests extends FunSuite with BeforeAndAfter {
  var bf: BloomFilter[String] = _
  var stopWords: List[String] = _
  var otherWords: List[String] = _

  var config = Config()

  before {
    initArguments
    setupTestData
    bf = BloomFilter.create[String](Funnels.stringFunnel(Charsets.UTF_8), stopWords.size, 0.001)
    stopWords.foreach(bf.put(_))
  }

  test("MightContain") {
    var matchCount:Int = 0;
    for (word:String <- stopWords) {
      if (bf.mightContain(word)) {
        matchCount+=1;
      }
    }
    assert(stopWords.size - matchCount < 3)
  }

  test("ContainFalseNegative") {
    var matchCount:Int = 0;
    for (word:String <- otherWords) {
      if (bf.mightContain(word)) {
        matchCount+=1;
      }
    }
    assert(matchCount == 0)
  }

  def setupTestData: Unit ={
    val someWords = "wouldn't,you,you'd,you'll,you're,you've,your,yours,yourself,yourselves,return,arent,cant,couldnt,didnt,doesnt,dont,hadnt,hasnt,havent,hes,heres,hows,im,isnt,its,lets,mustnt,shant,shes,shouldnt,thats,theres,theyll,theyre,theyve,wasnt,were,werent,whats,whens,wheres,whos,whys,wont,wouldnt,youd,youll,youre,youv"
    stopWords = someWords.split(',').toList

    val anotherWords = "a,able,about,above,abst,accordance,according,accordingly,across,act";
    otherWords = anotherWords.split(',').toList
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
