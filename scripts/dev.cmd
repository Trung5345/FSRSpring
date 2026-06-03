@echo off
setlocal enabledelayedexpansion

:: ─── Configuration ────────────────────────────────────────────────────────────
set BACKEND_PORT=8080
set PREFERRED_FRONTEND_PORT=3000
set PREFERRED_ADMIN_PORT=3001

pushd "%~dp0.."
set ROOT_DIR=%CD%
popd

set LOG_DIR=%ROOT_DIR%\logs\dev
set INFRA_LOG=%LOG_DIR%\infra.log

:: ─── Parse flags ──────────────────────────────────────────────────────────────
set WITH_TRANSLATE=false
set MAVEN_EXTRA=
for %%A in (%*) do (
    if "%%A"=="--translate" (
        set WITH_TRANSLATE=true
    ) else (
        set "MAVEN_EXTRA=!MAVEN_EXTRA! %%A"
    )
)

:: ─── Setup ────────────────────────────────────────────────────────────────────
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

echo [dev] Checking prerequisites...
echo.

:: ── Java ──────────────────────────────────────────────────────────────────────
where java >nul 2>&1
if errorlevel 1 (
    echo [dev] ERROR: Java not found.
    echo [dev]   Install JDK 17 from: https://adoptium.net/
    echo [dev]   Make sure JAVA_HOME is set and java.exe is in PATH.
    exit /b 1
)
for /f "tokens=*" %%V in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    echo [dev] Java: %%V
    goto :java_ok
)
:java_ok

:: ── Docker ────────────────────────────────────────────────────────────────────
where docker >nul 2>&1
if errorlevel 1 (
    echo [dev] ERROR: Docker not found.
    echo [dev]   Install Docker Desktop from: https://www.docker.com/products/docker-desktop
    exit /b 1
)
docker info >nul 2>&1
if errorlevel 1 (
    echo [dev] ERROR: Docker Desktop is not running.
    echo [dev]   Please start Docker Desktop and wait until it shows "Engine running".
    exit /b 1
)
echo [dev] Docker: running

:: ── Node.js ───────────────────────────────────────────────────────────────────
where node >nul 2>&1
if errorlevel 1 (
    echo [dev] ERROR: Node.js not found.
    echo [dev]   Install Node.js v18+ from: https://nodejs.org/
    exit /b 1
)
for /f %%V in ('node -v 2^>^&1') do echo [dev] Node.js: %%V

:: ── pnpm (optional, falls back to npm) ───────────────────────────────────────
set PNPM_CMD=
where pnpm >nul 2>&1
if not errorlevel 1 (
    set PNPM_CMD=pnpm
    for /f %%V in ('pnpm -v 2^>^&1') do echo [dev] pnpm: %%V
) else (
    echo [dev] pnpm: not found - using npm for user frontend
    echo [dev]   Optional install: npm install -g pnpm
)
echo.

:: ─── Auto-install node_modules if missing ─────────────────────────────────────
if not exist "%ROOT_DIR%\frontend\node_modules" (
    echo [dev] node_modules missing in frontend\ — installing...
    pushd "%ROOT_DIR%\frontend"
    if defined PNPM_CMD (
        call pnpm install
    ) else (
        call npm install
    )
    if errorlevel 1 (
        popd
        echo [dev] ERROR: Failed to install user frontend dependencies.
        exit /b 1
    )
    popd
    echo [dev] User frontend dependencies installed.
)

if not exist "%ROOT_DIR%\admin-frontend\node_modules" (
    echo [dev] node_modules missing in admin-frontend\ — installing...
    pushd "%ROOT_DIR%\admin-frontend"
    call npm install
    if errorlevel 1 (
        popd
        echo [dev] ERROR: Failed to install admin frontend dependencies.
        exit /b 1
    )
    popd
    echo [dev] Admin frontend dependencies installed.
)

:: ─── Stop conflicting Docker containers ───────────────────────────────────────
echo [dev] Stopping app/frontend Docker containers (if running)...
docker compose -f "%ROOT_DIR%\docker-compose.yml" stop app frontend >nul 2>&1

:: ─── Start infrastructure ─────────────────────────────────────────────────────
if "%WITH_TRANSLATE%"=="true" (
    echo [dev] Starting infrastructure: mysql, redis, libretranslate...
    docker compose -f "%ROOT_DIR%\docker-compose.yml" up -d mysql redis libretranslate >> "%INFRA_LOG%" 2>&1
) else (
    echo [dev] Starting infrastructure: mysql, redis (libretranslate skipped)
    echo [dev]   To enable translation features: scripts\dev.cmd --translate
    docker compose -f "%ROOT_DIR%\docker-compose.yml" up -d mysql redis >> "%INFRA_LOG%" 2>&1
    docker compose -f "%ROOT_DIR%\docker-compose.yml" stop libretranslate >> "%INFRA_LOG%" 2>&1
)

:: ─── Wait for MySQL ───────────────────────────────────────────────────────────
echo [dev] Waiting for MySQL on :3307...
set /a MYSQL_ELAPSED=0
:wait_mysql
    docker compose -f "%ROOT_DIR%\docker-compose.yml" exec -T mysql mysqladmin ping -h localhost --silent >> "%INFRA_LOG%" 2>&1
    if not errorlevel 1 goto :mysql_ready
    if %MYSQL_ELAPSED% geq 60 (
        echo.
        echo [dev] ERROR: MySQL did not become ready within 60s.
        echo [dev] Check log: %INFRA_LOG%
        exit /b 1
    )
    timeout /T 2 /NOBREAK >nul
    set /a MYSQL_ELAPSED+=2
    goto :wait_mysql
