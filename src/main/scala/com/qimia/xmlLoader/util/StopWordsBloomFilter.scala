package com.qimia.xmlLoader.util

import com.google.common.base.Charsets
import com.google.common.hash.{BloomFilter, Funnels}

import scala.io.Source

object StopWordsBloomFilter {

  var bf: BloomFilter[String] = _

  def init(stopWordsPath: String): Unit = {
    val stopWords = Source.fromFile(stopWordsPath).getLines.toList
    bf = BloomFilter.create[String](Funnels.stringFunnel(Charsets.UTF_8), stopWords.size, 0.001)
    stopWords.foreach(bf.put(_))
  }

  def contains(word: String): Boolean = {
    return bf.mightContain(word)
  }
}
