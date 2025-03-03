package com.leon.mlkitdemo

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.tasks.await

class TextRecognizer {
    // 使用中文识别器
    private val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    suspend fun recognizeText(bitmap: Bitmap): String {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            result.text
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.localizedMessage}"
        }
    }
}
