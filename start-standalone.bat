@echo off
chcp 65001 >nul
title demo6-cloud Standalone 模式
echo ============================================
echo   demo6-cloud Standalone 模式一键启动
echo   （跳过 Nacos，使用 Redis 缓存）
echo ============================================
echo.

REM 检查 MySQL
echo [0] 请确保 MySQL 已启动，数据库 game_trade 已初始化
echo     如未初始化，请运行: mysql -uroot -p123456 ^< schema.sql
echo.

REM 先编译
echo [1] 正在编译项目...
cd /d "%~dp0"
call mvn compile -DskipTests -q
if errorlevel 1 (
    echo [错误] 编译失败!
    pause
    exit /b 1
)
echo     编译完成

REM 启动服务
echo.
echo [2] 启动微服务 (Standalone Profile)...
echo.

echo 启动 Gateway (8080) - 统一入口...
start "Gateway-8080" cmd /c "cd /d %~dp0demo6-gateway && mvn spring-boot:run -Dspring-boot.run.profiles=standalone -q"
echo 等待 Gateway 启动...
timeout /t 8 /nobreak >nul

echo 启动 UserService (8081) - 用户服务...
start "UserService-8081" cmd /c "cd /d %~dp0demo6-user-service && mvn spring-boot:run -Dspring-boot.run.profiles=standalone -q"
timeout /t 5 /nobreak >nul

echo 启动 AccountService (8082) - 账号服务...
start "AccountService-8082" cmd /c "cd /d %~dp0demo6-account-service && mvn spring-boot:run -Dspring-boot.run.profiles=standalone -q"
timeout /t 5 /nobreak >nul

echo 启动 OrderService (8083) - 订单服务...
start "OrderService-8083" cmd /c "cd /d %~dp0demo6-order-service && mvn spring-boot:run -Dspring-boot.run.profiles=standalone -q"
timeout /t 5 /nobreak >nul

echo 启动 BargainService (8084) - 还价服务...
start "BargainService-8084" cmd /c "cd /d %~dp0demo6-bargain-service && mvn spring-boot:run -Dspring-boot.run.profiles=standalone -q"

echo.
echo ============================================
echo   全部服务启动中，请等待 30-60 秒
echo.
echo   Gateway (入口):   http://localhost:8080
echo   UserService:      http://localhost:8081
echo   AccountService:   http://localhost:8082
echo   OrderService:     http://localhost:8083
echo   BargainService:   http://localhost:8084
echo.
echo   用户端: http://localhost:8080/index.html
echo   管理端: http://localhost:8080/game-account-manager.html
echo ============================================
echo.
echo 按任意键关闭所有服务...
pause >nul

REM 停止所有服务
call "%~dp0stop-all.bat"
