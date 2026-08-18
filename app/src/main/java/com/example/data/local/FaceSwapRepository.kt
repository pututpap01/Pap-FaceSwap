package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.engine.PrivacySecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class FaceSwapRepository(
    private val context: Context,
    private val dao: FaceSwapDao
) {
    val publicProjects: Flow<List<FaceSwapEntity>> = dao.getAllPublicProjects()
    val vaultProjects: Flow<List<FaceSwapEntity>> = dao.getAllVaultProjects()
    val favoriteProjects: Flow<List<FaceSwapEntity>> = dao.getFavoriteProjects()

    suspend fun saveProject(
        title: String,
        targetBmp: Bitmap,
        sourceBmp: Bitmap,
        resultBmp: Bitmap,
        templateId: String? = null,
        isVault: Boolean = false,
        filterName: String = "NONE",
        scale: Float = 1.0f,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        rotation: Float = 0f,
        skinWarmth: Float = 0f,
        brightness: Float = 0f,
        contrast: Float = 1.0f,
        saturation: Float = 1.0f
    ): Long = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val projectsDir = File(context.filesDir, if (isVault) "vault_projects" else "projects")
        if (!projectsDir.exists()) projectsDir.mkdirs()

        val targetFile = File(projectsDir, "target_${timestamp}.jpg")
        val sourceFile = File(projectsDir, "source_${timestamp}.jpg")
        val resultFile = File(projectsDir, "result_${timestamp}.${if (isVault) "enc" else "jpg"}")

        // Save Target & Source
        saveBitmapToJpg(targetBmp, targetFile)
        saveBitmapToJpg(sourceBmp, sourceFile)

        if (isVault) {
            PrivacySecurityManager.encryptBitmapToFile(context, resultBmp, resultFile)
        } else {
            if (PrivacySecurityManager.isExifScrubbingEnabled(context)) {
                PrivacySecurityManager.sanitizeAndSaveImage(resultBmp, resultFile)
            } else {
                saveBitmapToJpg(resultBmp, resultFile)
            }
        }

        val entity = FaceSwapEntity(
            title = title.ifBlank { "Swap_${timestamp % 10000}" },
            targetImagePath = targetFile.absolutePath,
            sourceImagePath = sourceFile.absolutePath,
            resultImagePath = resultFile.absolutePath,
            templateId = templateId,
            filterName = filterName,
            timestamp = timestamp,
            isEncryptedVault = isVault,
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            rotation = rotation,
            skinWarmth = skinWarmth,
            brightness = brightness,
            contrast = contrast,
            saturation = saturation
        )

        dao.insertProject(entity)
    }

    suspend fun loadBitmapFromPath(path: String, isVault: Boolean = false): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) return@withContext null
            if (isVault) {
                PrivacySecurityManager.decryptBitmapFromFile(context, file)
            } else {
                BitmapFactory.decodeFile(file.absolutePath)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun loadBitmapFromUri(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun loadBitmapFromResource(drawableRes: Int): Bitmap? = withContext(Dispatchers.IO) {
        try {
            BitmapFactory.decodeResource(context.resources, drawableRes)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun toggleFavorite(id: Long, current: Boolean) = withContext(Dispatchers.IO) {
        dao.updateFavorite(id, !current)
    }

    suspend fun moveToVault(id: Long, moveToVault: Boolean) = withContext(Dispatchers.IO) {
        val entity = dao.getProjectById(id) ?: return@withContext
        val oldFile = File(entity.resultImagePath)
        if (oldFile.exists()) {
            val bmp = if (entity.isEncryptedVault) {
                PrivacySecurityManager.decryptBitmapFromFile(context, oldFile)
            } else {
                BitmapFactory.decodeFile(oldFile.absolutePath)
            }

            if (bmp != null) {
                val newDir = File(context.filesDir, if (moveToVault) "vault_projects" else "projects")
                if (!newDir.exists()) newDir.mkdirs()
                val newFile = File(newDir, "result_${System.currentTimeMillis()}.${if (moveToVault) "enc" else "jpg"}")

                if (moveToVault) {
                    PrivacySecurityManager.encryptBitmapToFile(context, bmp, newFile)
                } else {
                    saveBitmapToJpg(bmp, newFile)
                }
                oldFile.delete()

                val updatedEntity = entity.copy(
                    resultImagePath = newFile.absolutePath,
                    isEncryptedVault = moveToVault
                )
                dao.updateProject(updatedEntity)
                return@withContext
            }
        }
        dao.updateVaultStatus(id, moveToVault)
    }

    suspend fun deleteProject(id: Long) = withContext(Dispatchers.IO) {
        val entity = dao.getProjectById(id)
        if (entity != null) {
            File(entity.targetImagePath).delete()
            File(entity.sourceImagePath).delete()
            File(entity.resultImagePath).delete()
            dao.deleteProjectById(id)
        }
    }

    suspend fun clearVault() = withContext(Dispatchers.IO) {
        val vaultDir = File(context.filesDir, "vault_projects")
        if (vaultDir.exists()) {
            vaultDir.deleteRecursively()
        }
        dao.clearVault()
    }

    private fun saveBitmapToJpg(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
    }
}
