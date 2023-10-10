#!/usr/bin/bash


rm -f test/*.class test/*.jar test/MANIFEST.MF &&
    rm -rf test/build &&
    # export PWD=$(realpath test) &&
    cp build.jar test/. &&
    cd test &&
    java -jar build.jar Test.java &&
    java -jar Test.jar

