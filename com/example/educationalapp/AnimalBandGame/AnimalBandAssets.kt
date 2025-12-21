package com.example.educationalapp.AnimalBandGame

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.educationalapp.R

data class LoadedSheet(
    val frames: List<ImageBitmap>,
    val spec: SpriteSheetSpec
)

object AnimalBandAssets {

    // IMPORTANT: conform analiză sprite sheet-uri
    // Bear/Cat: 4x8 => 32 frames (idle 0..15, play 16..31)
    // Frog:     4x6 => 24 frames (idle 0..11, play 12..23)

    val frogSpec = SpriteSheetSpec(
        rows = 4,
        cols = 6,
        idle = FrameRange(start = 0, count = 12),
        play = FrameRange(start = 12, count = 12),
        idleFramePeriodSec = 0.14f,
        playFramePeriodSec = 0.10f
    )

    val bearSpec = SpriteSheetSpec(
        rows = 4,
        cols = 8,
        idle = FrameRange(start = 0, count = 16),
        play = FrameRange(start = 16, count = 16),
        idleFramePeriodSec = 0.14f,
        playFramePeriodSec = 0.10f
    )

    val catSpec = SpriteSheetSpec(
        rows = 4,
        cols = 8,
        idle = FrameRange(start = 0, count = 16),
        play = FrameRange(start = 16, count = 16),
        idleFramePeriodSec = 0.14f,
        playFramePeriodSec = 0.10f
    )

    suspend fun loadAll(context: Context): Map<MusicianId, LoadedSheet> {
        val frogBmp = SpriteSheetUtils.decodeBitmap(context, R.drawable.band_frog_sheet)
        val bearBmp = SpriteSheetUtils.decodeBitmap(context, R.drawable.band_bear_sheet)
        val catBmp = SpriteSheetUtils.decodeBitmap(context, R.drawable.band_cat_sheet)

        val frogFrames = SpriteSheetUtils.splitSpriteSheet(frogBmp, frogSpec.rows, frogSpec.cols).map { it.asImageBitmap() }
        val bearFrames = SpriteSheetUtils.splitSpriteSheet(bearBmp, bearSpec.rows, bearSpec.cols).map { it.asImageBitmap() }
        val catFrames = SpriteSheetUtils.splitSpriteSheet(catBmp, catSpec.rows, catSpec.cols).map { it.asImageBitmap() }

        return mapOf(
            MusicianId.FROG to LoadedSheet(frogFrames, frogSpec),
            MusicianId.BEAR to LoadedSheet(bearFrames, bearSpec),
            MusicianId.CAT to LoadedSheet(catFrames, catSpec),
        )
    }
}
