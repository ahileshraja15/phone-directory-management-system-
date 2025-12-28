#!/usr/bin/env pwsh
$ErrorActionPreference = 'Stop'

$desktop = [Environment]::GetFolderPath('Desktop')
$scriptRoot = $PSScriptRoot
if (-not $scriptRoot) { $scriptRoot = Split-Path -Parent $PSCommandPath }
$repoRoot = Split-Path -Parent $scriptRoot
$dist = Join-Path $repoRoot 'dist'
$startCmd = (Join-Path $dist 'start.cmd')

if (-not (Test-Path $startCmd)) { Write-Error "Launcher not found: $startCmd" }

$shell = New-Object -ComObject WScript.Shell
$lnkPath = Join-Path $desktop 'Phone Directory.lnk'
$sc = $shell.CreateShortcut($lnkPath)
$sc.TargetPath = $startCmd
$sc.WorkingDirectory = $dist
$sc.IconLocation = "$env:WINDIR\System32\shell32.dll, 167"
$sc.Save()

Write-Host "Created desktop shortcut: Phone Directory.lnk" -ForegroundColor Green
