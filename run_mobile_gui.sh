#!/usr/bin/env bash
set -euo pipefail

mkdir -p out

# Compile
javac -d out $(find src/main/java -name "*.java")

# Run Mobile-like GUI
java -cp out com.example.phonedir.GuiAppMobile