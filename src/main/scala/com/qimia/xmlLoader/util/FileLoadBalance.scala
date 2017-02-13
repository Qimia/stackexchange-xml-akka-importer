package com.qimia.xmlLoader.util

import java.io.File

object FileLoadBalance {

  val numberOfOutputFiles = Arguments.numberOfOutputFiles

  def getOutputFile(): File = {
    val rnd = new scala.util.Random
    return new File(Arguments.outputPath + "result" + rnd.nextInt(numberOfOutputFiles) + ".csv")
  }
}
