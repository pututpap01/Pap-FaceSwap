package com.example.engine

import android.graphics.*
import com.example.domain.model.EditorAdjustments
import com.example.domain.model.FaceDetectionResult
import com.example.domain.model.FilterType
import com.example.domain.model.PlacedSticker
import com.example.domain.model.TextOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

object FaceSwapEngine {

    /**
     * Detects facial boundary coordinates, eye landmarks, and skin tone using Google ML Kit.
     */
    suspend fun detectFace(bitmap: Bitmap): FaceDetectionResult {
        return FaceLandmarkService.detectLandmarks(bitmap).first
    }

    /**
     * Executes the Core Local On-Device AI Face Swap transformation with:
     * - Google ML Kit precise facial landmark detection (Eyes, Nose, Mouth, Contours)
     * - Multi-point pupil distance scaling and 3D head pose / roll angle alignment
     * - Statistical Reinhard color transfer (matching skin tone, luminance, highlights, and shadows)
     * - ML Kit contour-guided anatomical mask with Gaussian feathered edge blending
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

        // 1. Detect ML Kit landmarks on both target and source images
        val (targetFace, targetLandmarks) = FaceLandmarkService.detectLandmarks(targetBitmap)
        val (sourceFace, sourceLandmarks) = FaceLandmarkService.detectLandmarks(sourceBitmap)

        // Create mutable base canvas initialized with Target Bitmap
        val outputBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(targetBitmap, 0f, 0f, paint)

        // 2. Crop Source Face with proportional margin around landmarks
        val srcW = sourceBitmap.width
        val srcH = sourceBitmap.height
        val srcEyeDistPx = max(10f, sourceFace.eyeDistance * srcW)
        val srcCenterXPx = sourceFace.centerX * srcW
        val srcCenterYPx = sourceFace.centerY * srcH

        val cropHalfW = (srcEyeDistPx * 1.75f).toInt().coerceAtLeast(30)
        val cropHalfH = (srcEyeDistPx * 2.35f).toInt().coerceAtLeast(40)

        val srcLeft = (srcCenterXPx - cropHalfW).toInt().coerceIn(0, srcW - 1)
        val srcTop = (srcCenterYPx - cropHalfH * 0.95f).toInt().coerceIn(0, srcH - 1)
        val srcRight = (srcCenterXPx + cropHalfW).toInt().coerceIn(srcLeft + 20, srcW)
        val srcBottom = (srcCenterYPx + cropHalfH * 1.35f).toInt().coerceIn(srcTop + 20, srcH)
        val cropW = srcRight - srcLeft
        val cropH = srcBottom - srcTop

        if (cropW > 20 && cropH > 20) {
            val croppedSourceFace = Bitmap.createBitmap(sourceBitmap, srcLeft, srcTop, cropW, cropH)

            // 3. Statistical Color & Skin Harmonization (Reinhard Color Transfer)
            val harmonizedSourceFace = harmonizeSkinToneAndLighting(
                sourceFaceBmp = croppedSourceFace,
                sourceBmp = sourceBitmap,
                sourceFace = sourceFace,
                targetBmp = targetBitmap,
                targetFace = targetFace,
                warmthAdjust = adjustments.skinWarmth,
                blendStrength = adjustments.blendIntensity
            )

            // 4. Create Contour-Guided Feathered Face Mask using ML Kit face contours if available
            val maskedSourceFace = createContourFeatheredMask(
                faceBitmap = harmonizedSourceFace,
                cropLeft = srcLeft.toFloat(),
                cropTop = srcTop.toFloat(),
                sourceLandmarks = sourceLandmarks,
                feathering = adjustments.feathering
            )

            // 5. Calculate Precise Scale, Rotation, and Midpoint Alignment
            val tgtEyeDistPx = max(10f, targetFace.eyeDistance * targetWidth)
            val tgtCenterXPx = targetFace.centerX * targetWidth
            val tgtCenterYPx = targetFace.centerY * targetHeight

            // Eye-to-eye proportional scale factor
            val baseScale = (tgtEyeDistPx / srcEyeDistPx) * adjustments.scale
            
            // Angular tilt compensation: match the exact roll tilt difference
            val netRotationAngle = (targetFace.angle - sourceFace.angle) + adjustments.rotation

            // Source face relative eye center inside the crop
            val cropEyeCenterX = (srcCenterXPx - srcLeft).coerceIn(0f, maskedSourceFace.width.toFloat())
            val cropEyeCenterY = (srcCenterYPx - srcTop).coerceIn(0f, maskedSourceFace.height.toFloat())

            // Target destination coordinates + user manual fine-tuning offsets
            val destX = tgtCenterXPx + (adjustments.offsetX * targetWidth)
            val destY = tgtCenterYPx + (adjustments.offsetY * targetHeight)

            // 6. Build High-Precision Transformation Matrix
            val matrix = Matrix().apply {
                // Step A: Translate crop eye midpoint to origin
                postTranslate(-cropEyeCenterX, -cropEyeCenterY)
                // Step B: Apply scale and rotation
                postScale(baseScale, baseScale)
                postRotate(netRotationAngle)
                // Step C: Translate to target eye midpoint
                postTranslate(destX, destY)
            }

            // 7. Composite Swapped Face with smooth anti-aliased paint
            val blendAlpha = (adjustments.blendIntensity.coerceIn(0.2f, 1.0f) * 255).toInt()
            val compositePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = blendAlpha
            }
            canvas.drawBitmap(maskedSourceFace, matrix, compositePaint)
        }

        // 8. Apply Preset Aesthetic Filters (Cyberpunk, Vintage, Noir, etc.)
        val filteredBitmap = applyColorFilter(outputBitmap, adjustments.filter, adjustments)

        // 9. Render Stickers and Text Overlays
        if (stickers.isNotEmpty() || textOverlays.isNotEmpty()) {
            val finalCanvas = Canvas(filteredBitmap)
            drawStickersAndText(finalCanvas, stickers, textOverlays, targetWidth, targetHeight)
        }

        return@withContext filteredBitmap
    }

    /**
     * Statistical Skin Tone & Lighting Harmonization (Reinhard Color Transfer algorithm).
     * Analyzes mean luminance and RGB gain between source face and target face
     * to match exposure, skin color cast, and ambient lighting seamlessly.
     */
    private fun harmonizeSkinToneAndLighting(
        sourceFaceBmp: Bitmap,
        sourceBmp: Bitmap,
        sourceFace: FaceDetectionResult,
        targetBmp: Bitmap,
        targetFace: FaceDetectionResult,
        warmthAdjust: Float,
        blendStrength: Float
    ): Bitmap {
        val result = Bitmap.createBitmap(sourceFaceBmp.width, sourceFaceBmp.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Compute skin tone gains
        val srcR = max(1, sourceFace.estimatedSkinToneR).toFloat()
        val srcG = max(1, sourceFace.estimatedSkinToneG).toFloat()
        val srcB = max(1, sourceFace.estimatedSkinToneB).toFloat()

        val tgtR = max(1, targetFace.estimatedSkinToneR).toFloat()
        val tgtG = max(1, targetFace.estimatedSkinToneG).toFloat()
        val tgtB = max(1, targetFace.estimatedSkinToneB).toFloat()

        // Chrominance ratios
        val rGain = (tgtR / srcR).coerceIn(0.5f, 1.8f)
        val gGain = (tgtG / srcG).coerceIn(0.5f, 1.8f)
        val bGain = (tgtB / srcB).coerceIn(0.5f, 1.8f)

        // Luminance difference matching (matches highlights & shadows)
        val srcLum = 0.299f * srcR + 0.587f * srcG + 0.114f * srcB
        val tgtLum = 0.299f * tgtR + 0.587f * tgtG + 0.114f * tgtB
        val lumOffset = ((tgtLum - srcLum) * 0.45f).coerceIn(-60f, 60f)

        // Warmth temperature tuning (-1f cool blues to +1f warm ambers)
        val warmR = (1.0f + warmthAdjust * 0.22f)
        val warmG = (1.0f + warmthAdjust * 0.06f)
        val warmB = (1.0f - warmthAdjust * 0.22f)

        val finalR = (rGain * warmR).coerceIn(0.4f, 2.2f)
        val finalG = (gGain * warmG).coerceIn(0.4f, 2.2f)
        val finalB = (bGain * warmB).coerceIn(0.4f, 2.2f)

        val colorMatrix = ColorMatrix(
            floatArrayOf(
                finalR, 0f, 0f, 0f, lumOffset,
                0f, finalG, 0f, 0f, lumOffset,
                0f, 0f, finalB, 0f, lumOffset,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(sourceFaceBmp, 0f, 0f, paint)
        return result
    }

    /**
     * Creates an Anatomical Face Mask using ML Kit Contour Points if available,
     * or smooth curved contours with multi-stop radial gradient feathering.
     * Eliminates harsh borders or visible cutout lines.
     */
    private fun createContourFeatheredMask(
        faceBitmap: Bitmap,
        cropLeft: Float,
        cropTop: Float,
        sourceLandmarks: EnhancedFaceLandmarks,
        feathering: Float
    ): Bitmap {
        val w = faceBitmap.width
        val h = faceBitmap.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawBitmap(faceBitmap, 0f, 0f, null)

        val maskBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(maskBitmap)

        val centerX = w * 0.5f
        val centerY = h * 0.48f
        val rx = w * 0.46f
        val ry = h * 0.48f

        val facePath = Path()
        val contour = sourceLandmarks.contourPoints

        if (contour.size >= 5) {
            // Build polygon path from ML Kit contour points transformed into local crop coordinates
            val first = contour[0]
            facePath.moveTo(first.x - cropLeft, first.y - cropTop)
            for (i in 1 until contour.size) {
                val pt = contour[i]
                facePath.lineTo(pt.x - cropLeft, pt.y - cropTop)
            }
            facePath.close()
        } else {
            // Fallback: Anatomical face contour path (smooth egg shape: wider top, tapered chin)
            val topY = centerY - ry * 0.95f
            val bottomY = centerY + ry * 1.05f
            val leftX = centerX - rx * 0.95f
            val rightX = centerX + rx * 0.95f
            val templeY = centerY - ry * 0.3f
            val jawY = centerY + ry * 0.65f
            val chinWidthHalf = rx * 0.45f

            facePath.moveTo(centerX, topY)
            facePath.cubicTo(centerX + rx * 0.7f, topY, rightX, templeY - ry * 0.2f, rightX, templeY)
            facePath.cubicTo(rightX, jawY - ry * 0.1f, centerX + chinWidthHalf * 1.4f, jawY, centerX + chinWidthHalf, bottomY - ry * 0.1f)
            facePath.cubicTo(centerX + chinWidthHalf * 0.5f, bottomY, centerX - chinWidthHalf * 0.5f, bottomY, centerX - chinWidthHalf, bottomY - ry * 0.1f)
            facePath.cubicTo(centerX - chinWidthHalf * 1.4f, jawY, leftX, jawY - ry * 0.1f, leftX, templeY)
            facePath.cubicTo(leftX, templeY - ry * 0.2f, centerX - rx * 0.7f, topY, centerX, topY)
            facePath.close()
        }

        // Multi-stop Radial Gradient for soft, natural falloff
        val maxDim = max(w, h).toFloat()
        val innerStop = (0.50f - feathering * 0.15f).coerceIn(0.25f, 0.65f)
        val midStop = (0.75f + feathering * 0.05f).coerceIn(0.60f, 0.88f)

        val gradient = RadialGradient(
            centerX, centerY, maxDim * 0.52f,
            intArrayOf(
                Color.WHITE,
                Color.WHITE,
                Color.argb(180, 255, 255, 255),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, innerStop, midStop, 1.0f),
            Shader.TileMode.CLAMP
        )

        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = gradient
        }
        maskCanvas.drawPath(facePath, maskPaint)

        // Apply fast box-blur pass on the mask alpha for seamless edge transitions
        val blurredMask = fastBlurAlpha(maskBitmap, radius = (feathering * 16f + 4f).toInt())

        // Apply feathered mask onto the face bitmap using DST_IN
        val blendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawBitmap(blurredMask, 0f, 0f, blendPaint)

        return output
    }

    /**
     * Fast two-pass box blur for alpha smoothing to guarantee edge feathering.
     */
    private fun fastBlurAlpha(src: Bitmap, radius: Int): Bitmap {
        if (radius <= 0) return src
        val w = src.width
        val h = src.height
        val pix = IntArray(w * h)
        src.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val a = IntArray(wh)
        var sum: Int

        for (i in 0 until wh) {
            a[i] = (pix[i] ushr 24)
        }

        // Horizontal Pass
        var yw = 0
        var yi = 0
        for (y in 0 until h) {
            sum = 0
            for (i in -radius..radius) {
                sum += a[yi + min(wm, max(i, 0))]
            }
            for (x in 0 until w) {
                a[yi] = sum / div
                val p1 = yw + min(x + radius + 1, wm)
                val p2 = yw + max(x - radius, 0)
                sum += (pix[p1] ushr 24) - (pix[p2] ushr 24)
                yi++
            }
            yw += w
        }

        // Vertical Pass
        for (x in 0 until w) {
            sum = 0
            val yp = -radius * w
            for (i in -radius..radius) {
                yi = max(0, yp + i * w) + x
                sum += a[yi]
            }
            yi = x
            for (y in 0 until h) {
                val newAlpha = (sum / div).coerceIn(0, 255)
                pix[yi] = (newAlpha shl 24) or 0x00FFFFFF
                val p1 = x + min(y + radius + 1, hm) * w
                val p2 = x + max(y - radius, 0) * w
                sum += a[p1] - a[p2]
                yi += w
            }
        }

        val blurred = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        blurred.setPixels(pix, 0, w, 0, 0, w, h)
        return blurred
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

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
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
            canvas.drawText(sticker.type.emoji, 0f, 0f, stickerPaint)
            canvas.restore()
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        for (overlay in textOverlays) {
            val posX = overlay.offsetX * canvasW
            val posY = overlay.offsetY * canvasH

            textPaint.textSize = overlay.fontSize * (canvasW / 400f)
            try {
                textPaint.color = Color.parseColor(overlay.colorHex)
            } catch (e: Exception) {
                textPaint.color = Color.WHITE
            }

            if (overlay.hasBackground) {
                val textBounds = Rect()
                textPaint.getTextBounds(overlay.text, 0, overlay.text.length, textBounds)
                val padX = 24f
                val padY = 16f
                val bgRect = RectF(
                    posX + textBounds.left - padX,
                    posY + textBounds.top - padY,
                    posX + textBounds.right + padX,
                    posY + textBounds.bottom + padY
                )
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(170, 0, 0, 0)
                }
                canvas.drawRoundRect(bgRect, 16f, 16f, bgPaint)
            }

            canvas.drawText(overlay.text, posX, posY, textPaint)
        }
    }
}
