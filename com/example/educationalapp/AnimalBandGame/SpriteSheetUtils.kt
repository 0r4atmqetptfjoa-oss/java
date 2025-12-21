package com.example.educationalapp.AnimalBandGame

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

object SpriteSheetUtils {

    fun decodeBitmap(context: Context, resId: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inScaled = false }
        return BitmapFactory.decodeResource(context.resources, resId, opts)
    }

    /**
     * Estimează poziția "tălpilor" în cadrul unui frame, pe baza alpha-ului.
     * Returnează fracția (0..1) unde 1 înseamnă "jos de tot".
     *
     * Motiv: multe sprite-sheet-uri au padding transparent jos; dacă aliniez "feet" la
     * bottom-ul frame-ului, personajul pare că plutește. Cu această fracție aliniez
     * "ground" la ultimul pixel opac.
     */
    fun estimateFootYFrac(sheet: Bitmap, rows: Int, cols: Int): Float {
        val frameW = (sheet.width / cols).coerceAtLeast(1)
        val frameH = (sheet.height / rows).coerceAtLeast(1)

        val pixels = IntArray(frameW * frameH)
        var maxBottom = 0

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x0 = c * frameW
                val y0 = r * frameH
                sheet.getPixels(pixels, 0, frameW, x0, y0, frameW, frameH)

                var found = false
                var bottom = 0
                for (y in frameH - 1 downTo 0) {
                    val rowIdx = y * frameW
                    for (x in 0 until frameW) {
                        val a = (pixels[rowIdx + x] ushr 24) and 0xFF
                        if (a > 16) {
                            bottom = y
                            found = true
                            break
                        }
                    }
                    if (found) break
                }

                if (found) {
                    if (bottom > maxBottom) maxBottom = bottom
                }
            }
        }

        // +1 deoarece y este index de pixel; pentru fracție folosim "height".
        val frac = (maxBottom + 1).toFloat() / frameH.toFloat()
        return frac.coerceIn(0.70f, 1.0f)
    }
}
