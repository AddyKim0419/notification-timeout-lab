#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes
javac --release 21 -d build/classes src/main/java/sample/TimeoutLab.java src/test/java/sample/TimeoutLabTest.java
java -cp build/classes sample.TimeoutLabTest
java -cp build/classes sample.TimeoutLab
