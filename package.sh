#!/usr/bin/env bash
set -euo pipefail

OUT=out
SRC=src/main/java
DIST=dist

mkdir -p "$OUT" "$DIST"

# Compile
javac -d "$OUT" $(find "$SRC" -name "*.java")

# Create runnable JARs
jar cfe "$DIST/phone-directory-console.jar" com.example.phonedir.App -C "$OUT" .
jar cfe "$DIST/phone-directory-gui.jar" com.example.phonedir.GuiApp -C "$OUT" .
jar cfe "$DIST/phone-directory-mobile.jar" com.example.phonedir.GuiAppMobile -C "$OUT" .

ls -l "$DIST"