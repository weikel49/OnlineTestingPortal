@echo off
setlocal EnableDelayedExpansion

REM === Set base paths ===
set "JAVA_HOME=%~dp0jdk24"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "BASE_DIR=%~dp0"
set "SRC_DIR=%BASE_DIR%src"
set "BIN_DIR=%BASE_DIR%bin"
set "LIB_DIR=%BASE_DIR%lib"
set "WEBINF_DIR=%BASE_DIR%WebContent\WEB-INF\classes"

echo 🧹 Cleaning old compiled classes...
if exist "%BIN_DIR%" (
    rmdir /s /q "%BIN_DIR%"
)
mkdir "%BIN_DIR%"

echo 📦 Gathering Java source files...
set "SOURCE_FILES="

for %%f in ("%SRC_DIR%\com\example\testing\*.java") do (
    set "SOURCE_FILES=!SOURCE_FILES! %%f"
)
for %%f in ("%SRC_DIR%\com\example\testing\tests\*.java") do (
    set "SOURCE_FILES=!SOURCE_FILES! %%f"
)

echo 📦 Compiling source and test files...
javac -encoding UTF-8 -d "%BIN_DIR%" -cp "%LIB_DIR%\*;%SRC_DIR%" !SOURCE_FILES!

echo 📁 Copying db.properties...
copy "%WEBINF_DIR%\db.properties" "%BIN_DIR%\" >nul

echo 🚀 Running tests...
java -jar "%LIB_DIR%\junit-platform-console-standalone-1.13.0-M3.jar" ^
  --class-path "%BIN_DIR%;%LIB_DIR%\junit-platform-console-standalone-1.13.0-M3.jar;%LIB_DIR%\mockito-core-5.12.0.jar;%LIB_DIR%\byte-buddy-1.17.5.jar;%LIB_DIR%\byte-buddy-agent-1.17.5.jar;%LIB_DIR%\objenesis-3.4.jar;%LIB_DIR%\jakarta.servlet-api-6.0.0.jar;%LIB_DIR%\mysql-connector-j-9.3.0.jar;%LIB_DIR%\gson-2.10.1.jar" ^
  --scan-class-path ^
  --include-classname ".*Test"

pause
endlocal
