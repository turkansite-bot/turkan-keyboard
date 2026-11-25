@echo off
rem Lightweight project launcher (not the official Gradle Wrapper JAR).
rem Uses an installed Gradle only if it is exactly the pinned version; otherwise
rem downloads and checksum-verifies the pinned Gradle distribution.
setlocal
set "APP_HOME=%~dp0"
set "GRADLE_VERSION=8.9"

where gradle >nul 2>nul
if errorlevel 1 goto bootstrap
gradle --version 2>nul | findstr /R /C:"^Gradle %GRADLE_VERSION%$" >nul
if errorlevel 1 goto bootstrap
call gradle -p "%APP_HOME%" %*
exit /b %ERRORLEVEL%

:bootstrap
if defined GRADLE_USER_HOME (set "GH=%GRADLE_USER_HOME%") else (set "GH=%USERPROFILE%\.gradle")
set "DIST_ROOT=%GH%\wrapper\dists\gradle-%GRADLE_VERSION%-bin\turkan-bootstrap"
set "GRADLE_BIN=%DIST_ROOT%\gradle-%GRADLE_VERSION%\bin\gradle.bat"
set "URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip"
set "ZIP=%DIST_ROOT%\gradle-%GRADLE_VERSION%-bin.zip"

if exist "%GRADLE_BIN%" goto run
if not exist "%DIST_ROOT%" mkdir "%DIST_ROOT%"
echo Downloading Gradle %GRADLE_VERSION%...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; $wc=New-Object Net.WebClient; $wc.DownloadFile('%URL%','%ZIP%'); $exp=$wc.DownloadString('%URL%.sha256').Trim().Split(' ')[0].ToLower(); $act=(Get-FileHash '%ZIP%' -Algorithm SHA256).Hash.ToLower(); if ($exp -ne $act) { Remove-Item '%ZIP%'; throw 'Gradle download checksum mismatch' }; Expand-Archive -Force '%ZIP%' '%DIST_ROOT%'; Remove-Item '%ZIP%'"
if errorlevel 1 exit /b 1

:run
call "%GRADLE_BIN%" -p "%APP_HOME%" %*
exit /b %ERRORLEVEL%
