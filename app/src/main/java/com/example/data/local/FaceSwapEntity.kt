package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "face_swap_projects")
data class FaceSwapEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetImagePath: String,
    val sourceImagePath: String,
    val resultImagePath: String,
    val templateId: String? = null,
    val filterName: String = "NONE",
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isEncryptedVault: Boolean = false,
    val swapMode: String = "LOCAL_AI_FAST",
    val scale: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = 0f,
    val skinWarmth: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val noWatermark: Boolean = true
)
