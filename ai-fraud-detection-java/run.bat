@echo off
setlocal enabledelayedexpansion
echo ==========================================================
echo   Starting SENTINEL AI Fraud Defense System (Supabase)
echo ==========================================================

cd /d "%~dp0"

REM 1. Locate Java
if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
    set "PATH=!JAVA_HOME!\bin;!PATH!"
)

REM 2. Load .env file into environment if present
if exist ".env" (
    echo [CONFIG] Loading .env configuration...
    for /f "usebackq tokens=1,* delims==" %%A in (".env") do (
        set "KEY=%%A"
        set "VAL=%%B"
        if not "!KEY:~0,1!"=="#" (
            if not "!VAL!"=="" set "!KEY!=!VAL!"
        )
    )
) else if exist "..\.env" (
    echo [CONFIG] Loading ..\.env configuration...
    for /f "usebackq tokens=1,* delims==" %%A in ("..\.env") do (
        set "KEY=%%A"
        set "VAL=%%B"
        if not "!KEY:~0,1!"=="#" (
            if not "!VAL!"=="" set "!KEY!=!VAL!"
        )
    )
)

REM 3. Launch Java Application
if exist "SentinelFraudDefense.jar" (
    java -jar SentinelFraudDefense.jar
) else (
    java -cp "bin;lib\slf4j-api.jar;lib\slf4j-simple.jar;lib\*" com.sentinel.Main
)

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Application exited with code %ERRORLEVEL%
    pause
)
