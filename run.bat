@echo off
setlocal enabledelayedexpansion
if not exist build mkdir build

> build\sources.txt (
  for /r "src\main\java" %%F in (*.java) do (
    set "filepath=%%F"
    set "filepath=!filepath:\=/!"
    echo "!filepath!"
  )
)

javac --add-modules jdk.compiler -d build @build\sources.txt
if errorlevel 1 exit /b 1

java --add-modules jdk.compiler -cp build com.codeinsight.Main %*