:mysql_ready
echo [dev] MySQL ready.

:: ─── Read Redis password from .env ────────────────────────────────────────────
set REDIS_PASS=fsr_redis_pass_123
if exist "%ROOT_DIR%\.env" (
    for /f "usebackq tokens=1,* delims==" %%A in ("%ROOT_DIR%\.env") do (
        if "%%A"=="REDISPASSWORD" set "REDIS_PASS=%%B"
    )
)

:: ─── Wait for Redis ───────────────────────────────────────────────────────────
echo [dev] Waiting for Redis on :6380...
set /a REDIS_ELAPSED=0
:wait_redis
    docker compose -f "%ROOT_DIR%\docker-compose.yml" exec -T redis redis-cli -a "%REDIS_PASS%" ping 2>nul | findstr "PONG" >nul
    if not errorlevel 1 goto :redis_ready
    if %REDIS_ELAPSED% geq 30 (
        echo.
        echo [dev] ERROR: Redis did not become ready within 30s.
        echo [dev] Check log: %INFRA_LOG%
        exit /b 1
    )
    timeout /T 1 /NOBREAK >nul
    set /a REDIS_ELAPSED+=1
    goto :wait_redis
:redis_ready
echo [dev] Redis ready.

:: ─── Free ports ───────────────────────────────────────────────────────────────
call :free_port %BACKEND_PORT%
call :free_port %PREFERRED_FRONTEND_PORT%
call :free_port %PREFERRED_ADMIN_PORT%

set FRONTEND_PORT=%PREFERRED_FRONTEND_PORT%
set ADMIN_PORT=%PREFERRED_ADMIN_PORT%

echo.
echo [dev] Infrastructure log: %INFRA_LOG%
echo.

:: ─── Start Spring Boot FIRST (new window, output visible) ────────────────────
echo [dev] Starting Spring Boot (local profile) -^> http://localhost:%BACKEND_PORT%
echo [dev] (first run downloads Maven deps - may take ~60s)
start "FSR Backend :%BACKEND_PORT%" cmd /k "cd /d "%ROOT_DIR%" && call mvnw.cmd clean spring-boot:run -Dspring-boot.run.profiles=local %MAVEN_EXTRA%"

:: ─── Wait for Spring Boot to be healthy ───────────────────────────────────────
echo [dev] Waiting for Spring Boot health check...
set /a BACKEND_ELAPSED=0
:wait_backend
    curl -sf "http://localhost:%BACKEND_PORT%/api/words/count" >nul 2>&1
    if not errorlevel 1 goto :backend_ready
    if %BACKEND_ELAPSED% geq 300 (
        echo.
        echo [dev] ERROR: Spring Boot did not become ready within 5 minutes.
        echo [dev] Check the "FSR Backend" window for error details.
        exit /b 1
    )
    <nul set /p=.
    timeout /T 3 /NOBREAK >nul
    set /a BACKEND_ELAPSED+=3
    goto :wait_backend
:backend_ready
echo.
echo [dev] Spring Boot is ready!
echo.

:: ─── Start frontends (only after backend is healthy) ─────────────────────────
echo [dev] Starting user frontend  -^> http://localhost:%FRONTEND_PORT%
if defined PNPM_CMD (
    start "FSR User UI :%FRONTEND_PORT%" cmd /k "cd /d "%ROOT_DIR%\frontend" && pnpm dev -p %FRONTEND_PORT%"
) else (
    start "FSR User UI :%FRONTEND_PORT%" cmd /k "cd /d "%ROOT_DIR%\frontend" && npm run dev -- -p %FRONTEND_PORT%"
)

echo [dev] Starting admin frontend -^> http://localhost:%ADMIN_PORT%
start "FSR Admin UI :%ADMIN_PORT%" cmd /k "cd /d "%ROOT_DIR%\admin-frontend" && npm run dev -- -p %ADMIN_PORT%"

echo.
echo [dev] ================================================
echo [dev]   User UI  -^> http://localhost:%FRONTEND_PORT%
echo [dev]   Admin UI -^> http://localhost:%ADMIN_PORT%
echo [dev]   API      -^> http://localhost:%BACKEND_PORT%
echo [dev] ================================================
echo.
echo [dev] All three services are running in separate windows.
echo [dev] To stop everything: run  scripts\stop.cmd
echo.
pause
goto :eof

:: ─── :free_port <port> ────────────────────────────────────────────────────────
:free_port
    set "_FP=%1"
    :: Kill Docker containers mapped to this port
    for /f "tokens=1" %%C in ('docker ps --format "{{.Names}} {{.Ports}}" 2^>nul ^| findstr ":%_FP%->"') do (
        echo [dev] Stopping Docker container: %%C (port %_FP%)
        docker stop %%C >nul 2>&1
    )
    :: Kill local processes listening on this port
    for /f "tokens=5" %%P in ('netstat -ano 2^>nul ^| findstr /R /C:":%_FP% .*LISTENING"') do (
        if not "%%P"=="0" if not "%%P"=="" (
            echo [dev] Freeing port %_FP% ^(PID %%P^)
            taskkill /F /PID %%P >nul 2>&1
        )
    )
    goto :eof
