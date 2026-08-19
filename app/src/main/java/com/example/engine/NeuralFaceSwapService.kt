package com.example.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

object NeuralFaceSwapService {

    private const val TAG = "NeuralFaceSwapService"
    private const val MODEL_NAME = "gemini-2.5-flash-image"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Checks if a valid Gemini API key is configured.
     */
    fun isApiKeyConfigured(): Boolean {
        val key = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY" && key != "YOUR_API_KEY"
    }

    /**
     * Performs a true neural deep-learning generative face swap using Gemini 2.5 Flash Image.
     * Takes facial structure, skin texture, features, and expression from the source face
     * and photorealistically grafts it onto the target image.
     */
    suspend fun performNeuralFaceSwap(
        targetBitmap: Bitmap,
        sourceBitmap: Bitmap
    ): Bitmap? = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not configured or placeholder.")
            return@withContext null
        }

        try {
            // Resize for optimal latency and bandwidth (max 1024px)
            val scaledTarget = scaleBitmapDown(targetBitmap, 1024)
            val scaledSource = scaleBitmapDown(sourceBitmap, 1024)

            val targetBase64 = bitmapToBase64(scaledTarget)
            val sourceBase64 = bitmapToBase64(scaledSource)

            // Construct JSON request body for Gemini 2.5 Flash Image
            val jsonRoot = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            // 1. Text Prompt Instruction
            val promptPart = JSONObject().apply {
                put(
                    "text",
                    "Perform a high-fidelity, photorealistic neural face swap. " +
                    "Identity & Facial Source: The second image provided is the SOURCE person. Extract their facial identity, facial bone structure, eyes, nose, mouth shape, and skin tone. " +
                    "Target Scene: The first image provided is the TARGET scene/person. Seamlessly replace the face of the person in the target image with the source face. " +
                    "Requirements: Harmonize skin texture, 3D head pose angle, ambient lighting, shadows, hair edges, and facial expression naturally with the target photo. Do not produce cutout lines or pasted artifacts. Output the complete, full-resolution photorealistic swapped result image."
                )
            }
            partsArray.put(promptPart)

            // 2. Target Image
            val targetPart = JSONObject().apply {
                val inlineData = JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", targetBase64)
                }
                put("inlineData", inlineData)
            }
            partsArray.put(targetPart)

            // 3. Source Face Image
            val sourcePart = JSONObject().apply {
                val inlineData = JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", sourceBase64)
                }
                put("inlineData", inlineData)
            }
            partsArray.put(sourcePart)

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            jsonRoot.put("contents", contentsArray)

            // Generation config with IMAGE modality
            val genConfig = JSONObject().apply {
                val modalities = JSONArray().apply {
                    put("IMAGE")
                    put("TEXT")
                }
                put("responseModalities", modalities)
                val imageConfig = JSONObject().apply {
                    put("aspectRatio", "1:1")
                }
                put("imageConfig", imageConfig)
            }
            jsonRoot.put("generationConfig", genConfig)

            val requestBody = jsonRoot.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Neural API error ${response.code}: $responseString")
                return@withContext null
            }

            // Parse response to find generated image inlineData
            val responseJson = JSONObject(responseString)
            val candidates = responseJson.optJSONArray("candidates") ?: return@withContext null
            if (candidates.length() == 0) return@withContext null

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content") ?: return@withContext null
            val parts = content.optJSONArray("parts") ?: return@withContext null

            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                val inlineData = part.optJSONObject("inlineData")
                if (inlineData != null) {
                    val base64Data = inlineData.optString("data")
                    if (base64Data.isNotBlank()) {
                        val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                        val decodedBmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        if (decodedBmp != null) {
                            Log.i(TAG, "Successfully received neural AI face swap bitmap: ${decodedBmp.width}x${decodedBmp.height}")
                            return@withContext decodedBmp
                        }
                    }
                }
            }

            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform neural face swap: ${e.message}", e)
            return@withContext null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val bytes = stream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDimension && h <= maxDimension) return bitmap

        val ratio = min(maxDimension.toFloat() / w, maxDimension.toFloat() / h)
        val targetW = (w * ratio).toInt()
        val targetH = (h * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }
}
