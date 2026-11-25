#Requires -Version 5.1
<#
.SYNOPSIS
    Uploads the newest built jar to an existing CurseForge project.

.DESCRIPTION
    Uses the CurseForge "upload file" API:
      POST https://minecraft.curseforge.com/api/projects/{projectId}/upload

    The API token is passed as a parameter and never written to disk. Generate one at
    https://legacy.curseforge.com/account/api-tokens

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools/upload-curseforge.ps1 -ProjectId 1234567 -Token "<token>"
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ProjectId,

    [Parameter(Mandatory = $true)]
    [string]$Token,

    [string]$DisplayName = "",
    [string]$ChangelogFile = "",
    [ValidateSet("alpha", "beta", "release")]
    [string]$ReleaseType = "release",
    [string]$GameVersions = "1.21.1,NeoForge,Client,Server"
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http

$projectRoot = Split-Path -Parent $PSScriptRoot
$libs = Join-Path $projectRoot 'build\libs'

$jar = Get-ChildItem $libs -Filter 'stressroutes-*.jar' -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notmatch 'sources' } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $jar) {
    throw "No built jar in $libs. Run tools/build.ps1 first."
}

if (-not $ChangelogFile) {
    $ChangelogFile = Join-Path $projectRoot 'CHANGELOG.md'
}

$changelog = ''
if (Test-Path $ChangelogFile) {
    $changelog = Get-Content $ChangelogFile -Raw
}

if (-not $DisplayName) {
    $DisplayName = [System.IO.Path]::GetFileNameWithoutExtension($jar.Name) -replace '^stressroutes-', ''
}

$metadata = [ordered]@{
    changelog     = $changelog
    changelogType = 'markdown'
    displayName   = $DisplayName
    releaseType   = $ReleaseType
    gameVersions  = $GameVersions.Split(',') | ForEach-Object { $_.Trim() }
    relations     = @{
        projects = @(
            @{ slug = 'create'; type = 'requiredDependency' },
            @{ slug = 'xaeros-world-map'; type = 'optionalDependency' }
        )
    }
}

$metadataJson = $metadata | ConvertTo-Json -Depth 6

Write-Host "Project : $ProjectId"
Write-Host "File    : $($jar.Name) ($($jar.Length) bytes)"
Write-Host "Version : $DisplayName ($ReleaseType)"
Write-Host "Versions: $GameVersions"

$client = New-Object System.Net.Http.HttpClient
$client.Timeout = [TimeSpan]::FromMinutes(10)
$client.DefaultRequestHeaders.Add('X-Api-Token', $Token)

$content = New-Object System.Net.Http.MultipartFormDataContent
$metadataContent = New-Object System.Net.Http.StringContent($metadataJson, [System.Text.Encoding]::UTF8, 'application/json')
$metadataContent.Headers.ContentDisposition = [System.Net.Http.Headers.ContentDispositionHeaderValue]::Parse('form-data; name="metadata"')
$content.Add($metadataContent)

$stream = [System.IO.File]::OpenRead($jar.FullName)
$fileContent = New-Object System.Net.Http.StreamContent($stream)
$fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse('application/java-archive')
$fileContent.Headers.ContentDisposition = [System.Net.Http.Headers.ContentDispositionHeaderValue]::Parse("form-data; name=`"file`"; filename=`"$($jar.Name)`"")
$content.Add($fileContent)

try {
    $uri = "https://minecraft.curseforge.com/api/projects/$ProjectId/upload"
    $response = $client.PostAsync($uri, $content).GetAwaiter().GetResult()
    $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()

    if (-not $response.IsSuccessStatusCode) {
        Write-Error "Upload failed: HTTP $([int]$response.StatusCode) $($response.ReasonPhrase)`n$body"
        exit 1
    }

    Write-Host "Upload OK: $body"
} finally {
    $content.Dispose()
    $stream.Dispose()
    $client.Dispose()
}
