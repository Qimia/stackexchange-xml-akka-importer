package com.qimia.xmlLoader.util

import java.io.File

import com.github.tototoshi.csv.{CSVWriter, DefaultCSVFormat}

/**
  * Returns the files in a circular manner
  * 0 1 2 3 4 5 0 1 2 3 4 5 0 1 2...
  */
object FileLoadBalance {

  var config:AppConfig = _
  var circular:Iterator[(CSVWriter, CSVWriter)] = _

  def init(config: AppConfig) = {
    this.config = config
    implicit object MyFormat extends DefaultCSVFormat {
      override val delimiter = '|'
    }
    circular = Iterator.continually((0 until config.numberOfOutputFiles).map(x=>
      (
        CSVWriter.open(s"${config.outputPath}/postText$x.csv", true),
        CSVWriter.open(s"${config.outputPath}/postTag$x.csv", true)
      ))
    ).flatten
  }

  def closeAll={
    for(i <- 0 until config.numberOfOutputFiles+1){
      val (fl1, fl2) = nextOutputFileIndex
      fl1.close()
      fl2.close()
    }
  }

  def nextOutputFileIndex: (CSVWriter, CSVWriter) = circular.next
}
