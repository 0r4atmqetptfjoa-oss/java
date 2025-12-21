package com.example.educationalapp.AnimalBandGame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class MusicianId { FROG, BEAR, CAT }

enum class Judgement { PERFECT, GOOD, MISS }

data class FrameRange(val start: Int, val count: Int) {
    init { require(start >= 0 && count >= 0) }
    fun safeIndex(localFrame: Int): Int = start + (if (count == 0) 0 else (localFrame % count))
}

data class SpriteSheetSpec(
    val rows: Int,
    val cols: Int,
    // layout: primele 2 rânduri = idle, ultimele 2 = play (pentru bear/cat/monster)
    // pentru frog/dragon: primele 2 rânduri = idle, ultimele 2 = action
    val idle: FrameRange,
    val play: FrameRange,
    val idleFramePeriodSec: Float,
    val playFramePeriodSec: Float
)

class MusicianRuntimeState(
    val id: MusicianId
) {
    var enabled by mutableStateOf(true)

    // persistent (toggle)
    var performingPersistent by mutableStateOf(false)

    // temporar (pulse la hit)
    var performingPulse by mutableStateOf(false)

    // expus către UI: adevărat dacă e oricare activ
    val performing: Boolean
        get() = performingPersistent || performingPulse

    var combo by mutableIntStateOf(0)

    // feedback scurt în UI
    var lastJudgement by mutableStateOf<Judgement?>(null)
    var lastJudgementAtNanos by mutableLongStateOf(0L)
}
