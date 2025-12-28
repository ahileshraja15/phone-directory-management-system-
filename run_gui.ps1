#!/usr/bin/env pwsh
$ErrorActionPreference = 'Stop'

$ScriptDir = Split-Path -Parent $PSCommandPath
$Root = Split-Path -Parent $ScriptDir
$OutDir = Join-Path $Root 'out'
$SrcDir = Join-Path $Root 'src/main/java'

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

# Collect Java sources
$sources = Get-ChildItem -Recurse -Filter *.java -Path $SrcDir | ForEach-Object { $_.FullName }

if (-not $sources -or $sources.Count -eq 0) {
  Write-Error "No Java sources found"
}

# Compile
& javac -d $OutDir @sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

# Run GUI
& java -cp $OutDir com.example.phonedir.GuiApp
