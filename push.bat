@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"
title REDZON - Push and Build APK

where git >nul 2>&1
if errorlevel 1 (
  echo Git is not installed. Install Git for Windows first:
  echo https://git-scm.com/download/win
  pause
  exit /b 1
)

if not exist ".git" (
  git init
  git branch -M main
)

for /f "delims=" %%R in ('git remote get-url origin 2^>nul') do set "REPO_URL=%%R"
if not defined REPO_URL (
  set /p "REPO_URL=Enter GitHub repository URL: "
  if not defined REPO_URL (
    echo Repository URL is required.
    pause
    exit /b 1
  )
  git remote add origin "!REPO_URL!"
)

echo.
echo Adding project files...
git add .
git commit -m "Update REDZON Android app" >nul 2>&1
if errorlevel 1 echo No new changes to commit.

echo Pushing to GitHub...
git branch -M main
git push -u origin main
if errorlevel 1 (
  echo Push failed. Check your GitHub login and repository URL.
  pause
  exit /b 1
)

echo.
echo Push complete. GitHub Actions is building the APK.

where gh >nul 2>&1
if errorlevel 1 goto OPEN_ACTIONS

for /f "delims=" %%I in ('gh run list --workflow build-apk.yml --limit 1 --json databaseId --jq ".[0].databaseId" 2^>nul') do set "RUN_ID=%%I"
if not defined RUN_ID goto OPEN_ACTIONS

echo Waiting for APK build (this may take a few minutes)...
gh run watch !RUN_ID! --exit-status
if errorlevel 1 (
  echo Build failed. Open GitHub Actions to see the error.
  goto OPEN_ACTIONS
)

if not exist "apk" mkdir "apk"
gh run download !RUN_ID! -n REDZON-debug-apk -D apk
if errorlevel 1 goto OPEN_ACTIONS

echo.
echo APK downloaded to:
echo %~dp0apk
start "" "%~dp0apk"
pause
exit /b 0

:OPEN_ACTIONS
for /f "tokens=2 delims=^/" %%O in ("!REPO_URL:https://github.com/=!") do set "OWNER=%%O"
set "ACTIONS_URL=!REPO_URL!"
set "ACTIONS_URL=!ACTIONS_URL:.git=!/actions"
echo.
echo Open this page to download the APK artifact:
echo !ACTIONS_URL!
start "" "!ACTIONS_URL!"
pause
