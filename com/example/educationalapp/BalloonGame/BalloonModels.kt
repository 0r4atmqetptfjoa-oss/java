package com.example.educationalapp.BalloonGame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class BalloonState(
    val id: Long,
    val imageRes: Int,
    val speed: Float,
    val startX: Float
) {
    // Folosim State pentru ca Compose să redeseneze eficient doar când se schimbă Y
    var y by mutableFloatStateOf(0f)
    var isPopped by mutableStateOf(false)
}

data class PopParticle(
    val id: Long,
    val x: Float,
    val y: Float,
    val angle: Float
) {
    var alpha by mutableFloatStateOf(1f)
    var currentX by mutableFloatStateOf(x)
    var currentY by mutableFloatStateOf(y)
}