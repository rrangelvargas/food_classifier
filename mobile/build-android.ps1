$jdk = "C:\Program Files\Java\jdk-26.0.1"

if (-not (Test-Path "$jdk\bin\java.exe")) {
    Write-Error "JDK not found at $jdk. Update the path in build-android.ps1 or set JAVA_HOME."
    exit 1
}

$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$env:Path"

& "$PSScriptRoot\gradlew.bat" @args
