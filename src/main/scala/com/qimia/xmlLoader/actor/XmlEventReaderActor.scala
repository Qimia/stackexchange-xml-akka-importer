package com.qimia.xmlLoader.actor

import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.RoundRobinPool
import com.qimia.xmlLoader.model.{Post, PostsBatchMsg}
import com.qimia.xmlLoader.util.Arguments

import scala.io.Source
import scala.xml.pull._

class XmlEventReaderActor extends Actor with ActorLogging {
  import XmlEventReaderActor._

  def receive = {
    case fileName: String => {
      val saveBatchCsvActor = context.actorOf(RoundRobinPool(Arguments.numberOfSaveActors).props(Props(new SaveBatchCsvActor)), name = saveActorName)
      val xml = new XMLEventReader(Source.fromFile(fileName))
      var postsBatchMsg= new PostsBatchMsg
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
              if (postsBatchMsg.posts.size == Arguments.batchSize) {
                saveBatchCsvActor ! PostsBatch(postsBatchMsg)
                totalSentBatches += 1
                log.info("Batch " + totalSentBatches)
                postsBatchMsg = new PostsBatchMsg
              }
            }
          }
          case _ => // ignore
        }
      }
      // Last batch (less than maximum size)
      if (!postsBatchMsg.posts.isEmpty) {
        saveBatchCsvActor ! PostsBatch(postsBatchMsg)
        totalSentBatches += 1
        log.info("Batch " + totalSentBatches)
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

  def isQuestion(event: XMLEvent): Boolean = {
    event.asInstanceOf[EvElemStart].attrs.get(ATTR_POST_TYPE_ID) match {
      case Some(res) => return "1".equals(res.text)
    }
  }

  def getAttributeValue(event: XMLEvent, label: String): String = {
    event.asInstanceOf[EvElemStart].attrs.get(label) match {
      case Some(res) => res.text
      case None => " "
    }
  }
}
