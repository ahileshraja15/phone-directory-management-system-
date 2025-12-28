#!/usr/bin/env pwsh
$ErrorActionPreference = 'Stop'

# Resolve repo root
$ScriptDir = Split-Path -Parent $PSCommandPath
$Root = Split-Path -Parent $ScriptDir
$SrcDir = Join-Path $Root 'src/main/java'
$OutDir = Join-Path $Root 'out'
$DistDir = Join-Path $Root 'dist'
$StageDir = Join-Path $Root 'build_jar_staging'
$JarPath = Join-Path $DistDir 'PhoneDirectoryApp.jar'
$ZipPath = Join-Path $DistDir 'PhoneDirectoryApp.zip'

# Compile sources
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$sources = Get-ChildItem -Recurse -Filter *.java -Path $SrcDir | ForEach-Object { $_.FullName }
if (-not $sources -or $sources.Count -eq 0) { Write-Error "No Java sources found under $SrcDir" }
& javac -d $OutDir @sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

# Prepare staging folder with classes at root and manifest
if (Test-Path $StageDir) { Remove-Item -Recurse -Force $StageDir }
New-Item -ItemType Directory -Force -Path (Join-Path $StageDir 'META-INF') | Out-Null
# Copy compiled classes
Copy-Item -Recurse -Force (Join-Path $OutDir '*') $StageDir

# Create manifest
$mf = @(
  'Manifest-Version: 1.0',
  'Main-Class: com.example.phonedir.GuiAppMobile',
  ''
) -join "`r`n"
Set-Content -Path (Join-Path $StageDir 'META-INF\MANIFEST.MF') -Value $mf -Encoding ASCII

# Ensure dist exists
New-Item -ItemType Directory -Force -Path $DistDir | Out-Null

# Create JAR via zip then rename
if (Test-Path $ZipPath) { Remove-Item -Force $ZipPath }
if (Test-Path $JarPath) { Remove-Item -Force $JarPath }
Compress-Archive -Path (Join-Path $StageDir '*') -DestinationPath $ZipPath -Force
Move-Item -Force $ZipPath $JarPath

Write-Host "Built single JAR: $JarPath" -ForegroundColor Green
