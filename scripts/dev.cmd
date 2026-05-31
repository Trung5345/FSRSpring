@echo off
setlocal enabledelayedexpansion

set BACKEND_PORT=8080
set FRONTEND_PORT=3000
set ADMIN_PORT=3001

:: Resolve root directory (one level up from scripts\)
pushd "%~dp0.."
set ROOT_DIR=%CD%
popd

:: ── Free a port: kill Docker containers and local processes ─────────────────
goto :main

:free_port
    set _PORT=%1
    echo [dev] Checking port %_PORT%...

    :: Kill Docker containers mapping this port
    for /f "tokens=1" %%C in ('docker ps --format "{{.Names}} {{.Ports}}" 2^>nul ^| findstr ":%_PORT%->"') do (
        echo   Stopping Docker container: %%C
        docker stop %%C >nul 2>&1
    )

    :: Kill local processes listening on this port
    for /f "tokens=5" %%P in ('netstat -ano 2^>nul ^| findstr /R /C:":%_PORT% .*LISTENING"') do (
        if not "%%P"=="0" (
            for /f "tokens=1" %%N in ('tasklist /fi "PID eq %%P" /fo csv /nh 2^>nul ^| findstr /v "^$"') do (
                echo   Killing process %%P ^(%%N^) on port %_PORT%
            )
            taskkill /F /PID %%P >nul 2>&1
        )
    )
    goto :eof

:: ── Main ────────────────────────────────────────────────────────────────────
:main

echo [dev] Starting infrastructure services...
docker compose -f "%ROOT_DIR%\docker-compose.yml" up -d mysql redis libretranslate >nul 2>&1

:: Free all ports before starting
call :free_port %BACKEND_PORT%
call :free_port %FRONTEND_PORT%
call :free_port %ADMIN_PORT%

:: Start user frontend in a new window (port 3000)
echo [dev] Starting user frontend  --^> http://localhost:%FRONTEND_PORT%
start "FSR User UI :3000" cmd /k "cd /d "%ROOT_DIR%\frontend" && pnpm dev -p %FRONTEND_PORT%"

:: Start admin frontend in a new window (port 3001)
echo [dev] Starting admin frontend --^> http://localhost:%ADMIN_PORT%
start "FSR Admin UI :3001" cmd /k "cd /d "%ROOT_DIR%\admin-frontend" && npm run dev -- -p %ADMIN_PORT%"

echo.
echo [dev] ================================================
echo [dev]   User UI  --^> http://localhost:%FRONTEND_PORT%
echo [dev]   Admin UI --^> http://localhost:%ADMIN_PORT%
echo [dev]   API      --^> http://localhost:%BACKEND_PORT%
echo [dev] ================================================
echo.
echo [dev] Starting Spring Boot (local profile)...
echo [dev] Press Ctrl+C to stop Spring Boot.
echo [dev] Close the other two windows to stop frontends.
echo.

:: Run Spring Boot in this window (foreground, clean first to avoid stale class files)
cd /d "%ROOT_DIR%"
call mvnw.cmd clean spring-boot:run -Dspring-boot.run.profiles=local %*
