@echo off
if not exist bin mkdir bin

javac -d bin -sourcepath src\main\java src\main\java\org\subbdds\compile\*.java

java -cp bin org.subbdds.compile.UDPServer %1
pause