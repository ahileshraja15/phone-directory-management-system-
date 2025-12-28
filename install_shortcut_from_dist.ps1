#!/usr/bin/env pwsh
$ErrorActionPreference = 'Stop'

# Locate repo dist and copy to %USERPROFILE%\PhoneDirectoryApp
$scriptRoot = $PSScriptRoot
if (-not $scriptRoot) { $scriptRoot = Split-Path -Parent $PSCommandPath }
$repoRoot = Split-Path -Parent $scriptRoot
$dist = Join-Path $repoRoot 'dist'
if (-not (Test-Path $dist)) { Write-Error "dist not found: $dist" }

$dest = Join-Path $env:USERPROFILE 'PhoneDirectoryApp'
New-Item -ItemType Directory -Force -Path $dest | Out-Null
# Copy all dist contents to destination
Copy-Item -Recurse -Force (Join-Path $dist '*') $dest

# Create Start Menu shortcut
$programs = [Environment]::GetFolderPath('Programs')
$target = Join-Path $dest 'start.cmd'
if (-not (Test-Path $target)) { Write-Error "Launcher not found after copy: $target" }

$shell = New-Object -ComObject WScript.Shell
$lnkPath = Join-Path $programs 'Phone Directory.lnk'
$sc = $shell.CreateShortcut($lnkPath)
$sc.TargetPath = $target
$sc.WorkingDirectory = $dest
$sc.IconLocation = "$env:WINDIR\System32\shell32.dll, 167"
$sc.Save()

Write-Host "Installed to: $dest" -ForegroundColor Green
Write-Host "Start Menu shortcut: $lnkPath" -ForegroundColor Green
