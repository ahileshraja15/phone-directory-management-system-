#!/usr/bin/env pwsh
$ErrorActionPreference = 'Stop'

$scriptRoot = $PSScriptRoot
if (-not $scriptRoot) { $scriptRoot = Split-Path -Parent $PSCommandPath }
$repoRoot = Split-Path -Parent $scriptRoot
$dist = Join-Path $repoRoot 'dist'
$desktop = [Environment]::GetFolderPath('Desktop')
$dest = Join-Path $desktop 'PhoneDirectoryApp.zip'

if (-not (Test-Path $dist)) { Write-Error "dist folder not found: $dist" }

Compress-Archive -Path (Join-Path $dist '*') -DestinationPath $dest -Force

Write-Host "Created: $dest" -ForegroundColor Green
