package com.qimia.xmlLoader.util

import com.google.common.base.Charsets
import com.google.common.hash.{BloomFilter, Funnels}

import scala.io.Source

/**
  * There are merely 922 stopwords. I'm not sure if a Bloom Filter is useful.
  */
object StopWordsBloomFilter {

  var bf: BloomFilter[String] = BloomFilter.create[String](Funnels.stringFunnel(Charsets.UTF_8), 0, 0.001)

  def init(stopWordsPath: String): Unit = {
    stopWordsPath match{
      case "" =>
      case _ =>
        val stopWords = Source.fromFile(stopWordsPath).getLines.toList
        bf = BloomFilter.create[String](Funnels.stringFunnel(Charsets.UTF_8), stopWords.size, 0.001)
        stopWords.foreach(bf.put(_))
    }
  }

  def contains(word: String): Boolean = bf.mightContain(word)

}
