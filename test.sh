#!/usr/bin/bash


rm -f test/*.class test/*.jar test/MANIFEST.MF &&
    java build test/Test.java &&
    java -jar test/out.jar

