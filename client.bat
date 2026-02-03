@echo off
if "%~1"=="" goto error

if not exist bin mkdir bin

javac -d bin -sourcepath src\main\java src\main\java\org\subbdds\compile\*.java

java -cp bin org.subbdds.compile.UDPClient %1 %2 %3
goto end

:error
echo Usage: client.bat [source_file] [server_ip] [optional: dest_filename]
:end