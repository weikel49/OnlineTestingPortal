@echo off
setlocal

REM === Set relative paths ===
set "JAVA_HOME=%~dp0jdk24"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "BASE_DIR=%~dp0"
set "TOMCAT_DIR=%BASE_DIR%apache-tomcat"
set "WAR_NAME=OnlineTestingPortal"
set "WEB_SRC=%BASE_DIR%WebContent"
set "DEPLOY_DIR=%TOMCAT_DIR%\webapps\%WAR_NAME%"

echo 🧹 Cleaning previous deployment...
rmdir /S /Q "%DEPLOY_DIR%" 2>nul
mkdir "%DEPLOY_DIR%"

echo 📁 Copying WebContent to Tomcat webapps...
xcopy /E /I /Y "%WEB_SRC%\*" "%DEPLOY_DIR%" >nul

echo 🚀 Starting Tomcat...
set "CATALINA_HOME=%TOMCAT_DIR%"
call "%TOMCAT_DIR%\bin\startup.bat"

echo ⏳ Waiting for server to fully start...
timeout /t 4 >nul

echo 🌐 Opening browser...
start http://localhost:8080/%WAR_NAME%/login.html

pause
endlocal