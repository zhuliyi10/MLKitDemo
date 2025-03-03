package com.leon.mlkitdemo

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await

class FaceDetector {
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    private val detector = FaceDetection.getClient(options)

    suspend fun detectFaces(bitmap: Bitmap): String {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val faces = detector.process(image).await()
            buildString {
                appendLine("检测到 ${faces.size} 张人脸")
                faces.forEachIndexed { index, face ->
                    appendLine("人脸 ${index + 1}:")
                    appendLine("- 微笑概率: ${face.smilingProbability?.times(100)}%")
                    appendLine("- 左眼睁开概率: ${face.leftEyeOpenProbability?.times(100)}%")
                    appendLine("- 右眼睁开概率: ${face.rightEyeOpenProbability?.times(100)}%")
                    appendLine("- 头部旋转角度:")
                    appendLine("  左右: ${face.headEulerAngleY}°")
                    appendLine("  上下: ${face.headEulerAngleX}°")
                    appendLine("  倾斜: ${face.headEulerAngleZ}°")
                    appendLine()
                }
            }.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.localizedMessage}"
        }
    }
}
