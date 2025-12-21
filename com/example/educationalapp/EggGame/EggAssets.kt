package com.example.educationalapp.EggGame

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.educationalapp.R

data class EggLoadedAssets(
    val dragonFrames: List<ImageBitmap>,
    val dragonSpec: SpriteSheetSpec,
    val shellPiece: ImageBitmap?
)

object EggAssets {

    /**
     * dragon_sheet.png
     * Dimensiune: 2048 x 1093
     * Grid: 4 x 6 => 24 frames
     * Frame: 341 x 273 (raman 2px dreapta, 1px jos)
     *
     * Layout recomandat:
     * - Idle: frames 0..11 (primele 2 randuri)
     * - Hop:  frames 12..23 (ultimele 2 randuri, include "cloud puffs")
     */
    val dragonSpec = SpriteSheetSpec(
        rows = 4,
        cols = 6,
        idle = FrameRange(start = 0, count = 12),
        hop = FrameRange(start = 12, count = 12),
        idleFramePeriodSec = 0.16f,
        hopFramePeriodSec = 0.11f
    )

    suspend fun load(context: Context): EggLoadedAssets {
        val dragonBmp = SpriteSheetUtils.decodeBitmap(context, R.drawable.dragon_sheet)
        val frames = SpriteSheetUtils
            .splitSpriteSheet(dragonBmp, dragonSpec.rows, dragonSpec.cols)
            .map { it.asImageBitmap() }

        val shellBmp = SpriteSheetUtils.decodeBitmap(context, R.drawable.vfx_shell_piece)
        val shell = shellBmp?.asImageBitmap()

        return EggLoadedAssets(
            dragonFrames = frames,
            dragonSpec = dragonSpec,
            shellPiece = shell
        )
    }
}
