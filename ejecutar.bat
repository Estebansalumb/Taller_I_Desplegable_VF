@echo off
if not exist bin mkdir bin
javac -encoding UTF-8 -d bin -cp "lib\*" src\modelo\*.java src\datos\*.java src\presentacion\*.java
if errorlevel 1 exit /b 1
chcp 65001 >nul
java -cp "bin;lib\*" presentacion.Main
