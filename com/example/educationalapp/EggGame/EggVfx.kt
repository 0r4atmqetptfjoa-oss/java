package com.example.educationalapp.EggGame

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.random.Random

class EggVfxState {
    val shellPieces = mutableStateListOf<ShellParticle>()
}

object EggVfx {

    fun spawnCrack(state: EggVfxState, origin: Offset, intensity: Int) {
        // cateva particule mici la fiecare crack
        val n = (3 + intensity).coerceIn(3, 7)
        repeat(n) { state.shellPieces.add(ShellParticle.spawn(origin, burst = false)) }
    }

    fun spawnBurst(state: EggVfxState, origin: Offset) {
        // burst mare cand oul se sparge complet
        repeat(14) { state.shellPieces.add(ShellParticle.spawn(origin, burst = true)) }
    }

    fun update(state: EggVfxState, dt: Float) {
        state.shellPieces.removeAll { !it.alive }
        state.shellPieces.forEach { it.update(dt) }
    }

    fun draw(state: EggVfxState, scope: DrawScope, shellImg: ImageBitmap?) {
        state.shellPieces.forEach { it.draw(scope, shellImg) }
    }
}

class ShellParticle(
    private var x: Float,
    private var y: Float,
    private var vx: Float,
    private var vy: Float,
    private var rot: Float,
    private var rotV: Float,
    private var scale: Float
) {
    var alive: Boolean = true
    private var age = 0f
    private val life = 1.2f

    fun update(dt: Float) {
        age += dt
        x += vx * dt
        y += vy * dt
        vy += 1100f * dt
        rot += rotV * dt
        if (age >= life) alive = false
    }

    fun draw(scope: DrawScope, img: ImageBitmap?) {
        val a = (1f - age / life).coerceIn(0f, 1f)
        if (img == null) return

        scope.withTransform({
            translate(x, y)
            rotate(rot * (180f / PI.toFloat()))
            scale(scale, scale)
        }) {
            drawImage(
                image = img,
                topLeft = Offset(-img.width / 2f, -img.height / 2f),
                alpha = a
            )
        }
    }

    companion object {
        fun spawn(origin: Offset, burst: Boolean): ShellParticle {
            val dir = Random.nextFloat() * 2f * PI.toFloat()
            val spd = if (burst) 520f + Random.nextFloat() * 520f else 280f + Random.nextFloat() * 260f
            val vx = kotlin.math.cos(dir) * spd
            val vy = -kotlin.math.abs(kotlin.math.sin(dir)) * spd - (if (burst) 260f else 120f)
            val rotV = (Random.nextFloat() - 0.5f) * 12f
            val sc = if (burst) 0.70f + Random.nextFloat() * 0.55f else 0.50f + Random.nextFloat() * 0.45f
            return ShellParticle(
                x = origin.x,
                y = origin.y,
                vx = vx,
                vy = vy,
                rot = Random.nextFloat() * 2f * PI.toFloat(),
                rotV = rotV,
                scale = sc
            )
        }
    }
}
