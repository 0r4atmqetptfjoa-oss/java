package com.example.educationalapp.EggGame

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

object SpriteSheetUtils {

    /** IMPORTANT: inScaled=false ca sa nu se "rupa" grid-ul pe device-uri diferite. */
    fun decodeBitmap(context: Context, resId: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inScaled = false }
        return BitmapFactory.decodeResource(context.resources, resId, opts)
    }

    /**
     * Split row-major (r*cols + c). Folosește integer division.
     * Dacă sheet-ul are 1-2 pixeli în plus pe dreapta/jos, e OK: vor fi ignorați.
     */
    fun splitSpriteSheet(sheet: Bitmap?, rows: Int, cols: Int): List<Bitmap> {
        if (sheet == null || rows <= 0 || cols <= 0) return emptyList()
        val frameW = sheet.width / cols
        val frameH = sheet.height / rows
        if (frameW <= 0 || frameH <= 0) return emptyList()

        val out = ArrayList<Bitmap>(rows * cols)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                out.add(Bitmap.createBitmap(sheet, c * frameW, r * frameH, frameW, frameH))
            }
        }
        return out
    }
}
