#!/bin/sh

mvn install:install-file -Dfile=./libs/javassist-3.26.0-NEW.jar -DgroupId=org.javassist -DartifactId=javassist -Dversion=3.26.0-NEW -Dpackaging=jar -DgeneratePom=true

cd ./xtracer
./build.sh
cd ..

cd ./xinfer
mvn package
cd ..

source ./setup
