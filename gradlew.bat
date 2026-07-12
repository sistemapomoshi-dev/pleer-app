@echo off
setlocal

set DIR=%~dp0
set GRADLE_VERSION=8.10.2
set GRADLE_HOME=%DIR%.gradle\wrapper\dists\gradle-%GRADLE_VERSION%-bin\gradle-%GRADLE_VERSION%
set GRADLE_BIN=%GRADLE_HOME%\bin\gradle.bat

if "%JAVA_HOME%"=="" (
  if exist "C:\Program Files\Android\openjdk\jdk-21.0.8" (
    set JAVA_HOME=C:\Program Files\Android\openjdk\jdk-21.0.8
  )
)

if not exist "%GRADLE_BIN%" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $version='%GRADLE_VERSION%'; $project='%DIR%'; $zip=Join-Path $project \".gradle\gradle-$version-bin.zip\"; $root=Join-Path $project \".gradle\wrapper\dists\gradle-$version-bin\"; New-Item -ItemType Directory -Force -Path $root | Out-Null; if ((Test-Path $zip) -and ((Get-Item $zip).Length -lt 1000000)) { Remove-Item $zip -Force }; if (!(Test-Path $zip)) { & curl.exe -L --fail \"https://services.gradle.org/distributions/gradle-$version-bin.zip\" -o $zip }; if ((Get-Item $zip).Length -lt 1000000) { throw 'Gradle archive was not downloaded correctly' }; Expand-Archive -Path $zip -DestinationPath $root -Force"
)

call "%GRADLE_BIN%" %*
exit /b %ERRORLEVEL%
