#!/usr/bin/bash


rm -f test/*.class test/*.jar test/MANIFEST.MF &&
    rm -rf test/build &&
    java -jar build.jar test/Test.java &&
    java -jar test/Test.jar

