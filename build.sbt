name := "stackexchange-xml-csv-akka"

version := "1.0"

scalaVersion := "2.12.1"


libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.16"
libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.4"
libraryDependencies += "com.google.guava" % "guava" % "21.0"
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.4.16" % "test"
// https://mvnrepository.com/artifact/com.github.scopt/scopt
libraryDependencies += "com.github.scopt" %% "scopt" % "4.0.0-RC2"
// https://mvnrepository.com/artifact/org.apache.commons/commons-text
libraryDependencies += "org.apache.commons" % "commons-text" % "1.6"

