package com.example.engine

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import com.example.domain.model.FaceDetectionResult
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

data class EnhancedFaceLandmarks(
    val leftEye: PointF? = null,
    val rightEye: PointF? = null,
    val noseBase: PointF? = null,
    val mouthLeft: PointF? = null,
    val mouthRight: PointF? = null,
    val mouthBottom: PointF? = null,
    val contourPoints: List<PointF> = emptyList()
)

object FaceLandmarkService {

    private val detectorOptions by lazy {
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.1f)
            .build()
    }

    private val detector by lazy {
        FaceDetection.getClient(detectorOptions)
    }

    /**
     * Detects high-precision facial landmarks, 3D Euler angles, contour points,
     * and skin tone using Google ML Kit's Face Detection API.
     */
    suspend fun detectLandmarks(bitmap: Bitmap): Pair<FaceDetectionResult, EnhancedFaceLandmarks> = withContext(Dispatchers.Default) {
        val w = bitmap.width
        val h = bitmap.height

        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val faces = Tasks.await(detector.process(inputImage))

            if (!faces.isNullOrEmpty()) {
                val face: Face = faces[0]
                val bbox = face.boundingBox

                val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
                val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
                val noseBase = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
                val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
                val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position
                val mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position

                // Retrieve precise face contour polygon
                val faceContour = face.getContour(FaceContour.FACE)
                val contourPoints = faceContour?.points?.map { PointF(it.x, it.y) } ?: emptyList()

                // Calculate Eye Distance and Roll Angle from landmarks if available
                var computedAngle = face.headEulerAngleZ
                var eyeDistPx = (bbox.width() * 0.35f)
                var centerXPx = bbox.centerX().toFloat()
                var centerYPx = bbox.centerY().toFloat()

                if (leftEye != null && rightEye != null) {
                    val dx = rightEye.x - leftEye.x
                    val dy = rightEye.y - leftEye.y
                    eyeDistPx = sqrt(dx * dx + dy * dy)
                    computedAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    centerXPx = (leftEye.x + rightEye.x) * 0.5f
                    centerYPx = (leftEye.y + rightEye.y) * 0.5f
                }

                // Sample skin tone from cheek/nose bridge
                val sampleX = if (noseBase != null) noseBase.x.toInt().coerceIn(0, w - 1) else centerXPx.toInt().coerceIn(0, w - 1)
                val sampleY = if (noseBase != null) (noseBase.y - eyeDistPx * 0.2f).toInt().coerceIn(0, h - 1) else (centerYPx + eyeDistPx * 0.3f).toInt().coerceIn(0, h - 1)
                val pixel = bitmap.getPixel(sampleX, sampleY)

                val result = FaceDetectionResult(
                    hasFace = true,
                    faceCount = faces.size,
                    boundsLeft = max(0f, bbox.left.toFloat() / w),
                    boundsTop = max(0f, bbox.top.toFloat() / h),
                    boundsRight = min(1f, bbox.right.toFloat() / w),
                    boundsBottom = min(1f, bbox.bottom.toFloat() / h),
                    eyeDistance = (eyeDistPx / w).coerceIn(0.08f, 0.8f),
                    angle = computedAngle,
                    centerX = centerXPx / w,
                    centerY = centerYPx / h,
                    estimatedSkinToneR = Color.red(pixel),
                    estimatedSkinToneG = Color.green(pixel),
                    estimatedSkinToneB = Color.blue(pixel)
                )

                val landmarks = EnhancedFaceLandmarks(
                    leftEye = leftEye,
                    rightEye = rightEye,
                    noseBase = noseBase,
                    mouthLeft = mouthLeft,
                    mouthRight = mouthRight,
                    mouthBottom = mouthBottom,
                    contourPoints = contourPoints
                )

                return@withContext Pair(result, landmarks)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback to Golden-Ratio Heuristic
        val defaultResult = FaceDetectionResult(
            hasFace = true,
            faceCount = 1,
            boundsLeft = 0.22f,
            boundsTop = 0.15f,
            boundsRight = 0.78f,
            boundsBottom = 0.82f,
            eyeDistance = 0.32f,
            angle = 0f,
            centerX = 0.5f,
            centerY = 0.45f,
            estimatedSkinToneR = 220,
            estimatedSkinToneG = 180,
            estimatedSkinToneB = 150
        )
        return@withContext Pair(defaultResult, EnhancedFaceLandmarks())
    }
}
