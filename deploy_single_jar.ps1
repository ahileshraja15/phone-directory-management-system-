#!/usr/bin/env pwsh
$ErrorActionPreference = 'Stop'

# Resolve repo jar
$scriptRoot = $PSScriptRoot
if (-not $scriptRoot) { $scriptRoot = Split-Path -Parent $PSCommandPath }
$repoRoot = Split-Path -Parent $scriptRoot
$srcJar = Join-Path $repoRoot 'dist\PhoneDirectoryApp.jar'
if (-not (Test-Path $srcJar)) { Write-Error "JAR not found: $srcJar" }

# Copy JAR to Desktop
$desktop = [Environment]::GetFolderPath('Desktop')
$destJar = Join-Path $desktop 'PhoneDirectoryApp.jar'
Copy-Item -Force $srcJar $destJar

# Resolve javaw/java
$javaw = $null
try { $javaw = (Get-Command javaw -ErrorAction Stop).Source } catch {}
if (-not $javaw) {
  try { $javaw = (Get-Command java -ErrorAction Stop).Source } catch {}
}
if (-not $javaw) { Write-Error "java/javaw not found in PATH. Install JDK/JRE or add Java to PATH." }

# Create Start Menu shortcut to launch the JAR
$programs = [Environment]::GetFolderPath('Programs')
$lnkPath = Join-Path $programs 'Phone Directory.lnk'
$shell = New-Object -ComObject WScript.Shell
$sc = $shell.CreateShortcut($lnkPath)
$sc.TargetPath = $javaw
$sc.Arguments  = "-jar `"$destJar`""
$sc.WorkingDirectory = $desktop
$sc.IconLocation = "$env:WINDIR\System32\shell32.dll, 167"
$sc.Save()

Write-Host "Copied JAR to: $destJar" -ForegroundColor Green
Write-Host "Start Menu shortcut created: $lnkPath" -ForegroundColor Green
