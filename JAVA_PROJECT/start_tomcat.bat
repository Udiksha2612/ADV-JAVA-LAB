@echo off
setlocal
echo ========================================================
echo Starting Apache Tomcat 11 for ShopSphere...
echo ========================================================

rem Auto-detect WAR file location
set "WAR_FILE="
if exist "%~dp0ShopSphere\target\ShopSphere.war" (
    set "WAR_FILE=%~dp0ShopSphere\target\ShopSphere.war"
) else if exist "%~dp0JAVA_PROJECT\ShopSphere\target\ShopSphere.war" (
    set "WAR_FILE=%~dp0JAVA_PROJECT\ShopSphere\target\ShopSphere.war"
)

rem Tomcat location: uses CATALINA_HOME if already set, otherwise this default path
if not defined CATALINA_HOME set "CATALINA_HOME=C:\apache-tomcat-11.0.26"
if not exist "%CATALINA_HOME%\bin\startup.bat" (
    echo [ERROR] Tomcat not found at "%CATALINA_HOME%".
    echo Set CATALINA_HOME to your Tomcat 11 folder first, for example:
    echo    set CATALINA_HOME=C:\apache-tomcat-11.0.26
    pause
    exit /b 1
)

if not "%WAR_FILE%"=="" (
    echo Deploying latest ShopSphere.war to Tomcat webapps...
    copy /Y "%WAR_FILE%" "%CATALINA_HOME%\webapps\"
) else (
    echo [WARNING] ShopSphere.war not found yet in target. Build it with: mvn clean package
)

echo.
echo Launching Tomcat Server in new window...
echo Once Tomcat says "Server startup in [X] milliseconds", open:
echo.
echo    http://localhost:8080/ShopSphere/
echo.
start "Apache Tomcat 11 - ShopSphere" "%CATALINA_HOME%\bin\startup.bat"
echo Tomcat window opened! You can close this terminal.
pause
