@echo off
title demo6-cloud Microservices
cd /d "%~dp0"

echo ============================================
echo   demo6-cloud Full Microservice Mode
echo   Nacos + Redis + Spring Cloud Gateway
echo ============================================
echo.

echo [1/5] Starting Nacos (port 8848)...
start "Nacos" cmd /c "cd nacos\bin && startup.cmd -m standalone"
echo Waiting for Nacos (15s)...
timeout /t 15 /nobreak >nul

echo [2/5] Installing modules (first time)...
call mvnw install -DskipTests -q
if errorlevel 1 (
    echo [ERROR] Install failed!
    pause
    exit /b 1
)
echo Install OK

echo [3/5] Check MySQL (localhost:3306 / game_trade)...

echo [4/5] Starting services...

echo   Gateway (8080)...
start "Gateway-8080" cmd /c "cd demo6-gateway && ..\mvnw spring-boot:run -q"
timeout /t 5 /nobreak >nul

echo   UserService (8081)...
start "UserService-8081" cmd /c "cd demo6-user-service && ..\mvnw spring-boot:run -q"
timeout /t 3 /nobreak >nul

echo   AccountService (8082)...
start "AccountService-8082" cmd /c "cd demo6-account-service && ..\mvnw spring-boot:run -q"
timeout /t 3 /nobreak >nul

echo   OrderService (8083)...
start "OrderService-8083" cmd /c "cd demo6-order-service && ..\mvnw spring-boot:run -q"
timeout /t 3 /nobreak >nul

echo   BargainService (8084)...
start "BargainService-8084" cmd /c "cd demo6-bargain-service && ..\mvnw spring-boot:run -q"

echo.
echo ============================================
echo   Starting... wait 40-60 seconds
echo.
echo   Frontend:  http://localhost:8080
echo   Nacos:     http://localhost:8848/nacos (nacos/nacos)
echo ============================================
pause
call stop-all.bat
