@echo off
echo Connecting to server - EDIT THIS FILE to change the server IP address (use ipconfig in command prompt and change the IP address)
java --add-exports java.base/java.lang=ALL-UNNAMED --add-exports java.desktop/sun.awt=ALL-UNNAMED --add-exports java.desktop/sun.java2d=ALL-UNNAMED -Dsun.java2d.d3d=false -Dsun.java2d.uiScale=1 myGame.MyGame 192.168.1.100 6868 UDP