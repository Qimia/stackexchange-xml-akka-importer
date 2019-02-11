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
 * Cyclic file selection for load-balancing of multiple CSV outputs.
 * Send acknowledge from the process-actor to the read-actor

Dependencies
------------
* [AKKA](http://akka.io) concurrent message driven library
* [GUAVA](https://github.com/google/guava) Google Guave for Bloom Filter
* [Scala CSV](https://github.com/tototoshi/scala-csv) CSV Reader/Writer for Scala
* [TestKit](http://doc.akka.io/api/akka/2.0/akka/testkit/TestKit.html) Akka test library
* [ScalaTest](http://www.scalatest.org) Scala test library
* [Apache Commons Lang](https://commons.apache.org/proper/commons-text/) Used for escaping HTML chars.
* [JSoup](https://jsoup.org/) Used for parsing the HTML text of post body and title.
 
Notes
-----

 * Whole data is separated into multiple files (e.g. 32) with no particular order. Each row of Xml is placed in one of these files.
 * Columns are separated by '|'
 * Each row of:
   * postTags{i}.csv contains the row unique id and the list of tags for that question.
   * postText{i}.csv contains the same row unique id, title, body, domain
 * Each row of tagIDs contain the generated id number for each unique label.
   * Tag ID points to tag IDs in the postTags*.csv and postText*.csv files

process
-------

![process](XML-AKKA-Importer.png)
