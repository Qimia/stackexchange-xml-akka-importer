package com.qimia.xmlLoader.util

import java.io.File

/**
  * Returns the files in a circular manner
  * 0 1 2 3 4 5 0 1 2 3 4 5 0 1 2...
  */
object FileLoadBalance {

  var config:AppConfig = _
  var circular:Iterator[Int] = _

  def init(config: AppConfig) = {
    this.config = config
    circular = Iterator.continually((0 until config.numberOfOutputFiles).toList).flatten
  }

  def nextOutputFileIndex: Int = circular.next
}
