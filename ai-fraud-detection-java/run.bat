@echo off
setlocal
echo Starting SENTINEL AI Fraud Detection System...

set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

cd /d "%~dp0"

if exist "SentinelFraudDefense.jar" (
    java -jar SentinelFraudDefense.jar
) else (
    java -cp "bin;lib\sqlite-jdbc.jar;lib\slf4j-api.jar;lib\slf4j-simple.jar" com.sentinel.Main
)

if %ERRORLEVEL% NEQ 0 (
    pause
)
