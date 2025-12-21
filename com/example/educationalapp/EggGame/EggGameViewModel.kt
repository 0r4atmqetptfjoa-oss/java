package com.example.educationalapp.EggGame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.min

enum class EggState { INTACT, CRACK1, CRACK2, BROKEN, DRAGON }
enum class DragonAnim { IDLE, RISE, CELEBRATE }

@HiltViewModel
class EggGameViewModel @Inject constructor() : ViewModel() {
    var gameState by mutableStateOf(EggState.INTACT)
    var hatchProgress by mutableStateOf(0f)
    var dragonMode by mutableStateOf(DragonAnim.IDLE)
    
    private val bpm = 120f
    val beatPeriodNanos = (60_000_000_000f / bpm).toLong()

    fun onTapEgg(nowNanos: Long) {
        if (gameState == EggState.DRAGON) {
            dragonMode = DragonAnim.CELEBRATE
            return
        }
        
        val phase = (nowNanos % beatPeriodNanos).toDouble() / beatPeriodNanos
        val dist = min(phase, 1.0 - phase)
        val isHit = dist < 0.2
        
        if (isHit || nowNanos == 0L) { // 0L pt debug/click simplu
            hatchProgress += 0.15f
            updateState()
        }
    }
    
    private fun updateState() {
        gameState = when {
            hatchProgress >= 1f -> EggState.DRAGON
            hatchProgress >= 0.6f -> EggState.BROKEN 
            hatchProgress >= 0.3f -> EggState.CRACK2
            hatchProgress >= 0.1f -> EggState.CRACK1
            else -> EggState.INTACT
        }
        if (gameState == EggState.DRAGON) dragonMode = DragonAnim.RISE
    }
}