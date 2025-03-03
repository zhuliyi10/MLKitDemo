package com.leon.mlkitdemo

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.tasks.await

class QRCodeScanner {
    private val scanner = BarcodeScanning.getClient()

    suspend fun scanQRCode(bitmap: Bitmap): String {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val barcodes = scanner.process(image).await()
            
            if (barcodes.isEmpty()) {
                "未检测到二维码"
            } else {
                buildString {
                    barcodes.forEach { barcode ->
                        when (barcode.valueType) {
                            Barcode.TYPE_URL -> appendLine("网址: ${barcode.url?.url}")
                            Barcode.TYPE_TEXT -> appendLine("文本: ${barcode.rawValue}")
                            Barcode.TYPE_EMAIL -> appendLine("邮箱: ${barcode.email?.address}")
                            Barcode.TYPE_PHONE -> appendLine("电话: ${barcode.phone?.number}")
                            else -> appendLine("内容: ${barcode.rawValue}")
                        }
                    }
                }.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.localizedMessage}"
        }
    }
}
