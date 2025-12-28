#!/usr/bin/env pwsh
$ErrorActionPreference = 'Stop'

# Resolve Desktop and JAR
$desktop = [Environment]::GetFolderPath('Desktop')
$repoRoot = (Split-Path -Parent (Split-Path -Parent $PSCommandPath))
$repoJar  = Join-Path $repoRoot 'dist\PhoneDirectoryApp.jar'
$deskJar  = Join-Path $desktop 'PhoneDirectoryApp.jar'

# Ensure JAR is on Desktop (copy if missing)
if (-not (Test-Path $deskJar)) {
  if (-not (Test-Path $repoJar)) { Write-Error "JAR not found in repo: $repoJar" }
  Copy-Item -Force $repoJar $deskJar
}

# Resolve javaw/java
$javaw = $null
try { $javaw = (Get-Command javaw -ErrorAction Stop).Source } catch {}
if (-not $javaw) { try { $javaw = (Get-Command java -ErrorAction Stop).Source } catch {} }
if (-not $javaw) { Write-Error "java/javaw not found in PATH. Install Java (JRE/JDK) or add to PATH." }

# Create Desktop shortcut
$lnkPath = Join-Path $desktop 'Phone Directory.lnk'
$shell = New-Object -ComObject WScript.Shell
$sc = $shell.CreateShortcut($lnkPath)
$sc.TargetPath = $javaw
$sc.Arguments  = "-jar `"$deskJar`""
$sc.WorkingDirectory = $desktop
$sc.IconLocation = "$env:WINDIR\System32\shell32.dll, 167"
$sc.Save()

Write-Host "Desktop shortcut created: $lnkPath" -ForegroundColor Green
Write-Host "Targets: $javaw -jar `"$deskJar`"" -ForegroundColor DarkGray
