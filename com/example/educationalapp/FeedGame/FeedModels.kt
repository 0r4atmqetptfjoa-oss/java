package com.example.educationalapp.FeedGame

import androidx.annotation.DrawableRes

enum class MonsterState {
    IDLE,
    EATING
}

data class FoodItem(
    val id: Int,
    @DrawableRes val resId: Int,
    val name: String,
    val isHealthy: Boolean
)

data class FrameRange(val start: Int, val count: Int) {
    init { require(start >= 0); require(count >= 0) }

    fun index(localFrame: Int): Int {
        if (count <= 0) return start
        val lf = if (localFrame >= 0) localFrame else 0
        return start + (lf % count)
    }
}

data class SpriteSheetSpec(
    val rows: Int,
    val cols: Int,
    val idle: FrameRange,
    val eat: FrameRange,
    val idleFramePeriodSec: Float,
    val eatFramePeriodSec: Float
)
