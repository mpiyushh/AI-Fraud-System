@echo off
setlocal enabledelayedexpansion
echo ========================================================
echo   Building SENTINEL AI Fraud Defense Executable Fat JAR
echo ========================================================

cd /d "%~dp0"

REM 1. Locate JDK
if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\javac.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
    set "PATH=!JAVA_HOME!\bin;!PATH!"
) else if not defined JAVA_HOME (
    for /f "delims=" %%I in ('where javac 2^>nul') do (
        set "JAVAC_EXE=%%I"
    )
)

echo [1/4] Cleaning previous build artifacts...
if exist "SentinelFraudDefense.jar" del /f /q "SentinelFraudDefense.jar"
if exist "bin" rd /s /q "bin"
mkdir bin

echo [2/4] Compiling Java source code with Supabase PostgreSQL JDBC...
set "CP=lib\postgresql-42.7.3.jar;lib\slf4j-api.jar;lib\slf4j-simple.jar"

dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -cp "%CP%" -d bin @sources.txt
set COMPILE_ERR=%ERRORLEVEL%
del /f /q sources.txt

if %COMPILE_ERR% NEQ 0 (
    echo [ERROR] Compilation failed!
    exit /b %COMPILE_ERR%
)

echo [3/4] Extracting PostgreSQL and SLF4J dependencies into fat package...
cd bin
jar -xf ..\lib\postgresql-42.7.3.jar
jar -xf ..\lib\slf4j-api.jar
jar -xf ..\lib\slf4j-simple.jar
cd ..

echo [4/4] Packaging standalone fat JAR SentinelFraudDefense.jar...
jar -cfe SentinelFraudDefense.jar com.sentinel.Main -C bin .

echo ========================================================
echo BUILD SUCCESSFUL!
echo Output: SentinelFraudDefense.jar
echo ========================================================
