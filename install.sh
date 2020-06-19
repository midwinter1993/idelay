#!/bin/sh

mvn install:install-file -Dfile=./libs/javassist-3.26.0-GA.jar -DgroupId=org.javassist -DartifactId=javassist -Dversion=3.26.0-GA -Dpackaging=jar -DgeneratePom=true

cd ./xtracer
./build.sh
cd ..

cd ./xinfer
mvn package
cd ..
