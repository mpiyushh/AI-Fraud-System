@echo off
setlocal
echo ========================================================
echo   Building SENTINEL AI Fraud Defense Executable Fat JAR
echo ========================================================

set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"

cd /d "%~dp0"

echo [1/3] Compiling source code...
if not exist "bin" mkdir bin
javac -encoding UTF-8 -cp "lib\sqlite-jdbc.jar;lib\slf4j-api.jar;lib\slf4j-simple.jar" -d bin src\com\sentinel\exception\*.java src\com\sentinel\model\*.java src\com\sentinel\model\detectors\*.java src\com\sentinel\dao\*.java src\com\sentinel\db\*.java src\com\sentinel\service\*.java src\com\sentinel\ui\*.java src\com\sentinel\test\*.java src\com\sentinel\Main.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    exit /b %ERRORLEVEL%
)

echo [2/3] Extracting JDBC and SLF4J dependencies into bin...
cd bin
jar -xf ..\lib\sqlite-jdbc.jar
jar -xf ..\lib\slf4j-api.jar
jar -xf ..\lib\slf4j-simple.jar
cd ..

echo [3/3] Packaging standalone fat JAR SentinelFraudDefense.jar...
jar -cfe SentinelFraudDefense.jar com.sentinel.Main -C bin .

echo ========================================================
echo BUILD SUCCESSFUL!
echo Output: SentinelFraudDefense.jar
echo ========================================================
