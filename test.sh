#!/usr/bin/bash

set -e

cd test/

rm -f *.class *.jar *.zip
rm -rf build/
cp ../build.jar .
java -jar build.jar Test.java
java -jar Test.jar


# rm -f *.class *.jar *.zip
java -jar build.jar Test2.java
java -jar Test2.jar
