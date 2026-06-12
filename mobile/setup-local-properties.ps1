# Writes mobile/local.properties with sdk.dir for Gradle Android builds.
$sdkCandidates = @(
    "$env:LOCALAPPDATA\Android\Sdk",
    "$env:USERPROFILE\AppData\Local\Android\Sdk",
    "$env:ANDROID_HOME",
    "$env:ANDROID_SDK_ROOT",
    "C:\Android\Sdk"
) | Where-Object { $_ -and (Test-Path $_) } | Select-Object -Unique

$sdk = $null
foreach ($candidate in $sdkCandidates) {
    if (Test-Path (Join-Path $candidate "platform-tools")) {
        $sdk = $candidate
        break
    }
}

if (-not $sdk) {
    Write-Host "Android SDK not found." -ForegroundColor Red
    Write-Host ""
    Write-Host "Install Android Studio, then open it once so the SDK is downloaded:"
    Write-Host "  https://developer.android.com/studio"
    Write-Host ""
    Write-Host "After installation, run this script again."
    exit 1
}

$escaped = $sdk -replace "\\", "/"
$localProperties = Join-Path $PSScriptRoot "local.properties"
"sdk.dir=$escaped" | Set-Content -Path $localProperties -Encoding ASCII

Write-Host "Wrote $localProperties"
Write-Host "sdk.dir=$sdk"
Write-Host ""
Write-Host "You can now run:"
Write-Host "  .\gradlew.bat :androidApp:installDebug"
