#!/usr/bin/env bash
echo "This may take a while; sbt is slow. We download and package everything into a fat .jar file which contains all the necessary libraries"
sbt assembly;
cp target/scala-2.12/stackexchange-xml-csv-akka-assembly-1.0.jar  ./