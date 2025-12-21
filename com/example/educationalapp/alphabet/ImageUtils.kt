package com.example.educationalapp.alphabet

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

fun loadScaledBitmap(context: android.content.Context, resId: Int, maxDim: Int = 2048): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, options)
        val width = options.outWidth
        val height = options.outHeight
        if (width <= 0 || height <= 0) return null

        var inSample = 1
        while (width / inSample > maxDim || height / inSample > maxDim) {
            inSample *= 2
        }

        val opts2 = BitmapFactory.Options().apply { inSampleSize = inSample }
        BitmapFactory.decodeResource(context.resources, resId, opts2)
    } catch (t: Throwable) {
        t.printStackTrace()
        null
    }
}

@Composable
fun rememberScaledImageBitmap(resId: Int, maxDim: Int = 2048): ImageBitmap? {
    val ctx = LocalContext.current
    val bmp = remember(resId, maxDim) { loadScaledBitmap(ctx, resId, maxDim) }
    return bmp?.asImageBitmap()
}
