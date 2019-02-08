package com.qimia.xmlLoader.model

class Post (var id: String, var title: String, var body: String, var tags: String, val forumDomain:String) {
  override def toString: String = return "Post #" + id
}
