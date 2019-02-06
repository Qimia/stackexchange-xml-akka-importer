package com.qimia.xmlLoader

import java.io.File

import akka.actor.{ActorSystem, Props}
import com.qimia.xmlLoader.actor.{SaveBatchCsvActor, XmlEventReaderActor}
import com.qimia.xmlLoader.util.{ArgumentParser, Config, FileLoadBalance, StopWordsBloomFilter}
import akka.routing.RoundRobinPool
import com.qimia.xmlLoader.actor.XmlEventReaderActor.saveActorName

object XmlLoader {
  def main(args: Array[String]): Unit = {
    val config = ArgumentParser.parseArguments(args)
    run(config)
  }

  def run(config:Config)={
    StopWordsBloomFilter.init(config.stopWordsPath)
    FileLoadBalance.init(config)
    val system = ActorSystem("MySystem")
    val readXmlActor = system.actorOf(RoundRobinPool(config.numberOfReadActors).props(Props(new XmlEventReaderActor(config))), name = "readXmlActor")
    val xmlFileList = recursiveListFiles(new File(config.inputPath))
      .filter(x => x.isFile && x.getAbsolutePath.endsWith("Posts.xml"))
      .map(_.getAbsolutePath)
      .zipWithIndex
    xmlFileList.foreach(readXmlActor ! _)

  }


  /**
    * Gets all the files in a directory, recursively.
    * @param directory
    * @return
    */
  def recursiveListFiles(directory: File): Array[File] = {
    val these = directory.listFiles
    these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
  }

}
