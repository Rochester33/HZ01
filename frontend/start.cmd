@echo off
setlocal

cd /d "%~dp0"
set "PROJECT_DIR=%~dp0"
if "%PROJECT_DIR:~-1%"=="\" set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"

echo [HZ-01 Frontend] Project dir: %PROJECT_DIR%
echo [HZ-01 Frontend] Starting Spring Boot...

java -classpath ".mvn\wrapper\maven-wrapper.jar" ^
    "-Dmaven.multiModuleProjectDirectory=%PROJECT_DIR%" ^
    org.apache.maven.wrapper.MavenWrapperMain ^
    spring-boot:run

echo.
echo [HZ-01 Frontend] Process exited with code %ERRORLEVEL%
pause
