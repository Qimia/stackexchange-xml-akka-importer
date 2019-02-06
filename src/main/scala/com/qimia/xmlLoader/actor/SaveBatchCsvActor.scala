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
      val outputWriter = CSVWriter.open(FileLoadBalance.nextOutputFile, true)

      for (post : Post <- postsBatchMsg.posts) {
        normalize(post)
        outputWriter.writeRow(List(post.id, post.title, post.body, post.tags))
      }
      outputWriter.close()
      writtenRows += postsBatchMsg.posts.size
      writtenBatches += 1
      log.info(s"${self.hashCode()} written ${postsBatchMsg.posts.size} in batch, $writtenRows in total rows, in the batch $writtenBatches ")

      sender ! DoneMsg
      val threadId = Thread.currentThread().getId()
    }
    case _ => log.error(self.hashCode() + " Wrong message type")
  }
}

object SaveBatchCsvActor {
  var writtenRows:Int=0
  var writtenBatches:Int=0
  def normalize(post: Post): Unit ={
    post.title = post.title.toLowerCase
      .replaceAll("[\\p{Punct}||\\p{Cntrl}&&[^.'-]]"," ")
      .replaceAll(" +",",")
      .split(",")
      .filter(x => (!StopWordsBloomFilter.contains(x) && !x.matches("[0-9\\p{Punct}]*")))
      .map(x => x.replaceAll("^[.']+|[.']+$",""))
      .mkString(",")
    post.body = post.body.toLowerCase
      .replaceAll("[\\p{Punct}||\\p{Cntrl}&&[^.'-]]"," ")
      .replaceAll(" +",",")
      .split(",")
      .filter(x => (!StopWordsBloomFilter.contains(x) && !x.matches("[0-9\\p{Punct}]*")))
      .map(x => x.replaceAll("^[.']+|[.']+$",""))
      .mkString(",")
    post.tags = post.tags.toLowerCase().replaceAll("><",",").replaceAll("[<>]","")
  }
}