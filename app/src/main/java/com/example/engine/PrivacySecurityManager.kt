package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object PrivacySecurityManager {
    private const val PREFS_NAME = "facemorph_privacy_prefs"
    private const val KEY_PIN_HASH = "key_pin_hash"
    private const val KEY_VAULT_KEY = "key_vault_secret_key"
    private const val KEY_EXIF_SCRUB = "key_exif_scrub"
    private const val KEY_LOCAL_ONLY = "key_local_only_mode"

    private const val AES_KEY_SIZE = 256
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    fun isVaultPinSet(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PIN_HASH, null) != null
    }

    fun setVaultPin(context: Context, pin: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hash = hashString(pin)
        prefs.edit().putString(KEY_PIN_HASH, hash).apply()

        // Generate or ensure AES master key exists
        if (prefs.getString(KEY_VAULT_KEY, null) == null) {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(AES_KEY_SIZE, SecureRandom())
            val secretKey = keyGen.generateKey()
            val encodedKey = Base64.encodeToString(secretKey.encoded, Base64.NO_WRAP)
            prefs.edit().putString(KEY_VAULT_KEY, encodedKey).apply()
        }
    }

    fun verifyVaultPin(context: Context, pin: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return storedHash == hashString(pin)
    }

    fun isExifScrubbingEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_EXIF_SCRUB, true)
    }

    fun setExifScrubbingEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_EXIF_SCRUB, enabled).apply()
    }

    fun isLocalOnlyMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_LOCAL_ONLY, true)
    }

    fun setLocalOnlyMode(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_LOCAL_ONLY, enabled).apply()
    }

    private fun getVaultKey(context: Context): SecretKey {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var keyStr = prefs.getString(KEY_VAULT_KEY, null)
        if (keyStr == null) {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(AES_KEY_SIZE, SecureRandom())
            val secretKey = keyGen.generateKey()
            keyStr = Base64.encodeToString(secretKey.encoded, Base64.NO_WRAP)
            prefs.edit().putString(KEY_VAULT_KEY, keyStr).apply()
        }
        val decoded = Base64.decode(keyStr, Base64.NO_WRAP)
        return SecretKeySpec(decoded, "AES")
    }

    /**
     * Encrypts a Bitmap with AES-GCM 256-bit and writes to an encrypted file.
     */
    fun encryptBitmapToFile(context: Context, bitmap: Bitmap, targetFile: File): Boolean {
        return try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val rawBytes = stream.toByteArray()

            val secretKey = getVaultKey(context)
            val iv = ByteArray(GCM_IV_LENGTH)
            SecureRandom().nextBytes(iv)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

            val cipherText = cipher.doFinal(rawBytes)

            FileOutputStream(targetFile).use { fos ->
                fos.write(iv)
                fos.write(cipherText)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Decrypts an AES-GCM encrypted file back into a Bitmap in memory.
     */
    fun decryptBitmapFromFile(context: Context, encryptedFile: File): Bitmap? {
        return try {
            if (!encryptedFile.exists()) return null
            val fileBytes = encryptedFile.readBytes()
            if (fileBytes.size <= GCM_IV_LENGTH) return null

            val iv = fileBytes.copyOfRange(0, GCM_IV_LENGTH)
            val cipherText = fileBytes.copyOfRange(GCM_IV_LENGTH, fileBytes.size)

            val secretKey = getVaultKey(context)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(cipherText)
            BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Strips EXIF metadata from an image byte stream and saves a sanitized copy.
     */
    fun sanitizeAndSaveImage(bitmap: Bitmap, destinationFile: File): Boolean {
        return try {
            FileOutputStream(destinationFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun hashString(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }
}
