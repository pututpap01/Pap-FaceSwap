@echo off
REM ==========================================
REM FaceMorph AI - Automated Git Commit & Push (Windows)
REM ==========================================

IF NOT EXIST ".git" (
    echo [INFO] Inisialisasi Git repository...
    git init
    git branch -M main
)

SET COMMIT_MSG=%~1
IF "%COMMIT_MSG%"=="" (
    SET COMMIT_MSG=update: sinkronisasi otomatis %date% %time%
)

echo [1/3] Menambahkan file yang diubah...
git add .

echo [2/3] Membuat commit: "%COMMIT_MSG%"
git commit -m "%COMMIT_MSG%"

echo [3/3] Melakukan push ke GitHub...
FOR /F "tokens=*" %%g IN ('git branch --show-current') do (SET CURRENT_BRANCH=%%g)
IF "%CURRENT_BRANCH%"=="" (SET CURRENT_BRANCH=main)

git push origin %CURRENT_BRANCH%

echo.
echo [SELESAI] Kode berhasil dikirim ke GitHub!
