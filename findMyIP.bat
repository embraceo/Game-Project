@echo off
echo This script will display your network information to help with multiplayer setup.
echo Look for the "IPv4 Address" entry below - this is what other players need to connect to.
echo.
echo ================ NETWORK INFORMATION ================
ipconfig | findstr /i "IPv4"
echo =====================================================
echo.
echo Have other players edit their ConnectToRemoteServer.bat file
echo to use your IP address.
echo.
pause
