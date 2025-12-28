#!/usr/bin/env pwsh
$ErrorActionPreference = 'Stop'

# Resolve repo root
$ScriptDir = Split-Path -Parent $PSCommandPath
$Root = Split-Path -Parent $ScriptDir
$SrcDir = Join-Path $Root 'src/main/java'
$OutDir = Join-Path $Root 'out'
$DistDir = Join-Path $Root 'dist'
$ToolSrcDir = Join-Path $ScriptDir 'jarbuilder'
$ToolOutDir = Join-Path $ScriptDir 'jarbuilder_out'
$JarPath = Join-Path $DistDir 'PhoneDirectoryApp.jar'
$MainClass = 'com.example.phonedir.MaterialApp'

# Compile app sources
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$sources = Get-ChildItem -Recurse -Filter *.java -Path $SrcDir | ForEach-Object { $_.FullName }
if (-not $sources -or $sources.Count -eq 0) { Write-Error "No Java sources found under $SrcDir" }
$resp = Join-Path $OutDir 'sources.txt'
Set-Content -Path $resp -Value ($sources -join "`n") -Encoding ASCII
& javac -d $OutDir "@$resp"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

# Compile JarBuilder tool
New-Item -ItemType Directory -Force -Path $ToolOutDir | Out-Null
$toolSources = Get-ChildItem -Recurse -Filter *.java -Path $ToolSrcDir | ForEach-Object { $_.FullName }
$toolResp = Join-Path $ToolOutDir 'toolsources.txt'
Set-Content -Path $toolResp -Value ($toolSources -join "`n") -Encoding ASCII
& javac -d $ToolOutDir "@$toolResp"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

# Build jar using the tool
New-Item -ItemType Directory -Force -Path $DistDir | Out-Null
& java -cp $ToolOutDir JarBuilder $JarPath $OutDir $MainClass
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Built single JAR: $JarPath" -ForegroundColor Green