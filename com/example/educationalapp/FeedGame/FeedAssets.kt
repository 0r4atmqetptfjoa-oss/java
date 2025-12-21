package com.example.educationalapp.FeedGame

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.educationalapp.R

data class FeedLoadedAssets(
    val monsterFrames: List<ImageBitmap>,
    val monsterSpec: SpriteSheetSpec,
    val starImage: ImageBitmap?
)

object FeedAssets {

    /**
     * monster_sheet.png
     * Dimensiune: 2048 x 1117
     * Grid: 4 x 8 => 32 frames
     * Frame: 256 x 279 (ramane 1px jos)
     *
     * Layout recomandat:
     * - Idle: frames 0..15 (primele 2 randuri)
     * - Eat:  frames 16..31 (ultimele 2 randuri)
     */
    val monsterSpec = SpriteSheetSpec(
        rows = 4,
        cols = 8,
        idle = FrameRange(start = 0, count = 16),
        eat = FrameRange(start = 16, count = 16),
        idleFramePeriodSec = 0.14f,
        eatFramePeriodSec = 0.10f
    )

    suspend fun load(context: Context): FeedLoadedAssets {
        val monsterBmp = SpriteSheetUtils.decodeBitmap(context, R.drawable.monster_sheet)
        val frames = SpriteSheetUtils
            .splitSpriteSheet(monsterBmp, monsterSpec.rows, monsterSpec.cols)
            .map { it.asImageBitmap() }

        val starBmp = SpriteSheetUtils.decodeBitmap(context, R.drawable.vfx_star)
        val star = starBmp?.asImageBitmap()

        return FeedLoadedAssets(
            monsterFrames = frames,
            monsterSpec = monsterSpec,
            starImage = star
        )
    }
}
