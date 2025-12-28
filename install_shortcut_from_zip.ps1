#!/usr/bin/env pwsh
$ErrorActionPreference = 'Stop'

# Paths
$desktop = [Environment]::GetFolderPath('Desktop')
$candidates = @(
  (Join-Path $desktop 'PhoneDirectoryApp.zip'),
  (Join-Path $env:OneDrive 'Desktop\PhoneDirectoryApp.zip'),
  (Join-Path $env:USERPROFILE 'Desktop\PhoneDirectoryApp.zip')
) | Where-Object { $_ -and (Test-Path $_) }
if ($candidates.Count -gt 0) { $zip = $candidates[0] } else { $zip = (Join-Path $desktop 'PhoneDirectoryApp.zip') }
$dest = Join-Path $env:USERPROFILE 'PhoneDirectoryApp'
$programs = [Environment]::GetFolderPath('Programs')

if (-not (Test-Path $zip)) { Write-Error "Zip not found: $zip" }

# Ensure destination exists and extract
New-Item -ItemType Directory -Force -Path $dest | Out-Null
Expand-Archive -Path $zip -DestinationPath $dest -Force

# Create Start Menu shortcut
$target = Join-Path $dest 'start.cmd'
if (-not (Test-Path $target)) { Write-Error "Launcher not found after extract: $target" }

$shell = New-Object -ComObject WScript.Shell
$lnkPath = Join-Path $programs 'Phone Directory.lnk'
$sc = $shell.CreateShortcut($lnkPath)
$sc.TargetPath = $target
$sc.WorkingDirectory = $dest
$sc.IconLocation = "$env:WINDIR\System32\shell32.dll, 167"
$sc.Save()

Write-Host "Installed to: $dest" -ForegroundColor Green
Write-Host "Start Menu shortcut: $lnkPath" -ForegroundColor Green
