package com.example.educationalapp.EggGame

enum class EggState {
    INTACT, CRACK1, CRACK2, BROKEN, DRAGON
}

enum class DragonAnim {
    IDLE, HOP
}

data class FrameRange(val start: Int, val count: Int) {
    init {
        require(start >= 0)
        require(count >= 0)
    }

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
    val hop: FrameRange,
    val idleFramePeriodSec: Float,
    val hopFramePeriodSec: Float
)
