@echo off
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\apresentacao\executar.ps1" %*
exit /b %errorlevel%
