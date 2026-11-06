@echo off
setlocal enabledelayedexpansion
if not exist build mkdir build
if not exist build\test mkdir build\test

> build\sources_all.txt (
  for /r "src\main\java" %%F in (*.java) do (
    set "filepath=%%F"
    set "filepath=!filepath:\=/!"
    echo "!filepath!"
  )
  for /r "src\test\java" %%F in (*.java) do (
    set "filepath=%%F"
    set "filepath=!filepath:\=/!"
    echo "!filepath!"
  )
)

echo Compiling source and test files...
javac --add-modules jdk.compiler -d build\test @build\sources_all.txt
if errorlevel 1 exit /b 1

echo Running automated test suite...
java --add-modules jdk.compiler -ea -cp build\test com.codeinsight.TestRunner %*

