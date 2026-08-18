package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object SocialShareManager {

    /**
     * Saves a high-resolution bitmap directly to user's public Gallery without watermark.
     */
    fun saveToGallery(
        context: Context,
        bitmap: Bitmap,
        title: String = "FaceMorph_${System.currentTimeMillis()}"
    ): Uri? {
        val filename = "${title}.jpg"
        var fos: OutputStream? = null
        var imageUri: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FaceMorph AI")
                }
                val resolver = context.contentResolver
                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    fos = resolver.openOutputStream(imageUri)
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/FaceMorph AI")
                if (!imagesDir.exists()) imagesDir.mkdirs()
                val image = File(imagesDir, filename)
                fos = FileOutputStream(image)
                imageUri = Uri.fromFile(image)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }

            Toast.makeText(context, "✅ Berhasil disimpan ke Galeri (Tanpa Watermark)!", Toast.LENGTH_SHORT).show()
            return imageUri
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
            return null
        }
    }

    /**
     * Creates a temporary sharable file provider URI.
     */
    fun getShareableUri(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val cachePath = File(context.cacheDir, "shared_images")
            if (!cachePath.exists()) cachePath.mkdirs()
            val file = File(cachePath, "facemorph_share_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Shares image via specific social media app package or fallback to system chooser.
     */
    fun shareToSocialApp(
        context: Context,
        bitmap: Bitmap,
        targetPackage: String? = null,
        caption: String = "Dibuat dengan FaceMorph AI - Face Swap Instan & Gratis tanpa Watermark! 🎭✨"
    ) {
        val uri = getShareableUri(context, bitmap)
        if (uri == null) {
            Toast.makeText(context, "Gagal menyiapkan file share", Toast.LENGTH_SHORT).show()
            return
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (!targetPackage.isNullOrEmpty()) {
            shareIntent.setPackage(targetPackage)
        }

        try {
            if (targetPackage != null) {
                // Direct app launch
                context.startActivity(shareIntent)
            } else {
                // System chooser
                val chooser = Intent.createChooser(shareIntent, "Bagikan Hasil Face Swap")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            // Target package not installed, launch standard chooser
            val fallbackChooser = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, caption)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Bagikan Hasil Face Swap"
            )
            fallbackChooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fallbackChooser)
        }
    }
}
