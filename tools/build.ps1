#Requires -Version 5.1
<#
.SYNOPSIS
    Builds the mod, using a bundled JDK 21 if JAVA_HOME is not already 21+.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools/build.ps1
#>
[CmdletBinding()]
param(
    [string]$GradleHome = "$env:USERPROFILE\.gradle\wrapper\dists\gradle-8.14.3-bin\cv11ve7ro1n3o1j4so8xd9n66\gradle-8.14.3"
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    $java21Candidates = @(
        "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1.1\jbr",
        "C:\Program Files\Android\Android Studio\jbr",
        "C:\Java\jdk-21.0.5",
        "$env:USERPROFILE\.jdks\openjdk-21"
    )

    if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) {
        $found = $java21Candidates | Where-Object { Test-Path (Join-Path $_ 'bin\javac.exe') } | Select-Object -First 1
        if (-not $found) {
            throw "No JDK 21 found. Install one or set JAVA_HOME to a JDK 21."
        }
        $env:JAVA_HOME = $found
        Write-Host "Using JAVA_HOME=$env:JAVA_HOME"
    }

    $gradle = Join-Path $GradleHome 'bin\gradle.bat'
    if (Test-Path $gradle) {
        & $gradle @args build
    } else {
        & (Join-Path $projectRoot 'gradlew.bat') build
    }

    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed with exit code $LASTEXITCODE"
    }

    Get-ChildItem (Join-Path $projectRoot 'build\libs') -Filter '*.jar' |
        Where-Object { $_.Name -notmatch 'sources' } |
        ForEach-Object { Write-Host "Built: $($_.FullName)" }
}
finally {
    Pop-Location
}
