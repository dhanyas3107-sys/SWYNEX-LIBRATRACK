#!/usr/bin/env bash
# Compile and run LibraTrack on Linux / macOS / Git Bash.
# Usage:  ./run.sh          -> compile and start the application
#         ./run.sh test     -> compile and run the automated tests
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 -d out $(find src test -name "*.java")
if [ "$1" = "test" ]; then
    java -cp out LibraTrackTests
else
    java -cp out Main
fi
