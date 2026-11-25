#Requires -Version 5.1
<#
.SYNOPSIS
    Turns a source logo into the mod icon and the store avatars.

.DESCRIPTION
    Produces:
      src/main/resources/icon.png       128x128  (in-game mod list)
      docs/curseforge/avatar-400.png    400x400  (CurseForge project avatar)
      docs/curseforge/logo-source.png   untouched copy of the source for future edits

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools/prepare-logo.ps1 -Source "C:\Users\me\Downloads\Logo.png"
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$Source
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

if (-not (Test-Path $Source)) {
    throw "Logo not found: $Source"
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$iconPath = Join-Path $projectRoot 'src\main\resources\icon.png'
$curseforgeDir = Join-Path $projectRoot 'docs\curseforge'
New-Item -ItemType Directory -Force -Path (Split-Path $iconPath) | Out-Null
New-Item -ItemType Directory -Force -Path $curseforgeDir | Out-Null

$image = [System.Drawing.Image]::FromFile((Resolve-Path $Source))
try {
    Write-Host "Source: $($image.Width)x$($image.Height) $($image.PixelFormat)"

    function Resize-Logo {
        param([System.Drawing.Image]$Image, [int]$Size, [string]$Destination)

        $bitmap = New-Object System.Drawing.Bitmap($Size, $Size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
        try {
            $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
            $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
            $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
            $graphics.Clear([System.Drawing.Color]::Transparent)
            $graphics.DrawImage($Image, 0, 0, $Size, $Size)
        } finally {
            $graphics.Dispose()
        }
        $bitmap.Save($Destination, [System.Drawing.Imaging.ImageFormat]::Png)
        $bitmap.Dispose()
    }

    Resize-Logo -Image $image -Size 128 -Destination $iconPath
    Resize-Logo -Image $image -Size 400 -Destination (Join-Path $curseforgeDir 'avatar-400.png')
    Copy-Item -LiteralPath (Resolve-Path $Source) -Destination (Join-Path $curseforgeDir 'logo-source.png') -Force
} finally {
    $image.Dispose()
}

Write-Host "icon.png    -> $iconPath"
Write-Host "avatar-400  -> $(Join-Path $curseforgeDir 'avatar-400.png')"
