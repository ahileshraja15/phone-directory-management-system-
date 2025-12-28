#!/usr/bin/env pwsh
$ErrorActionPreference = 'Stop'

# Paths
$OutDir = "out"
$SrcDir = "src/main/java"
$DistDir = "dist"

# Ensure output dirs
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
New-Item -ItemType Directory -Force -Path $DistDir | Out-Null

# Compile all sources
$sources = Get-ChildItem -Recurse -Filter *.java -Path $SrcDir | ForEach-Object { $_.FullName }
if (-not $sources -or $sources.Count -eq 0) {
  Write-Error "No Java sources found under $SrcDir"
}

& javac -d $OutDir @sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

# Resolve jar executable
$jarCmd = $null
try { $jarCmd = (Get-Command jar -ErrorAction Stop).Source } catch {}
if (-not $jarCmd) {
  try { $javacCmd = (Get-Command javac -ErrorAction Stop).Source } catch { $javacCmd = $null }
  if ($javacCmd) {
    $jdkBin = Split-Path $javacCmd -Parent
    $candidate = Join-Path $jdkBin "jar.exe"
    if (Test-Path $candidate) { $jarCmd = $candidate }
  }
}
if (-not $jarCmd) { Write-Error "Could not locate 'jar' tool. Ensure JDK (not just JRE) is installed and on PATH." }

# Create runnable JARs
# Console
& $jarCmd cfe "$DistDir/phone-directory-console.jar" com.example.phonedir.App -C $OutDir .
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

# GUI (basic)
& $jarCmd cfe "$DistDir/phone-directory-gui.jar" com.example.phonedir.GuiApp -C $OutDir .
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

# GUI (mobile-like)
& $jarCmd cfe "$DistDir/phone-directory-mobile.jar" com.example.phonedir.GuiAppMobile -C $OutDir .
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Packaged JARs into ${DistDir}:" -ForegroundColor Green
Get-ChildItem $DistDir | Format-Table -AutoSize
