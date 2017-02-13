package com.qimia.xmlLoader

import java.io.File

import akka.actor.{ActorSystem, Props}
import com.qimia.xmlLoader.actor.XmlEventReaderActor
import com.qimia.xmlLoader.util.{Arguments, StopWordsBloomFilter}


object XmlLoader {
  def main(args: Array[String]): Unit = {
    initArguments
    StopWordsBloomFilter.init(Arguments.stopWordsPath)
    val system = ActorSystem("MySystem")
    val readXmlActor = system.actorOf(Props[XmlEventReaderActor], name = "readXmlActor")
    readXmlActor ! Arguments.inputFile
  }

  def initArguments: Unit ={
    val rootResources = getClass.getResource("/").getPath
    val dir = new File(rootResources + "output/");
    if (!dir.exists())
      dir.mkdir();
    Arguments.outputPath = rootResources + "output/"
    Arguments.inputFile = rootResources + "input.xml"
    Arguments.stopWordsPath = rootResources +  "stopwords.txt"
  }
}
