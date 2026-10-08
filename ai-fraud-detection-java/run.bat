@echo off
setlocal enabledelayedexpansion
echo ==========================================================
echo   Starting SENTINEL AI Fraud Defense System (Supabase)
echo ==========================================================

cd /d "%~dp0"

REM 1. Locate JDK / JRE
if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\javac.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
    set "PATH=!JAVA_HOME!\bin;!PATH!"
) else if not defined JAVA_HOME (
    for /f "delims=" %%I in ('where javac 2^>nul') do (
        set "JAVAC_EXE=%%I"
    )
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

REM 3. Compile Java Source Code (Ensures UI and source changes are always live)
echo [BUILD] Compiling Java sources...
if not exist "bin" mkdir bin
set "CP=lib\slf4j-api.jar;lib\slf4j-simple.jar"

dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -cp "%CP%" -d bin @sources.txt
set COMPILE_ERR=%ERRORLEVEL%
if exist sources.txt del /f /q sources.txt

if %COMPILE_ERR% NEQ 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b %COMPILE_ERR%
)

REM 4. Launch Desktop Application directly from compiled classes
echo [BOOT] Launching Sentinel Desktop Application (com.sentinel.Main)...
java -cp "bin;lib\slf4j-api.jar;lib\slf4j-simple.jar;lib\*" com.sentinel.Main %*

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Application exited with code %ERRORLEVEL%
    pause
)
