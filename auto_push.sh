#!/usr/bin/env bash

# ==========================================
# FaceMorph AI - Automated Git Commit & Push
# ==========================================

# 1. Pastikan repositori git sudah diinisialisasi
if [ ! -d ".git" ]; then
    echo "⚠️  Repositori git belum diinisialisasi. Menjalankan git init..."
    git init
    git branch -M main
fi

# 2. Cek pesan commit dari argumen
COMMIT_MSG="$1"
if [ -z "$COMMIT_MSG" ]; then
    TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")
    COMMIT_MSG="update: sinkronisasi otomatis ($TIMESTAMP)"
fi

echo "📦 Menambahkan semua perubahan file..."
git add .

# 3. Cek apakah ada perubahan untuk dicommit
if git diff-index --quiet HEAD -- 2>/dev/null; then
    echo "ℹ️  Tidak ada perubahan baru untuk dicommit."
else
    echo "📝 Membuat commit: '$COMMIT_MSG'"
    git commit -m "$COMMIT_MSG"
fi

# 4. Ambil nama branch aktif
BRANCH=$(git branch --show-current)
if [ -z "$BRANCH" ]; then
    BRANCH="main"
fi

# 5. Cek apakah remote 'origin' sudah terdaftar
if git remote get-url origin > /dev/null 2>&1; then
    echo "🚀 Mengirim (push) perubahan ke remote ($BRANCH)..."
    git push origin "$BRANCH"
    echo "✅ Selesai! Perubahan berhasil di-push ke GitHub."
else
    echo ""
    echo "⚠️  Remote 'origin' belum terdaftar."
    echo "👉 Silakan jalankan perintah ini sekali untuk menghubungkan repositori GitHub Anda:"
    echo "   git remote add origin https://github.com/USERNAME/NAMA_REPO.git"
    echo "   git push -u origin $BRANCH"
fi
