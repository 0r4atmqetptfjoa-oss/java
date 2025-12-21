package com.example.educationalapp.peekaboo

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * SpriteTools - varianta mai safe pentru resurse cu transparență deja existentă.
 *
 * Schimbări față de versiunea inițială:
 *  - processSpriteSheet NU mai scoate automat "alb" (iepurașul e alb -> era afectat).
 *  - poți scoate background doar dacă vrei, via removeBackgroundColor.
 *  - removeBackground devine "no-op" dacă imaginea are deja colțuri transparente.
 */
object SpriteTools {

    suspend fun processSpriteSheet(
        fullBitmap: Bitmap,
        rows: Int,
        cols: Int,
        applySafetyCrop: Boolean = true,
        removeBackgroundColor: Int? = null,
        tolerance: Int = 40,
    ): List<Bitmap> = withContext(Dispatchers.Default) {
        val frames = mutableListOf<Bitmap>()
        if (rows <= 0 || cols <= 0) return@withContext frames

        val frameWidth = fullBitmap.width / cols
        val frameHeight = fullBitmap.height / rows

        // tăiere margini ca să scăpăm de liniile gri din sprite-sheet (10% e safe)
        val safetyMarginX = if (applySafetyCrop) (frameWidth * 0.10f).toInt() else 0
        val safetyMarginY = if (applySafetyCrop) (frameHeight * 0.10f).toInt() else 0

        val cleanWidth = frameWidth - (2 * safetyMarginX)
        val cleanHeight = frameHeight - (2 * safetyMarginY)

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val startX = (col * frameWidth) + safetyMarginX
                val startY = (row * frameHeight) + safetyMarginY

                val sliced = Bitmap.createBitmap(
                    fullBitmap,
                    startX,
                    startY,
                    cleanWidth,
                    cleanHeight
                )

                val out = if (removeBackgroundColor != null) {
                    replaceColorWithTolerance(sliced, removeBackgroundColor, Color.TRANSPARENT, tolerance)
                } else {
                    sliced
                }

                frames.add(out)
            }
        }
        frames
    }

    /**
     * Scoate un fundal solid (dacă există).
     * Dacă imaginea are deja transparență în colțuri, o returnăm ca atare (de obicei e deja decupată).
     */
    suspend fun removeBackground(bitmap: Bitmap, tolerance: Int = 40): Bitmap = withContext(Dispatchers.Default) {
        val bg = guessBackgroundColorOrNull(bitmap) ?: return@withContext bitmap
        replaceColorWithTolerance(bitmap, bg, Color.TRANSPARENT, tolerance)
    }

    private fun guessBackgroundColorOrNull(src: Bitmap): Int? {
        // Dacă colțurile sunt transparente (alpha mic), presupunem că fundalul e deja ok
        fun alphaAt(x: Int, y: Int): Int {
            val pixel = src.getPixel(x.coerceIn(0, src.width - 1), y.coerceIn(0, src.height - 1))
            return Color.alpha(pixel)
        }

        val tlA = alphaAt(0, 0)
        val trA = alphaAt(src.width - 1, 0)
        val blA = alphaAt(0, src.height - 1)
        val brA = alphaAt(src.width - 1, src.height - 1)

        val transparentCorners = listOf(tlA, trA, blA, brA).count { it < 10 }
        if (transparentCorners >= 3) return null

        // altfel, luăm culoarea colțului stânga-sus ca "bg"
        val tl = src.getPixel(0, 0)
        return Color.rgb(Color.red(tl), Color.green(tl), Color.blue(tl))
    }

    private fun replaceColorWithTolerance(
        src: Bitmap,
        targetColor: Int,
        replaceColor: Int,
        tolerance: Int,
    ): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        val targetR = Color.red(targetColor)
        val targetG = Color.green(targetColor)
        val targetB = Color.blue(targetColor)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)

            if (abs(r - targetR) < tolerance &&
                abs(g - targetG) < tolerance &&
                abs(b - targetB) < tolerance
            ) {
                pixels[i] = replaceColor
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }
}
