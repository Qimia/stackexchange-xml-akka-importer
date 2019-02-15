package com.qimia.xmlLoader.util

import java.io.FileWriter

object Logger {
  var fw :FileWriter=_
  def init(config: AppConfig)={
    fw = new FileWriter(s"${config.outputPath}/log.txt")
  }

  def appendToLogFile(text:String):Unit={
      fw.write(s"$text\n")
    fw.flush()
  }
  def close():Unit=fw.close()
}
