package com.example.educationalapp.AnimalBandGame

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.educationalapp.R

/**
 * V2: nu mai “split”-uim sheet-ul în zeci de Bitmap-uri.
 * Păstrăm sheet-ul ca un singur ImageBitmap și desenăm frame-ul prin source-rect.
 * Avantaje: memorie mai mică, încărcare mai rapidă, posibilitate de FilterQuality High,
 * și crossfade între frame-uri pentru animație mai fluidă.
 */
data class LoadedSheet(
    val sheet: ImageBitmap,
    val spec: SpriteSheetSpec,
    val frameW: Int,
    val frameH: Int
)

object AnimalBandAssets {

    // Bear/Cat: 4x8 => 32 frames (idle 0..15, play 16..31)
    // Frog:     4x6 => 24 frames (idle 0..11, play 12..23)
    val frogSpec = SpriteSheetSpec(
        rows = 4,
        cols = 6,
        idle = FrameRange(start = 0, count = 12),
        play = FrameRange(start = 12, count = 12),
        idleFramePeriodSec = 0.12f,
        playFramePeriodSec = 0.08f
    )

    val bearSpec = SpriteSheetSpec(
        rows = 4,
        cols = 8,
        idle = FrameRange(start = 0, count = 16),
        play = FrameRange(start = 16, count = 16),
        idleFramePeriodSec = 0.12f,
        playFramePeriodSec = 0.08f
    )

    val catSpec = SpriteSheetSpec(
        rows = 4,
        cols = 8,
        idle = FrameRange(start = 0, count = 16),
        play = FrameRange(start = 16, count = 16),
        idleFramePeriodSec = 0.12f,
        playFramePeriodSec = 0.08f
    )

    suspend fun loadAll(context: Context): Map<MusicianId, LoadedSheet> {
        val frogBmp = SpriteSheetUtils.decodeBitmap(context, R.drawable.band_frog_sheet) ?: return emptyMap()
        val bearBmp = SpriteSheetUtils.decodeBitmap(context, R.drawable.band_bear_sheet) ?: return emptyMap()
        val catBmp = SpriteSheetUtils.decodeBitmap(context, R.drawable.band_cat_sheet) ?: return emptyMap()

        fun build(bmp: android.graphics.Bitmap, spec: SpriteSheetSpec): LoadedSheet {
            val frameW = (bmp.width / spec.cols).coerceAtLeast(1)
            val frameH = (bmp.height / spec.rows).coerceAtLeast(1)
            return LoadedSheet(
                sheet = bmp.asImageBitmap(),
                spec = spec,
                frameW = frameW,
                frameH = frameH
            )
        }

        return mapOf(
            MusicianId.FROG to build(frogBmp, frogSpec),
            MusicianId.BEAR to build(bearBmp, bearSpec),
            MusicianId.CAT to build(catBmp, catSpec),
        )
    }
}
