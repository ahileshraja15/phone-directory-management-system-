#!/usr/bin/env pwsh
$ErrorActionPreference = 'Stop'

$ScriptDir = Split-Path -Parent $PSCommandPath
$Root = Split-Path -Parent $ScriptDir
$OutDir = Join-Path $Root "out"
$SrcDir = Join-Path $Root "src/main/java"
$DistDir = Join-Path $Root "dist"
$ClassesDir = Join-Path $DistDir "classes"

# Compile
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$sources = Get-ChildItem -Recurse -Filter *.java -Path $SrcDir | ForEach-Object { $_.FullName }
if (-not $sources -or $sources.Count -eq 0) { Write-Error "No Java sources found under $SrcDir" }
& javac -d $OutDir @sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

# Recreate dist/classes
if (Test-Path $DistDir) { Remove-Item -Recurse -Force $DistDir }
New-Item -ItemType Directory -Force -Path $ClassesDir | Out-Null

# Copy compiled classes
Copy-Item -Recurse -Force (Join-Path $OutDir "*") $ClassesDir

# Create runner scripts in dist
$runnerConsole = @"
#!/usr/bin/env pwsh
# Run console app
java -cp "`$PSScriptRoot\classes" com.example.phonedir.App
"@
Set-Content -Path (Join-Path $DistDir "run_console.ps1") -Value $runnerConsole -Encoding UTF8

$runnerGui = @"
#!/usr/bin/env pwsh
# Run GUI app (basic)
java -cp "`$PSScriptRoot\classes" com.example.phonedir.GuiApp
"@
Set-Content -Path (Join-Path $DistDir "run_gui.ps1") -Value $runnerGui -Encoding UTF8

$runnerMobile = @"
#!/usr/bin/env pwsh
# Run GUI app (mobile-like)
java -cp "`$PSScriptRoot\classes" com.example.phonedir.GuiAppMobile
"@
Set-Content -Path (Join-Path $DistDir "run_mobile.ps1") -Value $runnerMobile -Encoding UTF8

Write-Host "Saved runnable application to ${DistDir}" -ForegroundColor Green
Get-ChildItem $DistDir | Format-Table -AutoSize