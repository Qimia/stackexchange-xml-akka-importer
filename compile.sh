#!/usr/bin/env bash
echo "This may take a while; sbt is slow. But you only need to call this file once."
sbt assembly;
cp scala target/scala-2.12/stackexchange-xml-csv-akka-assembly-1.0.jar  ./