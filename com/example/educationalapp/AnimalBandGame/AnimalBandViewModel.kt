package com.example.educationalapp.AnimalBandGame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.roundToLong

@HiltViewModel
class AnimalBandViewModel @Inject constructor() : ViewModel() {

    // BPM & Beat
    private val bpm: Float = 96f
    val beatPeriodNanos: Long = ((60f / bpm) * 1_000_000_000f).roundToLong()

    // ferestre de timing (secunde până la beat boundary)
    private val goodWindowSec = 0.14f
    private val perfectWindowSec = 0.06f

    // ancoră de timp (pornește când jocul începe să ruleze)
    private var startNanos: Long = 0L

    // HUD / scoring
    var jam by mutableFloatStateOf(0f)
        private set

    var isFinalJam by mutableStateOf(false)
        internal set

    val frog = MusicianRuntimeState(MusicianId.FROG)
    val bear = MusicianRuntimeState(MusicianId.BEAR)
    val cat = MusicianRuntimeState(MusicianId.CAT)

    private val pulseJobs: MutableMap<MusicianId, Job> = mutableMapOf()

    fun activeMusiciansCount(): Int = listOf(frog, bear, cat).count { it.enabled }

    /**
     * Timpul “de melodie” (monotonic), folosit pentru beat + animații.
     */
    fun songTimeNanos(nowNanos: Long): Long {
        if (startNanos == 0L) startNanos = nowNanos
        val t = nowNanos - startNanos
        return if (t < 0L) 0L else t
    }

    /**
     * Beat phase 0..1, sincronizat la start.
     */
    fun beatPhase(nowNanos: Long): Float {
        if (beatPeriodNanos <= 0L) return 0f
        val t = songTimeNanos(nowNanos)
        val mod = t % beatPeriodNanos
        return (mod.toDouble() / beatPeriodNanos.toDouble()).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Indexul beat-ului (0,1,2,...) sincronizat la start.
     */
    fun beatIndex(nowNanos: Long): Long {
        if (beatPeriodNanos <= 0L) return 0L
        return songTimeNanos(nowNanos) / beatPeriodNanos
    }

    fun toggleMusician(id: MusicianId) {
        cancelPulse(id)
        val m = musician(id)
        m.enabled = !m.enabled
        if (!m.enabled) {
            m.performingPersistent = false
            m.performingPulse = false
            m.combo = 0
            m.lastJudgement = null
            m.lastJudgementAtNanos = 0L
        }
    }

    fun togglePerforming(id: MusicianId) {
        cancelPulse(id)
        val m = musician(id)
        if (!m.enabled) return
        m.performingPersistent = !m.performingPersistent
        if (!m.performingPersistent) m.combo = 0
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
        val distSec = dist * (beatPeriodNanos.toDouble() / 1_000_000_000.0).toFloat()

        val judgement = when {
            distSec <= perfectWindowSec -> Judgement.PERFECT
            distSec <= goodWindowSec -> Judgement.GOOD
            else -> Judgement.MISS
        }

        m.lastJudgement = judgement
        m.lastJudgementAtNanos = nowNanos

        val success = judgement != Judgement.MISS
        if (success) {
            m.combo += 1

            val baseAdd = when (judgement) {
                Judgement.PERFECT -> 0.075f
                Judgement.GOOD -> 0.06f
                Judgement.MISS -> 0f
            }
            val comboAdd = (m.combo.coerceAtMost(10) * 0.004f)
            jam = (jam + baseAdd + comboAdd).coerceIn(0f, 1f)
            if (jam >= 1f) isFinalJam = true

            if (!m.performingPersistent) pulsePerforming(id, 650)
        } else {
            m.combo = 0
            jam = (jam - 0.02f).coerceIn(0f, 1f)
        }
    }

    private fun pulsePerforming(id: MusicianId, ms: Long) {
        val m = musician(id)
        pulseJobs[id]?.cancel()
        pulseJobs[id] = viewModelScope.launch {
            m.performingPulse = true
            delay(ms)
            m.performingPulse = false
        }
    }

    private fun cancelPulse(id: MusicianId) {
        pulseJobs[id]?.cancel()
        pulseJobs.remove(id)
        // asigură și starea
        musician(id).performingPulse = false
    }

    private fun musician(id: MusicianId): MusicianRuntimeState =
        when (id) {
            MusicianId.FROG -> frog
            MusicianId.BEAR -> bear
            MusicianId.CAT -> cat
        }
}
