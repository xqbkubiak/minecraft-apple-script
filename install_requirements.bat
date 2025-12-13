@echo off
title Mc-Apple Installer & Fixer
color 07

echo ===================================================
echo      Mc-Apple (xqbkubiak github) Auto Installer & Fixer
echo ===================================================
echo.

:: --- KROK 0: Sprawdzanie Pythona ---
echo [1/4] Checking for Python...
python --version >nul 2>&1
if %errorlevel% equ 0 goto install_reqs

py --version >nul 2>&1
if %errorlevel% equ 0 goto install_reqs

:: Nie znaleziono Pythona - pobieranie
echo [INFO] Python not found!
echo [INFO] Downloading Python 3.14.0 (Automatic)...
curl -o python_installer.exe https://www.python.org/ftp/python/3.14.0/python-3.14.0-amd64.exe
if %errorlevel% neq 0 (
    echo [ERROR] Failed to download Python. Check internet connection.
    pause
    exit
)

echo [INFO] Installing Python 3.14.0...
echo [INFO] Please wait... (This may take a minute)
:: Instalacja z dodaniem do PATH
python_installer.exe /quiet InstallAllUsers=1 PrependPath=1 Include_test=0
if %errorlevel% neq 0 (
    echo [ERROR] Python installation failed!
    del python_installer.exe
    pause
    exit
)

del python_installer.exe
echo [SUCCESS] Python installed! Restarting installer...
timeout /t 2 >nul
start "" "%~f0"
exit

:install_reqs
:: --- KROK 1: Instalacja bibliotek ---
echo.
echo [2/4] Installing requirements...

python -m pip install -r requirements.txt
if %errorlevel% equ 0 goto step2

echo [INFO] Retrying with 'py' launcher...
py -m pip install -r requirements.txt
if %errorlevel% equ 0 goto step2

:: --- KROK 2: Naprawa PATH (jesli nadal bledy) ---
:step2
echo.
echo [3/4] Checking configuration...
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [FIX] Refreshing PATH...
    set "PATH=%PATH%;%LOCALAPPDATA%\Programs\Python\Python314;%LOCALAPPDATA%\Programs\Python\Python314\Scripts"
)

:: --- KROK 3: Tworzenie start_bot.bat ---
:step3
echo.
echo [4/4] Creating 'start_bot.bat' launcher...
(
echo @echo off
echo title Mc-Apple Bot
echo python mc-apple.py
echo if %%errorlevel%% neq 0 py mc-apple.py
echo pause
) > start_bot.bat
echo [OK] Created start_bot.bat

goto success

:success
echo.
echo ===================================================
echo [SUCCESS] Setup completed!
echo.
echo You can now run the bot using 'start_bot.bat'
echo ===================================================
pause
:: End of installer
