#!/bin/sh

AGENT="$PWD/target/idelay-1.0-SNAPSHOT.jar"
JVMTI_AGENT="/$PWD/jvmti/x.so"

java -Dlog4j.configurationFile=log4j2.xml \ 
     -javaagent:$AGENT \
     -agentpath:$JVMTI_AGENT \
     -cp .:./original.jar Main
