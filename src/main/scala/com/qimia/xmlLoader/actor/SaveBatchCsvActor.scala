package com.qimia.xmlLoader.actor

import java.io.File

import akka.actor.{Actor, ActorLogging}
import com.github.tototoshi.csv.{CSVWriter, DefaultCSVFormat}
import com.qimia.xmlLoader.model.{Post, PostsBatchMsg}
import com.qimia.xmlLoader.util.{FileLoadBalance, StopWordsBloomFilter}

case class PostsBatch(posts: PostsBatchMsg)
case class DoneMsg()

class SaveBatchCsvActor extends Actor with ActorLogging {
  import SaveBatchCsvActor._

  def receive = {
    case PostsBatch(postsBatchMsg) => {
      implicit object MyFormat extends DefaultCSVFormat {
        override val delimiter = '|'
      }
      val outputWriter = CSVWriter.open(FileLoadBalance.getOutputFile(), true)

      for (post : Post <- postsBatchMsg.posts) {
        normalize(post)
        outputWriter.writeRow(List(post.id, post.title, post.body, post.tags))
      }
      outputWriter.close()

      sender ! DoneMsg
    }
    case _ => log.info(self.hashCode() + " Wrong message type")
  }
}

object SaveBatchCsvActor {
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