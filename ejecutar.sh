#!/bin/sh
mkdir -p bin
javac -encoding UTF-8 -d bin -cp "lib/*" src/modelo/*.java src/datos/*.java src/presentacion/*.java || exit 1
java -cp "bin:lib/*" presentacion.Main
