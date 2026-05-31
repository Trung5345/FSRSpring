@echo off
setlocal enabledelayedexpansion

set BACKEND_PORT=8080
set FRONTEND_PORT=3000
set ADMIN_PORT=3001

pushd "%~dp0.."
set ROOT_DIR=%CD%
popd

echo [stop] Stopping all FSRSpring services...

:: Kill processes on dev ports
for %%P in (%BACKEND_PORT% %FRONTEND_PORT% %ADMIN_PORT%) do (
    for /f "tokens=5" %%I in ('netstat -ano 2^>nul ^| findstr /R /C:":%%P .*LISTENING"') do (
        if not "%%I"=="0" (
            echo [stop] Killing process on port %%P ^(PID %%I^)
            taskkill /F /PID %%I >nul 2>&1
        )
    )
)

:: Close the frontend windows by title
taskkill /FI "WINDOWTITLE eq FSR User UI :3000" /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq FSR Admin UI :3001" /F >nul 2>&1

:: Stop Docker infrastructure
echo [stop] Stopping Docker infrastructure...
docker compose -f "%ROOT_DIR%\docker-compose.yml" stop mysql redis libretranslate >nul 2>&1

echo [stop] Done.
