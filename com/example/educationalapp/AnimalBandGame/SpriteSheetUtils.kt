package com.example.educationalapp.AnimalBandGame

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

object SpriteSheetUtils {

    fun decodeBitmap(context: Context, resId: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inScaled = false }
        return BitmapFactory.decodeResource(context.resources, resId, opts)
    }
}
