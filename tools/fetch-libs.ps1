#Requires -Version 5.1
<#
.SYNOPSIS
    Puts the Xaero jars required for compilation into .\libs.

.DESCRIPTION
    Lookup order:
      1. -Instance / -InstancePath (CurseForge instance) or the default instance path.
      2. Modrinth (downloads the newest 1.21.1 NeoForge build of each project).

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools/fetch-libs.ps1
#>
[CmdletBinding()]
param(
    [string]$Instance = "C:\Users\$env:USERNAME\curseforge\minecraft\Instances\My Create"
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$libs = Join-Path $projectRoot 'libs'
New-Item -ItemType Directory -Force -Path $libs | Out-Null

$targets = @(
    @{ Pattern = 'xaeroworldmap*.jar'; Project = 'xaeros-world-map'; Label = "Xaero's World Map" },
    @{ Pattern = 'xaerominimap*.jar';  Project = 'xaeros-minimap';   Label = "Xaero's Minimap" }
)

foreach ($target in $targets) {
    $existing = Get-ChildItem -Path $libs -Filter $target.Pattern -ErrorAction SilentlyContinue
    if ($existing) {
        Write-Host "[ok] $($target.Label): $($existing[0].Name)"
        continue
    }

    $mods = Join-Path $Instance 'mods'
    if (Test-Path $mods) {
        $found = Get-ChildItem -Path $mods -Filter $target.Pattern -ErrorAction SilentlyContinue
        if ($found) {
            Copy-Item $found[0].FullName -Destination $libs -Force
            Write-Host "[copied] $($found[0].Name)"
            continue
        }
    }

    Write-Host "[download] $($target.Label) from Modrinth..."
    $uri = "https://api.modrinth.com/v2/project/$($target.Project)/version?game_versions=%5B%221.21.1%22%5D&loaders=%5B%22neoforge%22%5D"
    $headers = @{ 'User-Agent' = 'create-stress-routes-build/1.0 (ismadev.es)' }
    $versions = Invoke-RestMethod -Uri $uri -Headers $headers -TimeoutSec 60
    if (-not $versions -or $versions.Count -eq 0) {
        throw "No 1.21.1 NeoForge build found for $($target.Project)"
    }
    $version = $versions[0]
    $file = $version.files | Where-Object { $_.primary } | Select-Object -First 1
    if (-not $file) {
        $file = $version.files[0]
    }
    $destination = Join-Path $libs $file.filename
    Invoke-WebRequest -Uri $file.url -OutFile $destination -Headers $headers -TimeoutSec 300
    Write-Host "[downloaded] $($file.filename)"
}

Write-Host ""
Write-Host "Extracting XaeroLib (nested inside Xaero's World Map jar)..."

$xaeroLib = Get-ChildItem -Path $libs -Filter 'xaerolib*.jar' -ErrorAction SilentlyContinue
if (-not $xaeroLib) {
    $worldMap = Get-ChildItem -Path $libs -Filter 'xaeroworldmap*.jar' -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($worldMap) {
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $zip = [System.IO.Compression.ZipFile]::OpenRead($worldMap.FullName)
        $nested = $zip.Entries | Where-Object { $_.FullName -match '^META-INF/jarjar/xaerolib.*\.jar$' } | Select-Object -First 1
        if ($nested) {
            $destination = Join-Path $libs (Split-Path $nested.FullName -Leaf)
            [System.IO.Compression.ZipFileExtensions]::ExtractToFile($nested, $destination, $true)
            Write-Host "[extracted] $(Split-Path $nested.FullName -Leaf)"
        } else {
            Write-Warning "No nested XaeroLib jar found in $($worldMap.Name)."
        }
        $zip.Dispose()
    }
} else {
    Write-Host "[ok] XaeroLib: $($xaeroLib[0].Name)"
}

Write-Host ""
Write-Host "libs/ ready:"
Get-ChildItem -Path $libs -Filter '*.jar' | ForEach-Object { Write-Host "  $($_.Name)" }
