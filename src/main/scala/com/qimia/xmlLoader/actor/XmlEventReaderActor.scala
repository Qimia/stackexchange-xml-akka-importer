package com.qimia.xmlLoader.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.routing.RoundRobinPool
import com.qimia.xmlLoader.model.{Post, PostsBatchMsg}
import com.qimia.xmlLoader.util.Config
import XmlEventReaderActor._

import scala.io.Source
import scala.xml.pull._

class XmlEventReaderActor(config: Config) extends Actor with ActorLogging {

  val saveBatchCsvActor = context.actorOf(RoundRobinPool(config.numberOfSaveActors).props(Props(new SaveBatchCsvActor(config))), name = saveActorName)


  def receive = {
    case (fileName: String, fileIndex: Int) => {
      println(fileIndex)
      val xmlBuffer = Source.fromFile(fileName)
      xmlBuffer.next()
      val xml = new XMLEventReader(xmlBuffer)
      var postsBatchMsg = new PostsBatchMsg
      while (xml.hasNext) {
        val next = xml.next()
        next match {
          case EvElemStart(_, ROW_ELEMENT, _, _) => {
            if (isQuestion(next)) {
              val post = new Post(getAttributeValue(next, ATTR_POST_ID),
                getAttributeValue(next, ATTR_TITLE),
                getAttributeValue(next, ATTR_BODY),
                getAttributeValue(next, ATTR_TAGS))

              postsBatchMsg.addPost(post)
              if (postsBatchMsg.posts.size == config.batchSize) {
                saveBatchCsvActor ! PostsBatch(postsBatchMsg)
                totalSentRows += postsBatchMsg.posts.size
                totalSentBatches += 1
                log.info(s"${postsBatchMsg.posts.size} rows in ${totalSentBatches}th batch, file index: $fileIndex, total $totalSentRows rows sent")
                postsBatchMsg = new PostsBatchMsg
              }
            }
          }
          case _ =>
        }
      }


      // Last batch (less than maximum size)
      if (postsBatchMsg.posts.nonEmpty) {
        saveBatchCsvActor ! PostsBatch(postsBatchMsg)
        totalSentRows += postsBatchMsg.posts.size
        totalSentBatches += 1
        log.info(s"Finished index $fileIndex, batch " + totalSentBatches + s" total $totalSentRows rows sent.")
      }
    }
    case DoneMsg => {
      totalReceivedBatches += 1
      if (totalSentBatches == totalReceivedBatches) {
        log.info("Finished " + totalReceivedBatches)
        context.system.terminate()
      }
    }
  }
}

object XmlEventReaderActor {
  val ROW_ELEMENT = "row"
  val ATTR_POST_ID = "Id"
  val ATTR_POST_TYPE_ID = "PostTypeId"
  val ATTR_BODY = "Body"
  val ATTR_TITLE = "Title"
  val ATTR_TAGS = "Tags"

  val saveActorName = "saveCsvActor"

  var totalSentBatches = 0
  var totalReceivedBatches = 0
  var totalSentRows = 0


  def isQuestion(event: XMLEvent): Boolean = {
    event.asInstanceOf[EvElemStart].attrs.get(ATTR_POST_TYPE_ID) match {
      case Some(res) => "1".equals(res.text)
    }
  }

  def getAttributeValue(event: XMLEvent, label: String): String = {
    event.asInstanceOf[EvElemStart].attrs.get(label) match {
      case Some(res) => res.text
      case None => " "
    }
  }
}
