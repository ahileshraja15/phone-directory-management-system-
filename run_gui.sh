#!/usr/bin/env bash
set -euo pipefail

mkdir -p out

# Compile
javac -d out $(find src/main/java -name "*.java")

# Run GUI
java -cp out com.example.phonedir.GuiApp