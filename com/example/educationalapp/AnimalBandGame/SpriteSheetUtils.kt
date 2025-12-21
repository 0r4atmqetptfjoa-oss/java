package com.example.educationalapp.AnimalBandGame

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.educationalapp.R

object SpriteSheetUtils {

    fun decodeBitmap(context: Context, resId: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inScaled = false }
        return BitmapFactory.decodeResource(context.resources, resId, opts)
    }

    /**
     * Split row-major. Atenție:
     * - folosește integer division (w = width/cols, h = height/rows)
     * - la unele sheet-uri rămân 1-2 pixeli “rest” (normal, sigur)
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
