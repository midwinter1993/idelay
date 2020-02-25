#!/bin/sh

AGENT="$PWD/target/idelay-1.0-SNAPSHOT.jar"

echo java -Dlog4j.configurationFile=log4j2.xml -javaagent:$AGENT test.Test
java -Dlog4j.configurationFile=log4j2.xml -javaagent:$AGENT test.Test
