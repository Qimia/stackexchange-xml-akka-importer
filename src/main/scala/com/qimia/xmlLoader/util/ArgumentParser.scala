package com.qimia.xmlLoader.util

import java.io.{File, FileInputStream}

import com.typesafe.config.ConfigFactory
import scopt.OParser

/**
  * An instance of this class will be passed around as program arguments.
  */
case class AppConfig(
                   inputPath:String="",
                   outputPath:String="",
                   stopWordsPath:String="",
                   numberOfSaveActors:Int=4,
                   numberOfReadActors:Int=2,
                   numberOfOutputFiles:Int=32,
                   batchSize:Int = 800
                 )

object ArgumentParser{

  def parse(args:Array[String]):AppConfig={

    if(new File("app.conf").exists()){
      return parseConfigFile()
    }
    parseArguments(args)
  }

  def parseConfigFile():AppConfig={
    val parsed = ConfigFactory.parseFile(new File("app.conf"))
    var config = AppConfig()
    parsed.entrySet().forEach(a=>a.getKey match {
      case "inputPath" => config = config.copy(inputPath = parsed.getString(a.getKey))
      case "outputPath" => config = config.copy(outputPath = parsed.getString(a.getKey))
      case "numberOfSaveActors" => config = config.copy(numberOfSaveActors = parsed.getInt(a.getKey))
      case "numberOfReadActors" => config = config.copy(numberOfReadActors = parsed.getInt(a.getKey))
      case "numberOfOutputFiles" => config = config.copy(numberOfOutputFiles = parsed.getInt(a.getKey))
      case "batchSize" => config = config.copy(batchSize = parsed.getInt(a.getKey))
      case _ => throw new IllegalArgumentException(s"Unexpected argument '${a.getKey}' found in app.conf")
    })

    if (!(new File(config.inputPath)).isDirectory) {
      System.err.println("Input path is not a directory")
      System.exit(2)
    }
    else if (!(new File(config.outputPath)).isDirectory) {
      System.err.println("Output path is not a directory")
      System.exit(3)
    }
    config
  }

  def parseArguments(args:Array[String]):AppConfig= {
    val builder = OParser.builder[AppConfig]
    import builder._
    def >(x: Int, name: String) = if (x > 0) success else failure(s"Value <$name> must be greater than 0")

    val parser1 = {
      OParser.sequence(
        programName("StackExchange-XML-Importer"),
        head("StackExchange-XML-Importer", "0.1"),
        opt[String]('i', "input")
          .required()
          .action((x, c) => c.copy(inputPath = x))
          .text("The input directory containing the XML files. All XML files in this directory will be parsed."),
        opt[String]('o', "output")
          .required()
          .action((x, c) => {
            c.copy(outputPath = x)}
          )
          .text("The output directory to save CSVs in. If it does not exist, the program will attempt to create it. If exists, all the files in it will be deleted."),

        opt[Int]('s', "numSaveActors")
          .action((x, s) => s.copy(numberOfSaveActors = x))
          .validate(>(_, "numSaveActors"))
          .text("The number of save actors"),

        opt[Int]('f', "numOutFiles")
          .action((x, n) => n.copy(numberOfOutputFiles = x))
          .validate(>(_, "numOutFiles"))
          .text("The number of output files"),

        opt[Int]('b', "batchSize")
          .action((x, b) => b.copy(batchSize = x))
          .validate(>(_, "batchSize"))
          .text("The batch size"),

        opt[Int]('w', "numWritePerFile")
          .action((x, b) => b.copy(numberOfSaveActors = x))
          .validate(>(_, "numWritePerFile"))
          .text("The number of save actors per file"),

        opt[Int](name = "numRead")
          .action((x, b) => b.copy(numberOfReadActors = x))
          .validate(>(_, "numRead"))
          .text("The number of read actors"),

        help("help").text("Either these parameters should be given or the app.conf can be put into the working directory of the application. template.conf can be used as a template.")
      )
    }

    // OParser.parse returns Option[Config]
    OParser.parse(parser1, args, AppConfig()) match {
      case Some(config) => config
      case _ => throw new IllegalArgumentException("There is a problem with the parsed arguments.")
    }
  }
}
