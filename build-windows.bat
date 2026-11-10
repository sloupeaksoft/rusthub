@echo off
rem Сборка Rust HUD на Windows. Нужен JDK 8 (JAVA_HOME должен указывать на него).
cd /d "%~dp0"
if not exist gradle-4.10.3 (
  echo Скачиваю Gradle 4.10.3...
  powershell -NoProfile -Command "Invoke-WebRequest https://services.gradle.org/distributions/gradle-4.10.3-bin.zip -OutFile gradle.zip; Expand-Archive gradle.zip -DestinationPath . -Force; Remove-Item gradle.zip"
)
call gradle-4.10.3\bin\gradle.bat build --no-daemon
echo.
echo Готово: build\libs\rusthud-1.12.2-1.0.0.jar
pause
