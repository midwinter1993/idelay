#!/bin/sh

AGENT="$PWD/target/idelay-1.0-SNAPSHOT.jar"

echo java -javaagent:$AGENT test.Test
java -javaagent:$AGENT test.Test
