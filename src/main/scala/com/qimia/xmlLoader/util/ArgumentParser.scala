package com.qimia.xmlLoader.util

import java.io.File
import java.nio.file.Files

import scopt.OParser

/**
  * An instance of this class will be passed around as program arguments.
  */
case class Config(
                   inputPath:String="",
                   outputPath:String="",
                   stopWordsPath:String="",
                   numberOfSaveActors:Int=3,
                   numberOfReadActors:Int=4,
                   numberOfOutputFiles:Int=1,
                   batchSize:Int = 800,
                   columnSeparator:Char=',',
                   rowSeparator:Char='\n'
                 )

object ArgumentParser{

  def parseArguments(args:Array[String]):Config= {
    val builder = OParser.builder[Config]
    import builder._
    def >(x: Int, name: String) = if (x > 0) success else failure(s"Value <$name> must be greater than 0")

    val parser1 = {
      OParser.sequence(
        programName("StackExchange-XML-Importer"),
        head("StackExchange-XML-Importer", "0.1"),
        opt[String]('i', "input")
          .required()
          .validate(x=> if (new File(x).exists()) success else failure("The input directory does not exist"))
          .action((x, c) => c.copy(inputPath = x))
          .text("The input directory containing the XML files. All XML files in this directory will be parsed."),
        opt[String]('o', "out")
          .required()
          .action((x, c) => {
            val fl = new File(x)
            if (!fl.exists) {
              fl.mkdirs()
              println("The output directory was created.")
            }
            fl.listFiles().foreach(_.delete())
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

        opt[Char]('c', "colsep")
          .action((x, c) => c.copy(columnSeparator = x))
          .validate(>(_, "colsep"))
          .text("The column separator for CSVs."),

        opt[Char]('r', "rowsep")
          .action((x, r) => r.copy(rowSeparator = x))
          .text("The row separator for CSVs"),

        help("help").text("prints this usage text")
      )
    }

    // OParser.parse returns Option[Config]
    OParser.parse(parser1, args, Config()) match {
      case Some(config) => config
      case _ => throw new IllegalArgumentException("There is a problem with the parsed arguments.")
    }
  }
}
