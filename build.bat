@echo off
setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set SRC_DIR=%SCRIPT_DIR%src\main\java
set RES_DIR=%SCRIPT_DIR%src\main\resources
set BUILD_DIR=%SCRIPT_DIR%build
set LIB_DIR=%SCRIPT_DIR%lib
set MOD_ID=diy_the_spire

echo === Building DIY_the_spire ===

if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"

set CLASSPATH=
if exist "%LIB_DIR%" (
    for %%f in ("%LIB_DIR%\*.jar") do (
        set CLASSPATH=!CLASSPATH!%%f;
    )
)

echo Compiling Java sources...
if exist sources.txt del sources.txt
for /r "%SRC_DIR%" %%f in (*.java) do (
    echo "%%f" >> sources.txt
)
javac -encoding UTF-8 -source 1.8 -target 1.8 -cp "%CLASSPATH%" -d "%BUILD_DIR%" @sources.txt

if errorlevel 1 (
    echo Compilation failed!
    del sources.txt
    exit /b 1
)

echo Copying resources...
xcopy /s /e /y "%RES_DIR%" "%BUILD_DIR%"

echo Creating JAR...
jar -cvf "%SCRIPT_DIR%%MOD_ID%.jar" -C "%BUILD_DIR%" .

if errorlevel 1 (
    echo JAR creation failed!
    del sources.txt
    exit /b 1
)

echo Build successful: %MOD_ID%.jar
rmdir /s /q "%BUILD_DIR%"
del sources.txt

endlocal