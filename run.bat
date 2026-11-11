@echo off
REM Compile and run LibraTrack on Windows (Command Prompt).
REM Usage:  run.bat          -> compile and start the application
REM         run.bat test     -> compile and run the automated tests
cd /d "%~dp0"
chcp 65001 >nul
if not exist out mkdir out
dir /s /b src\*.java test\*.java > sources.tmp
javac -encoding UTF-8 -d out @sources.tmp
del sources.tmp
if /i "%1"=="test" (
    java -cp out LibraTrackTests
) else (
    java -cp out Main
)
