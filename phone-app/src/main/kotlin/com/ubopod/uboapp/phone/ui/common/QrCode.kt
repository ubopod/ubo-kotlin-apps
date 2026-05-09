package com.ubopod.uboapp.phone.ui.common

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Generate a square monochrome QR-code [ImageBitmap] for [payload]. Returns
 * `null` for empty / un-encodable inputs (ZXing will throw on those).
 *
 * Mirrors the QR code helper in
 * `ubo-swift-app/ubo-swift-app/Views/Device/RenderDeviceView.swift` which
 * uses CoreImage's `CIFilterQRCodeGenerator`.
 */
public fun generateQrCodeBitmap(payload: String, sizePx: Int = 512): ImageBitmap? {
    if (payload.isEmpty()) return null
    return runCatching {
        val matrix = MultiFormatWriter().encode(
            payload,
            BarcodeFormat.QR_CODE,
            sizePx,
            sizePx,
            mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M),
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
