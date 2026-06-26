@echo off
echo Stopping all services...

for %%p in (8080 8081 8082 8083 8084 8848) do (
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :%%p ^| findstr LISTENING') do (
        taskkill /F /PID %%a 2>nul
        echo Stopped port %%p (PID: %%a)
    )
)

taskkill /F /IM redis-server.exe 2>nul
echo All services stopped.
pause
