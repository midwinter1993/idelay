#!/bin/sh

make -C jvmti

mvn install:install-file -Dfile=./javassist-3.26.0-GA.jar -DgroupId=org.javassist -DartifactId=javassist -Dversion=3.26.0-GA -Dpackaging=jar -DgeneratePom=true

mvn package
