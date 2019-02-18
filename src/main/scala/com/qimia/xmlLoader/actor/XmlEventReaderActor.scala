package com.qimia.xmlLoader.actor

import java.io.File

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.routing.RoundRobinPool
import com.qimia.xmlLoader.model.{Post, PostsBatchMsg}
import com.qimia.xmlLoader.util.AppConfig
import XmlEventReaderActor._
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.Jsoup

import scala.io.Source
import scala.xml.pull._

class XmlEventReaderActor(config: AppConfig, saveBatchCsvActor: ActorRef) extends Actor with ActorLogging {

  def receive = {
    case (fileName: String, fileIndex: Int) => {
      val dirName = new File(fileName).getParentFile.getName
      val xmlBuffer = Source.fromFile(fileName)
      xmlBuffer.next()
      val xml = new XMLEventReader(xmlBuffer)
      var postsBatchMsg = new PostsBatchMsg
      while (xml.hasNext) {
        val next = xml.next()
        next match {
          case EvElemStart(_, ROW_ELEMENT, _, _) => {
            if (isQuestion(next)) {
              next.asInstanceOf[EvElemStart]
              val post = new Post(getAttributeValue(next, ATTR_POST_ID),

                removeCodeBlocks(getAttributeValue(next, ATTR_TITLE)),

                Jsoup.parse(removeCodeBlocks(getAttributeValue(next, ATTR_BODY))).text(),

                getAttributeValue(next, ATTR_TAGS),

                dirName)

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

  /**
    * Warning!!! Unescapes the HTML tags if any
    * @param str
    * @return
    */
  def removeCodeBlocks(str:String)=
    StringEscapeUtils.unescapeHtml4(str)
      .replaceAll("\n", " ")
      .replaceAll("\\<code\\>.*?\\<\\/code\\>", " ")
}
