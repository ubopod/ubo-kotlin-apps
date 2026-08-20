package com.ubopod.uboapp.wear.ui.common

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Wear-local counterpart to the phone app's `generateQrCodeBitmap` (ui/common/QrCode.kt). */
public fun generateQrCodeBitmap(payload: String, sizePx: Int = 384): ImageBitmap? {
    if (payload.isEmpty()) return null
    return runCatching {
        val matrix = MultiFormatWriter().encode(
            payload,
            BarcodeFormat.QR_CODE,
            sizePx,
            sizePx,
            mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                // ZXing's default 4-module quiet zone eats a large fraction
                // of a small watch-sized code, reading as a wide dead white
                // border. 1 module is still a real quiet zone, just sized
                // for this screen instead of a print-scale default.
                EncodeHintType.MARGIN to 1,
            ),
        )
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (y in 0 until sizePx) {
            for (x in 0 until sizePx) {
                bitmap.setPixel(x, y, if (matrix.get(x, y)) BLACK else WHITE)
            }
        }
        bitmap.asImageBitmap()
    }.getOrNull()
}

private const val BLACK: Int = 0xFF000000.toInt()
private const val WHITE: Int = 0xFFFFFFFF.toInt()
