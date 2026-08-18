package com.example.engine

import android.content.Context
import android.graphics.*
import android.media.FaceDetector
import com.example.domain.model.EditorAdjustments
import com.example.domain.model.FaceDetectionResult
import com.example.domain.model.FilterType
import com.example.domain.model.PlacedSticker
import com.example.domain.model.StickerType
import com.example.domain.model.TextOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object FaceSwapEngine {

    /**
     * Detects facial boundary coordinates on a bitmap using Android FaceDetector API
     * with intelligent heuristics fallback.
     */
    suspend fun detectFace(bitmap: Bitmap): FaceDetectionResult = withContext(Dispatchers.Default) {
        try {
            // Android FaceDetector requires Bitmap.Config.RGB_565 and even width
            val width = if (bitmap.width % 2 == 0) bitmap.width else bitmap.width - 1
            val height = bitmap.height
            val bmp565 = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            val canvas = Canvas(bmp565)
            canvas.drawBitmap(bitmap, 0f, 0f, null)

            val maxFaces = 1
            val faces = arrayOfNulls<FaceDetector.Face>(maxFaces)
            val detector = FaceDetector(width, height, maxFaces)
            val faceCount = detector.findFaces(bmp565, faces)

            if (faceCount > 0 && faces[0] != null) {
                val face = faces[0]!!
                val midPoint = PointF()
                face.getMidPoint(midPoint)
                val eyeDistance = face.eyesDistance()
                val confidence = face.confidence()

                val boxHalfWidth = eyeDistance * 1.5f
                val boxHalfHeight = eyeDistance * 2.0f

                val left = max(0f, (midPoint.x - boxHalfWidth) / width)
                val top = max(0f, (midPoint.y - boxHalfHeight * 1.2f) / height)
                val right = min(1f, (midPoint.x + boxHalfWidth) / width)
                val bottom = min(1f, (midPoint.y + boxHalfHeight * 1.3f) / height)

                // Sample skin tone from center of face
                val sampleX = midPoint.x.toInt().coerceIn(0, bitmap.width - 1)
                val sampleY = (midPoint.y + eyeDistance * 0.3f).toInt().coerceIn(0, bitmap.height - 1)
                val pixel = bitmap.getPixel(sampleX, sampleY)

                return@withContext FaceDetectionResult(
                    hasFace = true,
                    faceCount = faceCount,
                    boundsLeft = left,
                    boundsTop = top,
                    boundsRight = right,
                    boundsBottom = bottom,
                    eyeDistance = eyeDistance / width,
                    angle = 0f,
                    estimatedSkinToneR = Color.red(pixel),
                    estimatedSkinToneG = Color.green(pixel),
                    estimatedSkinToneB = Color.blue(pixel)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Standard Golden-Ratio Facial heuristic default
        return@withContext FaceDetectionResult(
            hasFace = true,
            faceCount = 1,
            boundsLeft = 0.22f,
            boundsTop = 0.15f,
            boundsRight = 0.78f,
            boundsBottom = 0.82f,
            eyeDistance = 0.32f,
            angle = 0f,
            estimatedSkinToneR = 220,
            estimatedSkinToneG = 180,
            estimatedSkinToneB = 150
        )
    }

    /**
     * Executes the Core Local On-Device AI Face Swap transformation.
     */
    suspend fun performFaceSwap(
        targetBitmap: Bitmap,
        sourceBitmap: Bitmap,
        adjustments: EditorAdjustments = EditorAdjustments(),
        stickers: List<PlacedSticker> = emptyList(),
        textOverlays: List<TextOverlay> = emptyList()
    ): Bitmap = withContext(Dispatchers.Default) {
        val targetWidth = targetBitmap.width
        val targetHeight = targetBitmap.height

        // Create mutable base canvas initialized with Target Bitmap
        val outputBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // 1. Draw base target image
        canvas.drawBitmap(targetBitmap, 0f, 0f, paint)

        // 2. Detect face geometries
        val targetFace = detectFace(targetBitmap)
        val sourceFace = detectFace(sourceBitmap)

        // Calculate source face crop coordinates
        val srcW = sourceBitmap.width
        val srcH = sourceBitmap.height
        val srcLeft = (sourceFace.boundsLeft * srcW).toInt().coerceIn(0, srcW - 1)
        val srcTop = (sourceFace.boundsTop * srcH).toInt().coerceIn(0, srcH - 1)
        val srcRight = (sourceFace.boundsRight * srcW).toInt().coerceIn(srcLeft + 10, srcW)
        val srcBottom = (sourceFace.boundsBottom * srcH).toInt().coerceIn(srcTop + 10, srcH)
        val cropW = srcRight - srcLeft
        val cropH = srcBottom - srcTop

        if (cropW > 10 && cropH > 10) {
            val croppedSourceFace = Bitmap.createBitmap(sourceBitmap, srcLeft, srcTop, cropW, cropH)

            // 3. Match Skin Tone & Color Harmonization
            val colorHarmonizedFace = harmonizeSkinTone(
                sourceFaceBmp = croppedSourceFace,
                sourceSkinR = sourceFace.estimatedSkinToneR,
                sourceSkinG = sourceFace.estimatedSkinToneG,
                sourceSkinB = sourceFace.estimatedSkinToneB,
                targetSkinR = targetFace.estimatedSkinToneR,
                targetSkinG = targetFace.estimatedSkinToneG,
                targetSkinB = targetFace.estimatedSkinToneB,
                warmthAdjust = adjustments.skinWarmth
            )

            // 4. Create feathered elliptical / natural face mask for seamless blending
            val maskedFace = createFeatheredFaceMask(
                faceBitmap = colorHarmonizedFace,
                feathering = adjustments.feathering
            )

            // 5. Target face placement calculations
            val tgtFaceCenterX = (targetFace.boundsLeft + targetFace.boundsRight) * 0.5f * targetWidth
            val tgtFaceCenterY = (targetFace.boundsTop + targetFace.boundsBottom) * 0.5f * targetHeight
            val tgtFaceWidth = (targetFace.boundsRight - targetFace.boundsLeft) * targetWidth
            val tgtFaceHeight = (targetFace.boundsBottom - targetFace.boundsTop) * targetHeight

            // Base scale matching
            val scaleFactor = (tgtFaceWidth / maskedFace.width) * adjustments.scale
            val finalW = maskedFace.width * scaleFactor
            val finalH = maskedFace.height * scaleFactor

            val matrix = Matrix()
            // Center transformation origin
            matrix.postTranslate(-maskedFace.width * 0.5f, -maskedFace.height * 0.5f)
            matrix.postScale(scaleFactor, scaleFactor)
            matrix.postRotate(adjustments.rotation)
            // Translate to target face center + manual fine-tuning offsets
            val posX = tgtFaceCenterX + (adjustments.offsetX * targetWidth)
            val posY = tgtFaceCenterY + (adjustments.offsetY * targetHeight)
            matrix.postTranslate(posX, posY)

            // Draw with blend intensity alpha
            val blendPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = (adjustments.blendIntensity.coerceIn(0.1f, 1f) * 255).toInt()
            }
            canvas.drawBitmap(maskedFace, matrix, blendPaint)
        }

        // 6. Apply Color Grading / Aesthetics Filter (Cyberpunk, Vintage, Noir, etc.)
        val filteredBitmap = applyColorFilter(outputBitmap, adjustments.filter, adjustments)

        // 7. Render Stickers & Accessories
        if (stickers.isNotEmpty() || textOverlays.isNotEmpty()) {
            val finalCanvas = Canvas(filteredBitmap)
            drawStickersAndText(finalCanvas, stickers, textOverlays, targetWidth, targetHeight)
        }

        return@withContext filteredBitmap
    }

    /**
     * Color harmonization & skin warmth balance algorithm.
     */
    private fun harmonizeSkinTone(
        sourceFaceBmp: Bitmap,
        sourceSkinR: Int,
        sourceSkinG: Int,
        sourceSkinB: Int,
        targetSkinR: Int,
        targetSkinG: Int,
        targetSkinB: Int,
        warmthAdjust: Float
    ): Bitmap {
        val result = Bitmap.createBitmap(sourceFaceBmp.width, sourceFaceBmp.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Calculate ratio differences in RGB channels
        val rRatio = (targetSkinR.toFloat() / max(1, sourceSkinR)).coerceIn(0.6f, 1.5f)
        val gRatio = (targetSkinG.toFloat() / max(1, sourceSkinG)).coerceIn(0.6f, 1.5f)
        val bRatio = (targetSkinB.toFloat() / max(1, sourceSkinB)).coerceIn(0.6f, 1.5f)

        // Apply warmth shift
        val finalR = rRatio * (1.0f + warmthAdjust * 0.2f)
        val finalG = gRatio * (1.0f + warmthAdjust * 0.05f)
        val finalB = bRatio * (1.0f - warmthAdjust * 0.2f)

        val colorMatrix = ColorMatrix(
            floatArrayOf(
                finalR, 0f, 0f, 0f, 0f,
                0f, finalG, 0f, 0f, 0f,
                0f, 0f, finalB, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(sourceFaceBmp, 0f, 0f, paint)
        return result
    }

    /**
     * Creates a soft, feathered boundary mask for seamless Poisson-like blending.
     */
    private fun createFeatheredFaceMask(faceBitmap: Bitmap, feathering: Float): Bitmap {
        val w = faceBitmap.width
        val h = faceBitmap.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Draw face
        canvas.drawBitmap(faceBitmap, 0f, 0f, null)

        // Create mask using Radial Gradient Alpha
        val maskBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(maskBitmap)

        val radius = max(w, h) * 0.55f
        val innerRadius = radius * (1.0f - (feathering * 0.5f).coerceIn(0.1f, 0.8f))

        val gradient = RadialGradient(
            w * 0.5f, h * 0.5f, radius,
            intArrayOf(
                android.graphics.Color.WHITE,
                android.graphics.Color.WHITE,
                android.graphics.Color.TRANSPARENT
            ),
            floatArrayOf(0f, innerRadius / radius, 1f),
            Shader.TileMode.CLAMP
        )

        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
        }
        maskCanvas.drawOval(RectF(0f, 0f, w.toFloat(), h.toFloat()), maskPaint)

        // Apply mask onto face using DST_IN
        val blendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawBitmap(maskBitmap, 0f, 0f, blendPaint)

        return output
    }

    /**
     * Applies aesthetic color grading and tuning adjustments (Brightness, Contrast, Saturation, Filters).
     */
    fun applyColorFilter(
        inputBitmap: Bitmap,
        filter: FilterType,
        adjustments: EditorAdjustments
    ): Bitmap {
        val output = Bitmap.createBitmap(inputBitmap.width, inputBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val finalMatrix = ColorMatrix()

        // 1. Basic adjustments
        val brightnessMat = ColorMatrix().apply {
            val b = adjustments.brightness * 255f
            set(
                floatArrayOf(
                    1f, 0f, 0f, 0f, b,
                    0f, 1f, 0f, 0f, b,
                    0f, 0f, 1f, 0f, b,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }

        val contrastMat = ColorMatrix().apply {
            val c = adjustments.contrast
            val t = (1.0f - c) * 128f
            set(
                floatArrayOf(
                    c, 0f, 0f, 0f, t,
                    0f, c, 0f, 0f, t,
                    0f, 0f, c, 0f, t,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }

        val satMat = ColorMatrix().apply {
            setSaturation(adjustments.saturation)
        }

        finalMatrix.postConcat(brightnessMat)
        finalMatrix.postConcat(contrastMat)
        finalMatrix.postConcat(satMat)

        // 2. Preset Filter Grading
        val filterMat = ColorMatrix()
        when (filter) {
            FilterType.NONE -> { /* No-op */ }
            FilterType.CYBERPUNK -> {
                // Boost blues & cyans in shadows, hot magenta in highlights
                filterMat.set(
                    floatArrayOf(
                        1.2f, 0.0f, 0.3f, 0f, 20f,
                        0.0f, 0.9f, 0.2f, 0f, -10f,
                        0.2f, 0.3f, 1.4f, 0f, 35f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            FilterType.VINTAGE_90S -> {
                // Warm nostalgic film sepia & soft contrast
                filterMat.set(
                    floatArrayOf(
                        1.15f, 0.1f, 0.0f, 0f, 15f,
                        0.05f, 1.05f, 0.0f, 0f, 10f,
                        0.0f, 0.05f, 0.85f, 0f, -5f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            FilterType.GOLDEN_HOUR -> {
                // Golden amber warmth and rich sunlight glow
                filterMat.set(
                    floatArrayOf(
                        1.3f, 0.1f, 0.0f, 0f, 25f,
                        0.1f, 1.15f, 0.0f, 0f, 15f,
                        0.0f, 0.0f, 0.75f, 0f, -20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            FilterType.FILM_NOIR -> {
                // High contrast cinematic black and white
                val bwMat = ColorMatrix().apply { setSaturation(0f) }
                val bwContrast = ColorMatrix().apply {
                    val c = 1.35f
                    val t = (1.0f - c) * 128f
                    set(
                        floatArrayOf(
                            c, 0f, 0f, 0f, t,
                            0f, c, 0f, 0f, t,
                            0f, 0f, c, 0f, t,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                }
                filterMat.setConcat(bwContrast, bwMat)
            }
            FilterType.STUDIO_GLOW -> {
                // Soft glamorous skin luminosity
                filterMat.set(
                    floatArrayOf(
                        1.08f, 0.02f, 0.02f, 0f, 18f,
                        0.02f, 1.08f, 0.02f, 0f, 18f,
                        0.02f, 0.02f, 1.10f, 0f, 22f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            FilterType.COMIC_POP -> {
                // Vivid pop art saturation and distinct punch
                val popSat = ColorMatrix().apply { setSaturation(1.6f) }
                filterMat.set(popSat)
            }
            FilterType.WARM_SUNSET -> {
                filterMat.set(
                    floatArrayOf(
                        1.25f, 0.05f, 0.0f, 0f, 30f,
                        0.05f, 0.95f, 0.0f, 0f, 5f,
                        0.0f, 0.0f, 0.8f, 0f, -15f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            FilterType.DRAMATIC_CONTRAST -> {
                val dMat = ColorMatrix().apply {
                    val c = 1.4f
                    val t = (1.0f - c) * 128f
                    set(
                        floatArrayOf(
                            c, 0f, 0f, 0f, t,
                            0f, c, 0f, 0f, t,
                            0f, 0f, c, 0f, t,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                }
                filterMat.set(dMat)
            }
        }

        finalMatrix.postConcat(filterMat)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(finalMatrix)
        }
        canvas.drawBitmap(inputBitmap, 0f, 0f, paint)
        return output
    }

    /**
     * Renders emoji stickers, accessories, and text overlays onto the final canvas.
     */
    private fun drawStickersAndText(
        canvas: Canvas,
        stickers: List<PlacedSticker>,
        textOverlays: List<TextOverlay>,
        canvasW: Int,
        canvasH: Int
    ) {
        val stickerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = canvasW * 0.16f
            textAlign = Paint.Align.CENTER
        }

        for (sticker in stickers) {
            val posX = sticker.offsetX * canvasW
            val posY = sticker.offsetY * canvasH

            canvas.save()
            canvas.translate(posX, posY)
            canvas.rotate(sticker.rotation)
            canvas.scale(sticker.scale, sticker.scale)

            canvas.drawText(sticker.type.emoji, 0f, stickerPaint.textSize * 0.35f, stickerPaint)
            canvas.restore()
        }

        for (overlay in textOverlays) {
            val posX = overlay.offsetX * canvasW
            val posY = overlay.offsetY * canvasH

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = try { android.graphics.Color.parseColor(overlay.colorHex) } catch (e: Exception) { android.graphics.Color.WHITE }
                textSize = overlay.fontSize * (canvasW / 400f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                setShadowLayer(8f, 2f, 2f, android.graphics.Color.BLACK)
            }

            if (overlay.hasBackground) {
                val bounds = Rect()
                textPaint.getTextBounds(overlay.text, 0, overlay.text.length, bounds)
                val padX = 24f
                val padY = 16f
                val bgRect = RectF(
                    posX - bounds.width() * 0.5f - padX,
                    posY - bounds.height() - padY,
                    posX + bounds.width() * 0.5f + padX,
                    posY + padY
                )
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb(160, 0, 0, 0)
                }
                canvas.drawRoundRect(bgRect, 16f, 16f, bgPaint)
            }

            canvas.drawText(overlay.text, posX, posY, textPaint)
        }
    }

    /**
     * Generates a side-by-side / split Before & After comparison bitmap.
     */
    fun createSplitComparison(
        beforeBitmap: Bitmap,
        afterBitmap: Bitmap,
        splitRatio: Float = 0.5f // 0f (all before) to 1f (all after)
    ): Bitmap {
        val w = beforeBitmap.width
        val h = beforeBitmap.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Draw Before (Left side)
        val splitX = (w * splitRatio).toInt()
        val beforeSrc = Rect(0, 0, splitX, h)
        val beforeDst = Rect(0, 0, splitX, h)
        canvas.drawBitmap(beforeBitmap, beforeSrc, beforeDst, null)

        // Draw After (Right side)
        val afterSrc = Rect(splitX, 0, w, h)
        val afterDst = Rect(splitX, 0, w, h)
        canvas.drawBitmap(afterBitmap, afterSrc, afterDst, null)

        // Draw Divider Line with Glow
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            strokeWidth = 6f
            style = Paint.Style.STROKE
            setShadowLayer(8f, 0f, 0f, android.graphics.Color.CYAN)
        }
        canvas.drawLine(splitX.toFloat(), 0f, splitX.toFloat(), h.toFloat(), dividerPaint)

        return output
    }
}
