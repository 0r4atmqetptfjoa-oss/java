package com.example.educationalapp.AnimalBandGame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.min

enum class MusicianId { FROG, BEAR, CAT }

enum class BandHitQuality(val label: String, val color: Color) {
    PERFECT("Perfect", Color(0xFF00E5FF)),
    GOOD("Good", Color(0xFFFFEB3B)),
    MISS("Miss", Color(0xFFFF5252)),
    NONE("", Color.Transparent)
}

@HiltViewModel
class AnimalBandViewModel @Inject constructor() : ViewModel() {

    // --- State ---
    var frogPlaying by mutableStateOf(false)
    var bearPlaying by mutableStateOf(false)
    var catPlaying by mutableStateOf(false)

    var frogCombo by mutableIntStateOf(0)
    var bearCombo by mutableIntStateOf(0)
    var catCombo by mutableIntStateOf(0)

    var jam by mutableStateOf(0f)
    var isFinalJam by mutableStateOf(false)

    // Hit feedback state
    var lastHitQuality by mutableStateOf(BandHitQuality.NONE)
    var lastHitMusician by mutableStateOf<MusicianId?>(null)
    var lastHitTime by mutableStateOf(0L)

    // Config
    private val bpm = 120f
    val beatPeriodNanos = (60_000_000_000f / bpm).toLong().coerceAtLeast(1L)

    fun activeMusiciansCount(): Int {
        var c = 0
        if (frogPlaying) c++
        if (bearPlaying) c++
        if (catPlaying) c++
        return c
    }

    fun synergyMultiplier(): Float {
        return when (activeMusiciansCount()) {
            0 -> 1f
            1 -> 1f
            2 -> 1.25f
            else -> 1.5f
        }
    }

    fun evaluateHit(nowNanos: Long): BandHitQuality {
        if (nowNanos <= 0L) return BandHitQuality.NONE
        val phase = (nowNanos % beatPeriodNanos).toDouble() / beatPeriodNanos.toDouble()
        val distToBeat = min(phase, 1.0 - phase) * beatPeriodNanos.toDouble()
        val distMs = distToBeat / 1_000_000.0
        return when {
            distMs <= 70.0 -> BandHitQuality.PERFECT // Putin mai iertator
            distMs <= 150.0 -> BandHitQuality.GOOD
            else -> BandHitQuality.MISS
        }
    }

    fun onMusicianClick(id: MusicianId, nowNanos: Long) {
        val quality = evaluateHit(nowNanos)
        lastHitQuality = quality
        lastHitMusician = id
        lastHitTime = System.nanoTime() // Folosim system time pt UI feedback transient

        // Logic combo & jam
        when (id) {
            MusicianId.FROG -> {
                if (!frogPlaying) frogPlaying = true
                frogCombo = updateCombo(frogCombo, quality)
            }
            MusicianId.BEAR -> {
                if (!bearPlaying) bearPlaying = true
                bearCombo = updateCombo(bearCombo, quality)
            }
            MusicianId.CAT -> {
                if (!catPlaying) catPlaying = true
                catCombo = updateCombo(catCombo, quality)
            }
        }
        
        // Update Jam Meter
        val boost = if(quality == BandHitQuality.PERFECT) 0.05f else 0.02f
        jam = (jam + boost * synergyMultiplier()).coerceIn(0f, 1f)
    }

    private fun updateCombo(current: Int, quality: BandHitQuality): Int {
        return when (quality) {
            BandHitQuality.PERFECT, BandHitQuality.GOOD -> (current + 1).coerceAtMost(999)
            BandHitQuality.MISS -> 0
            else -> current
        }
    }

    fun toggleMusician(id: MusicianId) {
        when(id) {
            MusicianId.FROG -> frogPlaying = !frogPlaying
            MusicianId.BEAR -> bearPlaying = !bearPlaying
            MusicianId.CAT -> catPlaying = !catPlaying
        }
    }
    
    fun resetJam() {
        jam = 0f
        isFinalJam = false
    }
}