@echo off
if "%~1"=="" goto error

:: Create bin if not exists
if not exist bin mkdir bin

:: Compile (Quietly)
javac -d bin -sourcepath src\main\java src\main\java\org\subbdds\compile\*.java

:: Run: %1=File, %2=IP, %3=OptionalDestName
java -cp bin org.subbdds.compile.UDPClient %1 %2 %3
goto end

:error
echo Usage: client.bat [source_file] [server_ip] [optional: dest_filename]
:end