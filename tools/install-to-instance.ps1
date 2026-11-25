#Requires -Version 5.1
<#
.SYNOPSIS
    Copies the newest built jar into a CurseForge instance's mods folder.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools/install-to-instance.ps1
    powershell -ExecutionPolicy Bypass -File tools/install-to-instance.ps1 -Instance "C:\...\My Create (With Let it walk)"
#>
[CmdletBinding()]
param(
    [string]$Instance = "C:\Users\$env:USERNAME\curseforge\minecraft\Instances\My Create"
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

$jar = Get-ChildItem (Join-Path $projectRoot 'build\libs') -Filter 'stressroutes-*.jar' -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notmatch 'sources' } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $jar) {
    throw "No built jar found. Run tools/build.ps1 first."
}

$mods = Join-Path $Instance 'mods'
if (-not (Test-Path $mods)) {
    throw "Mods folder not found: $mods"
}

Get-ChildItem $mods -Filter 'stressroutes-*.jar' | Remove-Item -Force
Copy-Item $jar.FullName -Destination $mods -Force
Write-Host "Installed $($jar.Name) into $mods"
Write-Host "Start the instance from CurseForge, open Xaero's World Map and look for the 'Stress' button on the left."
