package com.example.educationalapp.AnimalBandGame

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.random.Random

class BandVfxState {
    val particles = mutableStateListOf<BandParticle>()
    val shockwaves = mutableStateListOf<BandShockwave>()
}

object BandVfx {

    fun spawnNoteBurst(state: BandVfxState, anchor: Offset) {
        if (anchor == Offset.Zero) return
        repeat(3) { state.particles.add(BandParticle.note(anchor)) }
    }

    fun spawnShockwave(state: BandVfxState, center: Offset) {
        state.shockwaves.add(BandShockwave(center))
    }

    fun update(state: BandVfxState, dt: Float) {
        state.particles.removeAll { !it.alive }
        state.particles.forEach { it.update(dt) }

        state.shockwaves.removeAll { !it.alive }
        state.shockwaves.forEach { it.update(dt) }
    }

    fun draw(state: BandVfxState, scope: DrawScope, noteImg: ImageBitmap?) {
        state.shockwaves.forEach { it.draw(scope) }
        state.particles.forEach { it.draw(scope, noteImg) }
    }
}

class BandParticle(private var x: Float, private var y: Float) {
    var alive: Boolean = true
    private var age = 0f
    private var vx = (Random.nextFloat() - 0.5f) * 220f
    private var vy = -320f - Random.nextFloat() * 220f

    fun update(dt: Float) {
        age += dt
        x += vx * dt
        y += vy * dt
        vy += 900f * dt
        if (age > 1.4f) alive = false
    }

    fun draw(scope: DrawScope, img: ImageBitmap?) {
        val alpha = (1f - age / 1.4f).coerceIn(0f, 1f)
        val s = (1f - age * 0.25f).coerceIn(0.2f, 1f)

        if (img != null) {
            scope.withTransform({
                translate(x, y)
                scale(s, s)
            }) {
                drawImage(img, topLeft = Offset(-img.width / 2f, -img.height / 2f), alpha = alpha)
            }
        } else {
            scope.drawCircle(Color.White.copy(alpha = alpha), radius = 10f, center = Offset(x, y))
        }
    }

    companion object {
        fun note(anchor: Offset) = BandParticle(anchor.x, anchor.y)
    }
}

class BandShockwave(private val center: Offset) {
    private var radius = 0f
    private var alpha = 1f
    var alive: Boolean = true

    fun update(dt: Float) {
        radius += 900f * dt
        alpha -= dt * 1.6f
        if (alpha <= 0f) alive = false
    }

    fun draw(scope: DrawScope) {
        scope.drawCircle(
            color = Color.White.copy(alpha = alpha.coerceIn(0f, 1f)),
            radius = radius,
            center = center,
            style = Stroke(width = 18f)
        )
    }
}
