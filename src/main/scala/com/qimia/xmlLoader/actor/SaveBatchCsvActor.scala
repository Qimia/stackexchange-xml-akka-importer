package com.qimia.xmlLoader.actor

import akka.actor.{Actor, ActorLogging}
import com.github.tototoshi.csv.{CSVWriter, DefaultCSVFormat}
import com.qimia.xmlLoader.model.{Post, PostsBatchMsg}
import com.qimia.xmlLoader.util.{Config, FileLoadBalance, StopWordsBloomFilter}

case class PostsBatch(posts: PostsBatchMsg)
case class DoneMsg()

class SaveBatchCsvActor(config:Config) extends Actor with ActorLogging {
  import SaveBatchCsvActor._

  def receive = {
    case PostsBatch(postsBatchMsg) => {
      implicit object MyFormat extends DefaultCSVFormat {
        override val delimiter = '|'
      }

      val fileIndex = FileLoadBalance.nextOutputFileIndex
      val bodyTitleWriter = CSVWriter.open(s"${config.outputPath}/postText$fileIndex.csv", true)
      val tagRelationWriter = CSVWriter.open(s"${config.outputPath}/postTags$fileIndex.csv", true)

      for (post : Post <- postsBatchMsg.posts) {
        normalize(post)
        val tagsOfPost = post.tags.split(",")

        /**
          * Create the necessary tags
          * If the tag is not seen before, it is created with the next ID, incremental from 1
          */        tagsOfPost.foreach(
          tag => {
            val tagID:Int = tagIdPairs getOrElse (tag, tagIdPairs.size)
            tagIdPairs(tag) = tagID
          }
        )

        /**
          * Write the title and body information to CSV
          * rowID is the primary key for both
          * first file contains the row ID, title and body
          * second file contains the same row ID and list of IDs of the tags
          */

        val newRowID = getRowID
        bodyTitleWriter.writeRow(List(newRowID, post.title, post.body, post.forumDomain))
        tagRelationWriter.writeRow(List(newRowID, tagsOfPost.map(tagIdPairs(_)).mkString(","), post.forumDomain))
      }
      bodyTitleWriter.close()
      tagRelationWriter.close()
      writtenRows += postsBatchMsg.posts.size
      writtenBatches += 1
      log.info(s"${self.hashCode()} written ${postsBatchMsg.posts.size} in batch, $writtenRows in total rows, in the batch $writtenBatches ")

      sender ! DoneMsg
    }
    case _ => log.error(self.hashCode() + " Wrong message type")
  }
}

object SaveBatchCsvActor {

  /**
    * We must use thread safe map and variables as these are global objects modified by multiple threads.
    */
  val tagIdPairs: scala.collection.concurrent.Map[String,Int] = scala.collection.concurrent.TrieMap[String,Int]()
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


  def writeTags(config:Config):Unit = {
    implicit object MyFormat extends DefaultCSVFormat {
      override val delimiter = '|'
    }
    val writer = CSVWriter.open(s"${config.outputPath}/tagIDs.csv", true)
    tagIdPairs.foreach(x=>writer.writeRow(List(x._2, x._1)))
    writer.flush()
    writer.close()
  }
}