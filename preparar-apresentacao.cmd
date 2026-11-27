@echo off
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\apresentacao\preparar.ps1" %*
exit /b %errorlevel%
