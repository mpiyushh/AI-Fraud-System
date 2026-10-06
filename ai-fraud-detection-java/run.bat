@echo off
setlocal enabledelayedexpansion
echo Starting SENTINEL AI Fraud Detection System (Supabase PostgreSQL)...

cd /d "%~dp0"

REM 1. Locate Java
if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
    set "PATH=!JAVA_HOME!\bin;!PATH!"
)

REM 2. Run Application
if exist "SentinelFraudDefense.jar" (
    java -jar SentinelFraudDefense.jar
) else (
    java -cp "bin;lib\postgresql-42.7.3.jar;lib\slf4j-api.jar;lib\slf4j-simple.jar;lib\*" com.sentinel.Main
)

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Application exited with code %ERRORLEVEL%
    pause
)
