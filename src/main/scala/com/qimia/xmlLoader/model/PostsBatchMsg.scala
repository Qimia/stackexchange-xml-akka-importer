package com.qimia.xmlLoader.model

import scala.collection.mutable.ArrayBuffer

class PostsBatchMsg {
  var posts = ArrayBuffer[Post]()

  def addPost(post: Post): Unit = {
    posts.append(post);
  }

  def clearPosts(): Unit = {
    posts.clear()
  }
}
