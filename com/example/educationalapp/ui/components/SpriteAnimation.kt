package com.example.educationalapp.ui.components

import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.Rect
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.ceil

/**
 * A composable that displays a sprite sheet animation.
 *
 * The animation's total duration is controlled by the `frameCount` and `fps` parameters.
 * For the cinematic title (60 frames), an `fps` of 24 will result in a 2.5-second loop (60 / 24 = 2.5).
 */
@Composable
fun SpriteAnimation(
    sheet: ImageBitmap,
    frameCount: Int,
    columns: Int,
    modifier: Modifier = Modifier,
    frameIndex: Int? = null,
    fps: Int = 30,
    loop: Boolean = true,
    isPlaying: Boolean = true,
    onAnimationFinished: () -> Unit = {},
) {
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(isPlaying, loop, frameIndex) {
        if (frameIndex != null) {
            animatable.snapTo(frameIndex.toFloat())
            return@LaunchedEffect
        }

        if (isPlaying) {
            val target = frameCount.toFloat()
            val duration = (frameCount * 1000) / fps

            if (loop) {
                animatable.snapTo(0f)
                animatable.animateTo(
                    targetValue = target,
                    animationSpec = infiniteRepeatable(
                        animation = tween(duration, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )
            } else {
                animatable.snapTo(0f)
                animatable.animateTo(
                    targetValue = target,
                    animationSpec = tween(duration, easing = LinearEasing)
                )
                onAnimationFinished()
            }
        } else {
            animatable.snapTo(0f)
        }
    }

    val sheetWidth = sheet.width
    val sheetHeight = sheet.height
    val frameWidthPx = sheetWidth / columns
    val rows = ceil(frameCount.toFloat() / columns).toInt().coerceAtLeast(1)
    val frameHeightPx = sheetHeight / rows

    val density = LocalDensity.current
    val frameWidthDp = with(density) { frameWidthPx.toDp() }
    val frameHeightDp = with(density) { frameHeightPx.toDp() }

    val baseModifier = if (modifier.toString().contains("Constraints") || modifier.toString().contains("Size")) {
        modifier
    } else {
        Modifier
            .size(frameWidthDp, frameHeightDp)
            .then(modifier)
    }

    Canvas(modifier = baseModifier) {
        val canvasWidth = size.width.toInt()
        val canvasHeight = size.height.toInt()

        val currentFrame = (animatable.value.toInt() % frameCount).coerceIn(0, frameCount - 1)
        val col = currentFrame % columns
        val row = currentFrame / columns

        val srcX = col * frameWidthPx
        val srcY = row * frameHeightPx

        drawIntoCanvas { canvas ->
            val nativePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
                isDither = true
            }

            val srcRect = Rect(srcX, srcY, srcX + frameWidthPx, srcY + frameHeightPx)
            val dstRect = Rect(0, 0, canvasWidth, canvasHeight)

            try {
                canvas.nativeCanvas.drawBitmap(
                    sheet.asAndroidBitmap(),
                    srcRect,
                    dstRect,
                    nativePaint
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

/**
 * A reusable composable helper that loads an [ImageBitmap] from a drawable resource asynchronously.
 * It uses an internal `loadOptimizedBitmap` function to decode the bitmap on a background thread (Dispatchers.IO),
 * preventing the UI thread from being blocked. The decoded bitmap is then returned as state.
 *
 * @param resId The ID of the drawable resource.
 * @param maxTextureSize The maximum size (width or height) of the decoded bitmap. This is used to downsample large images to prevent excessive memory usage.
 * @return The loaded [ImageBitmap], or `null` if it's still loading.
 */
@Composable
fun rememberSheet(
    @DrawableRes resId: Int,
    maxTextureSize: Int = 2048
): ImageBitmap? {
    val context = LocalContext.current
    val resources = context.resources

    var sheet by remember(resId) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(resId, maxTextureSize) {
        sheet = withContext(Dispatchers.IO) {
            loadOptimizedBitmap(resources, resId, maxTextureSize)
        }
    }

    return sheet
}

@Composable
fun rememberAssetSheet(
    path: String,
    maxTextureSize: Int = 2048
): ImageBitmap? {
    val context = LocalContext.current
    val assets = context.assets

    var sheet by remember(path) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(path, maxTextureSize) {
        sheet = withContext(Dispatchers.IO) {
            runCatching {
                loadOptimizedBitmapFromAssets(assets, path, maxTextureSize)
            }.getOrNull()
        }
    }

    return sheet
}

private fun loadOptimizedBitmap(res: Resources, resId: Int, maxTextureSize: Int): ImageBitmap {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeResource(res, resId, options)

    var inSampleSize = 1
    val width = options.outWidth
    val height = options.outHeight

    while (width / inSampleSize > maxTextureSize || height / inSampleSize > maxTextureSize) {
        inSampleSize *= 2
    }

    options.inJustDecodeBounds = false
    options.inSampleSize = inSampleSize

    val bitmap = BitmapFactory.decodeResource(res, resId, options)
    return bitmap.asImageBitmap()
}

private fun loadOptimizedBitmapFromAssets(assets: AssetManager, path: String, maxTextureSize: Int): ImageBitmap {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    var inputStream: InputStream = assets.open(path)
    BitmapFactory.decodeStream(inputStream, null, options)
    inputStream.close()

    var inSampleSize = 1
    val width = options.outWidth
    val height = options.outHeight

    while (width / inSampleSize > maxTextureSize || height / inSampleSize > maxTextureSize) {
        inSampleSize *= 2
    }

    options.inJustDecodeBounds = false
    options.inSampleSize = inSampleSize

    inputStream = assets.open(path)
    val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
    inputStream.close()

    return bitmap!!.asImageBitmap()
}
