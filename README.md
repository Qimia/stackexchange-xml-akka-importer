stackexchange-xml-akka-importer
===============================
Scala project to read **stackexchange** xml data file, process it's data and save it as CSV file.
Initially this project started as part of a data science project for tags prediction in stackexchange data, and after multiple versions of Streaming `STAX` reader, Event Reader with multi-threading and Java `Camel` ETL reader we end up with this solution that gives the most optimal solution as a performance matter (handled 51GB in about 70 minutes).  

Steps
-----
 * Read XML File using event reader model within `AKKA` actor
 * Un-marshal data into Post model
 * Filter posts answers
 * Aggregate multiple posts as a batch and send it process
 * `RoundRobinPool` of AKKA actors to handle batches
 * Normalize each post information (title, body and tags)
   * Remove stop words using `BloomFilter` index
   * Remove HTML tags
   * Remove numbers
 * Select random file within range to save the output as CSV for load-balancing
 * Send acknowledge from the process-actor to the read-actor

Dependencies
------------
* [AKKA](http://akka.io) concurrent message driven library
* [GUAVA](https://github.com/google/guava) Google Guave for Bloom Filter
* [Scala CSV](https://github.com/tototoshi/scala-csv) CSV Reader/Writer for Scala
* [TestKit](http://doc.akka.io/api/akka/2.0/akka/testkit/TestKit.html) Akka test library
* [ScalaTest](http://www.scalatest.org) Scala test library
 
Notes
-----

The output CSVs have the format = id|title|body|tags
id, body and tags are each a comma-separated list.

process
-------

![process](XML-AKKA-Importer.png)
