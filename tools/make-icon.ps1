#Requires -Version 5.1
<#
.SYNOPSIS
    Regenerates src/main/resources/icon.png (128x128) for Stress Routes.

.DESCRIPTION
    Pure geometry: a dark map background, a green kinetic network with three nodes and a
    generator block. No third-party art.
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$projectRoot = Split-Path -Parent $PSScriptRoot
$output = Join-Path $projectRoot 'src\main\resources\icon.png'
New-Item -ItemType Directory -Force -Path (Split-Path $output) | Out-Null

$size = 128
$bitmap = New-Object System.Drawing.Bitmap($size, $size)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$graphics.Clear([System.Drawing.Color]::FromArgb(255, 12, 20, 15))

# Faint map grid
$gridPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(24, 110, 200, 130), 1)
for ($i = 16; $i -lt $size; $i += 24) {
    $graphics.DrawLine($gridPen, $i, 0, $i, $size)
    $graphics.DrawLine($gridPen, 0, $i, $size, $i)
}
$gridPen.Dispose()

# Border
$borderPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 42, 62, 48), 3)
$graphics.DrawRectangle($borderPen, 1.5, 1.5, $size - 4, $size - 4)
$borderPen.Dispose()

# Kinetic network: two horizontal runs and a vertical drop, with a dark halo.
$haloPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(180, 6, 14, 8), 9)
$haloPen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
$linePen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 61, 220, 106), 5)
$linePen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round

$path = @(
    (New-Object System.Drawing.PointF(22, 40)),
    (New-Object System.Drawing.PointF(78, 40)),
    (New-Object System.Drawing.PointF(78, 82)),
    (New-Object System.Drawing.PointF(106, 82))
)
$graphics.DrawLines($haloPen, $path)
$graphics.DrawLines($linePen, $path)
$haloPen.Dispose()
$linePen.Dispose()

# Nodes: source (bright), consumer (yellow-green), transmission (dim)
$outlineBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(220, 6, 14, 8))
$sourceBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 107, 255, 158))
$consumerBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 200, 232, 90))
$transmissionBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 46, 143, 82))

foreach ($node in @(
    @{ X = 22; Y = 40; B = $sourceBrush },
    @{ X = 78; Y = 40; B = $transmissionBrush },
    @{ X = 78; Y = 82; B = $transmissionBrush },
    @{ X = 106; Y = 82; B = $consumerBrush }
)) {
    $graphics.FillEllipse($outlineBrush, ($node.X - 13), ($node.Y - 13), 26, 26)
    $graphics.FillEllipse($node.B, ($node.X - 10), ($node.Y - 10), 20, 20)
}

$outlineBrush.Dispose()
$sourceBrush.Dispose()
$consumerBrush.Dispose()
$transmissionBrush.Dispose()

$graphics.Dispose()
$bitmap.Save($output, [System.Drawing.Imaging.ImageFormat]::Png)
$bitmap.Dispose()

Write-Host "Wrote $output"
