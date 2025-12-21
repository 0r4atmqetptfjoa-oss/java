
package com.example.educationalapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap

@Composable
fun resizeBitmap(imageBitmap: ImageBitmap, maxWidth: Int, maxHeight: Int): ImageBitmap {
    val originalBitmap = imageBitmap.asAndroidBitmap()
    val originalWidth = originalBitmap.width
    val originalHeight = originalBitmap.height

    if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
        return imageBitmap
    }

    val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()
    val newWidth: Int
    val newHeight: Int

    if (originalWidth > originalHeight) {
        newWidth = maxWidth
        newHeight = (newWidth / aspectRatio).toInt()
    } else {
        newHeight = maxHeight
        newWidth = (newHeight * aspectRatio).toInt()
    }

    val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
    return resizedBitmap.asImageBitmap()
}
