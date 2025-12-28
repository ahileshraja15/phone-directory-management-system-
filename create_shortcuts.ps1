#!/usr/bin/env pwsh
$ErrorActionPreference = 'Stop'

$desktop = [Environment]::GetFolderPath('Desktop')
$scriptRoot = $PSScriptRoot
if (-not $scriptRoot) { $scriptRoot = Split-Path -Parent $PSCommandPath }
$repoRoot = Split-Path -Parent $scriptRoot
$dist = Join-Path $repoRoot 'dist'

function New-Shortcut($name, $target, $workdir) {
  $shell = New-Object -ComObject WScript.Shell
  $lnk = Join-Path $desktop $name
  $sc = $shell.CreateShortcut($lnk)
  $sc.TargetPath = $target
  $sc.WorkingDirectory = $workdir
  $sc.IconLocation = "$env:WINDIR\System32\shell32.dll, 167"
  $sc.Save()
}

$mobileTarget = (Resolve-Path (Join-Path $dist 'start_mobile_gui.cmd')).Path
$guiTarget    = (Resolve-Path (Join-Path $dist 'start_gui.cmd')).Path
$consoleTarget= (Resolve-Path (Join-Path $dist 'start_console.cmd')).Path

New-Shortcut 'Phone Directory (Mobile).lnk'  $mobileTarget  $dist
New-Shortcut 'Phone Directory (GUI).lnk'     $guiTarget     $dist
New-Shortcut 'Phone Directory (Console).lnk' $consoleTarget $dist

Write-Host "Created desktop shortcuts: 'Phone Directory (Mobile|GUI|Console).lnk'" -ForegroundColor Green
