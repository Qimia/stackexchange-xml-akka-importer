package com.qimia.xmlLoader

import java.io.{File, FileNotFoundException, FileWriter}
import java.util.Calendar

import akka.actor.{ActorSystem, Props}
import com.qimia.xmlLoader.actor.{SaveBatchCsvActor, XmlEventReaderActor}
import com.qimia.xmlLoader.util._
import akka.routing.RoundRobinPool
import com.qimia.xmlLoader.actor.XmlEventReaderActor.saveActorName

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object XmlLoader {
  def main(args: Array[String]): Unit = {
    val config = ArgumentParser.parse(args)
    run(config)
  }

  def run(config:AppConfig):Unit={

    val start = System.currentTimeMillis()
    validateArgs(config)
    Logger.init(config)
    try {

      Logger.appendToLogFile(s"Started at ${Calendar.getInstance().getTime}")
      Logger.appendToLogFile(s"Input path: ${config.inputPath}")
      Logger.appendToLogFile(s"Output path: ${config.outputPath}")
      Logger.appendToLogFile(s"Number of output files: ${config.numberOfOutputFiles}")
      Logger.appendToLogFile(s"Number of read actors: ${config.numberOfReadActors}")
      Logger.appendToLogFile(s"Number of write actors: ${config.numberOfSaveActors}")
      Logger.appendToLogFile(s"Batch size: ${config.batchSize}")

      StopWordsBloomFilter.init(config.stopWordsPath)
      FileLoadBalance.init(config)

      val system = ActorSystem("MySystem")
      val saveBatchCsvActor = system.actorOf(RoundRobinPool(config.numberOfSaveActors).props(Props(new SaveBatchCsvActor(config))), name = saveActorName)
      val readXmlActor = system.actorOf(RoundRobinPool(config.numberOfReadActors).props(Props(new XmlEventReaderActor(config, saveBatchCsvActor))), name = "readXmlActor")

      val xmlFileList = recursiveListFiles(new File(config.inputPath))
        .filter(x => x.isFile && x.getAbsolutePath.endsWith("Posts.xml"))
        .sorted(Ordering.fromLessThan((file: File, file1: File) => file.length() < file1.length()))
        .map(_.getAbsolutePath)
        .zipWithIndex
      Logger.appendToLogFile("")
      Logger.appendToLogFile("XML files to parse")

      xmlFileList.foreach(x => s"${Logger.appendToLogFile(x._1)}")
      xmlFileList.foreach(readXmlActor ! _)

      Await.ready(system.whenTerminated, Duration.Inf)

      Logger.appendToLogFile("")
      Logger.appendToLogFile(s"Successfuly finished at ${Calendar.getInstance().getTime}")

    } catch {
      case e =>
        Logger.appendToLogFile(s"FAILURE TO COMPLETE. ")
        Logger.appendToLogFile("Reason:")
        Logger.appendToLogFile(e.toString)

    } finally {
      Logger.appendToLogFile(s"Total ${(System.currentTimeMillis()-start)/1000} seconds.")
      Logger.close()
    }

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

  def validateArgs(config:AppConfig)={
    prepareOutDir(config)
    validateInputDirExists(config)
  }

  def validateInputDirExists(config: AppConfig)={
    val inDir = new File(config.inputPath)
    if (!inDir.exists()){
      throw new FileNotFoundException(s"The input directory '${config.inputPath}' does not exist.")
    }
  }

  def prepareOutDir(config: AppConfig)={
    val outDir = new File(config.outputPath+"/")
    if (!outDir.exists()) {
      outDir.mkdirs()
      println("The output directory was created.")
    }
    outDir.listFiles().foreach(_.delete())
  }
}
