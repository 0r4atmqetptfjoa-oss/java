package com.example.educationalapp.AnimalBandGame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToLong

@HiltViewModel
class AnimalBandViewModel @Inject constructor() : ViewModel() {

    // BPM & Beat
    private val bpm: Float = 96f
    val beatPeriodNanos: Long = ((60f / bpm) * 1_000_000_000f).roundToLong()

    // cât de “iertător” e jocul (secunde până la beat)
    private val baseHitWindowSec = 0.14f

    // HUD / scoring
    var jam: Float = 0f
        private set

    var isFinalJam: Boolean = false
        internal set

    val frog = MusicianRuntimeState(MusicianId.FROG)
    val bear = MusicianRuntimeState(MusicianId.BEAR)
    val cat = MusicianRuntimeState(MusicianId.CAT)

    private var pulseJobs: MutableMap<MusicianId, Job> = mutableMapOf()

    fun activeMusiciansCount(): Int = listOf(frog, bear, cat).count { it.enabled }

    fun toggleMusician(id: MusicianId) {
        val m = musician(id)
        m.enabled = !m.enabled
        if (!m.enabled) {
            m.performing = false
            m.combo = 0
        }
    }

    fun togglePerforming(id: MusicianId) {
        val m = musician(id)
        if (!m.enabled) return
        m.performing = !m.performing
        if (!m.performing) m.combo = 0
    }

    /**
     * Tap = “hit timing” (combo + jam).
     * Dacă e hit bun => pulse performing scurt (dacă nu e deja performing persistent).
     */
    fun onMusicianTap(id: MusicianId, nowNanos: Long) {
        val m = musician(id)
        if (!m.enabled) return

        val phase = beatPhase(nowNanos) // 0..1
        val dist = min(phase, 1f - phase) // distanța până la beat boundary (0 sau 1)

        val hitWindow = baseHitWindowSec
        val distSec = dist * (beatPeriodNanos.toDouble() / 1_000_000_000.0).toFloat()

        val success = distSec <= hitWindow
        if (success) {
            m.combo += 1
            jam = (jam + 0.06f + (m.combo.coerceAtMost(10) * 0.004f)).coerceIn(0f, 1f)
            if (jam >= 1f) isFinalJam = true

            if (!m.performing) pulsePerforming(id, 650)
        } else {
            // miss => rupe combo (dar nu penalizează jam agresiv)
            m.combo = 0
            jam = (jam - 0.02f).coerceIn(0f, 1f)
        }
    }

    private fun pulsePerforming(id: MusicianId, ms: Long) {
        val m = musician(id)
        pulseJobs[id]?.cancel()
        pulseJobs[id] = viewModelScope.launch {
            m.performing = true
            delay(ms)
            // revine la false doar dacă nu e “persistent toggle” (aici e pulse, deci îl punem false)
            m.performing = false
        }
    }

    private fun musician(id: MusicianId): MusicianRuntimeState =
        when (id) {
            MusicianId.FROG -> frog
            MusicianId.BEAR -> bear
            MusicianId.CAT -> cat
        }

    private fun beatPhase(nowNanos: Long): Float {
        if (beatPeriodNanos <= 0) return 0f
        val mod = nowNanos % beatPeriodNanos
        return (mod.toDouble() / beatPeriodNanos.toDouble()).toFloat().coerceIn(0f, 1f)
    }
}
