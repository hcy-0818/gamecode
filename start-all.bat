@echo off
chcp 65001 >nul
echo ============================================
echo   demo6-cloud 微服务一键启动脚本
echo ============================================
echo.

REM 检查 Nacos
if not exist "nacos-server\" (
    if exist "nacos-server.zip" (
        echo [1/3] 正在解压 Nacos...
        powershell -Command "Expand-Archive -Path 'nacos-server.zip' -DestinationPath '.' -Force"
        ren nacos nacos-server 2>nul
    ) else (
        echo [错误] 未找到 nacos-server.zip，请先下载 Nacos
        echo 下载地址: https://github.com/alibaba/nacos/releases
        pause
        exit /b 1
    )
)

REM 启动 Nacos
echo [1/3] 启动 Nacos (端口 8848)...
start "Nacos-Server" cmd /c "cd nacos-server\bin && startup.cmd -m standalone"

REM 等待 Nacos 启动
echo 等待 Nacos 启动 (约15秒)...
timeout /t 15 /nobreak >nul

REM 检查 Redis
echo [2/3] 检查 Redis...
redis-cli ping >nul 2>&1
if errorlevel 1 (
    echo [警告] Redis 未启动，尝试启动...
    start "Redis" cmd /c "redis-server"
    timeout /t 5 /nobreak >nul
    redis-cli ping >nul 2>&1
    if errorlevel 1 (
        echo [错误] Redis 启动失败，请确保已安装 Redis for Windows
        echo 下载地址: https://github.com/microsoftarchive/redis/releases
        pause
        exit /b 1
    )
)
echo Redis 连接正常

REM 初始化数据库
echo [2.5/3] 初始化数据库...
echo 请确保 MySQL 已启动，数据库 game_trade 已创建
echo 如果尚未初始化，请在 MySQL 中运行 schema.sql 和 data.sql

REM 启动微服务
echo [3/3] 启动微服务...

echo 启动 Gateway (8080)...
start "Gateway-8080" cmd /c "cd demo6-gateway && mvn spring-boot:run"
timeout /t 3 /nobreak >nul

echo 启动 UserService (8081)...
start "UserService-8081" cmd /c "cd demo6-user-service && mvn spring-boot:run"
timeout /t 3 /nobreak >nul

echo 启动 AccountService (8082)...
start "AccountService-8082" cmd /c "cd demo6-account-service && mvn spring-boot:run"
timeout /t 3 /nobreak >nul

echo 启动 OrderService (8083)...
start "OrderService-8083" cmd /c "cd demo6-order-service && mvn spring-boot:run"
timeout /t 3 /nobreak >nul

echo 启动 BargainService (8084)...
start "BargainService-8084" cmd /c "cd demo6-bargain-service && mvn spring-boot:run"

echo.
echo ============================================
echo   全部服务启动中，请等待约 30-60 秒
echo.
echo   Gateway:        http://localhost:8080
echo   UserService:    http://localhost:8081
echo   AccountService:  http://localhost:8082
echo   OrderService:   http://localhost:8083
echo   BargainService: http://localhost:8084
echo   Nacos 控制台:    http://localhost:8848/nacos (nacos/nacos)
echo.
echo   用户端: http://localhost:8080/index.html
echo   管理端: http://localhost:8080/game-account-manager.html
echo ============================================
echo.
pause
