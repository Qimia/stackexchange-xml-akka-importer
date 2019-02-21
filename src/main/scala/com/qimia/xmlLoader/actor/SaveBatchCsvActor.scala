package com.qimia.xmlLoader.actor

import akka.actor.{Actor, ActorLogging}
import com.github.tototoshi.csv.{CSVWriter, DefaultCSVFormat}
import com.qimia.xmlLoader.model.{Post, PostsBatchMsg}
import com.qimia.xmlLoader.util.{AppConfig, FileLoadBalance, StopWordsBloomFilter}

case class PostsBatch(posts: PostsBatchMsg)
case class DoneMsg()

class SaveBatchCsvActor(config:AppConfig) extends Actor with ActorLogging {
  import SaveBatchCsvActor._

  def receive = {
    case PostsBatch(postsBatchMsg) => {
      implicit object MyFormat extends DefaultCSVFormat {
        override val delimiter = '|'
      }

      val fileIndex = FileLoadBalance.nextOutputFileIndex
      val bodyTitleWriter = CSVWriter.open(s"${config.outputPath}/postText$fileIndex.csv", true)
      val tagsWriter = CSVWriter.open(s"${config.outputPath}/postTags$fileIndex.csv", true)

      for (post : Post <- postsBatchMsg.posts) {
        normalize(post)
        val tagsOfPost = post.tags.split(",")


        /**
          * Write the title and body information to CSV
          * rowID is the primary key for both
          * first file contains the row ID, title and body
          * second file contains the same row ID and list of IDs of the tags
          */

        val newRowID = getRowID
        bodyTitleWriter.writeRow(List(newRowID, post.title, post.body, post.forumDomain))
        tagsWriter.writeRow(List(newRowID, tagsOfPost.mkString(","), post.forumDomain))
      }
      bodyTitleWriter.close()
      tagsWriter.close()
      writtenRows += postsBatchMsg.posts.size
      writtenBatches += 1
      log.info(s"${self.hashCode()} written ${postsBatchMsg.posts.size} in batch, $writtenRows in total rows, in the batch $writtenBatches ")

      sender ! DoneMsg
    }
    case _ => log.error(self.hashCode() + " Wrong message type")
  }
}

object SaveBatchCsvActor {

  @volatile var writtenRows:Int=0
  @volatile var writtenBatches:Int=0
  @volatile private var rowID:Long = -1

  def getRowID=synchronized{
    rowID+=1
    rowID
  }

  /**
    * A lot of the normalizations are removed. They are done in the Spark code of Overflow-processor.
    * @param post
    */
  def normalize(post: Post): Unit ={
    post.title = post.title
    post.body = post.body
    post.tags = post.tags.toLowerCase().replaceAll("><",",").replaceAll("[<>]","")
  }

}