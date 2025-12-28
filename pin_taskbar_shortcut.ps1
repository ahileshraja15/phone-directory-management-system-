#!/usr/bin/env pwsh
$ErrorActionPreference = 'Stop'

# Existing Start Menu shortcut (created earlier)
$startMenuLnk = Join-Path $env:APPDATA 'Microsoft\Windows\Start Menu\Programs\Phone Directory.lnk'
if (-not (Test-Path $startMenuLnk)) {
  Write-Error "Start Menu shortcut not found: $startMenuLnk. Run create_start_menu_shortcut.ps1 first."
}

# User Taskbar pinned folder
$taskbarDir = Join-Path $env:APPDATA 'Microsoft\Internet Explorer\Quick Launch\User Pinned\TaskBar'
if (-not (Test-Path $taskbarDir)) {
  Write-Error "Taskbar pinned folder not found: $taskbarDir"
}

$taskbarLnk = Join-Path $taskbarDir 'Phone Directory.lnk'
Copy-Item -Force $startMenuLnk $taskbarLnk

Write-Host "Pinned to taskbar folder: $taskbarLnk" -ForegroundColor Green
Write-Host "If the icon doesn't appear immediately, sign out/in or restart Explorer to refresh the taskbar." -ForegroundColor Yellow
