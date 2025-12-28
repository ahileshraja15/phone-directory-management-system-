#!/usr/bin/env pwsh
$ErrorActionPreference = 'Stop'

$programs = [Environment]::GetFolderPath('Programs')
$scriptRoot = $PSScriptRoot
if (-not $scriptRoot) { $scriptRoot = Split-Path -Parent $PSCommandPath }
$repoRoot = Split-Path -Parent $scriptRoot
$dist = Join-Path $repoRoot 'dist'
$target = Join-Path $dist 'start.cmd'

if (-not (Test-Path $target)) { Write-Error "Launcher not found: $target" }

$shell = New-Object -ComObject WScript.Shell
$lnkPath = Join-Path $programs 'Phone Directory.lnk'
$sc = $shell.CreateShortcut($lnkPath)
$sc.TargetPath = $target
$sc.WorkingDirectory = $dist
$sc.IconLocation = "$env:WINDIR\System32\shell32.dll, 167"
$sc.Save()

Write-Host "Created Start Menu shortcut: $lnkPath" -ForegroundColor Green
