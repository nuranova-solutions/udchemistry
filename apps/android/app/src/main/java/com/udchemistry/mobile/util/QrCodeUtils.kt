package com.udchemistry.mobile.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayOutputStream

object QrCodeUtils {
    fun createPngDataUrl(content: String, size: Int = 480): String {
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888)

        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap[x, y] = if (bitMatrix[x, y]) 0xFF0C162A.toInt() else 0xFFFFFFFF.toInt()
            }
        }

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        return "data:image/png;base64,$base64"
    }

    fun decodeDataUrl(dataUrl: String?): ImageBitmap? {
        if (dataUrl.isNullOrBlank()) {
            return null
        }

        val bytes = decodeDataUrlBytes(dataUrl) ?: return null
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        return bitmap.asImageBitmap()
    }

    fun decodeDataUrlBytes(dataUrl: String?): ByteArray? {
        if (dataUrl.isNullOrBlank()) {
            return null
        }

        val payload = dataUrl.substringAfter("base64,", missingDelimiterValue = dataUrl)
        return Base64.decode(payload, Base64.DEFAULT)
    }
}
