#!/usr/bin/bash


rm -f test/*.class test/*.jar test/MANIFEST.MF &&
    java -jar out.jar test/Test.java &&
    java -jar test/out.jar

