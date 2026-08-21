package com.ubopod.uboapp.wear.ui.common

import android.graphics.Bitmap

/**
 * Decode a packed RGB byte buffer (3 bytes per pixel, row-major, no
 * padding, no alpha) into a [Bitmap] — the wire format of
 * `FrameStreamDataEvent`. Wear-local counterpart to the phone app's
 * `decodeRgb888Frame` (ui/common/RgbFrameDecoder.kt).
 */
public fun decodeRgb888Frame(data: ByteArray, width: Int, height: Int): Bitmap? {
    if (width <= 0 || height <= 0 || data.size < width * height * 3) return null
    val pixels = IntArray(width * height)
    var offset = 0
    for (i in pixels.indices) {
        val r = data[offset].toInt() and 0xFF
        val g = data[offset + 1].toInt() and 0xFF
        val b = data[offset + 2].toInt() and 0xFF
        pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        offset += 3
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
}
