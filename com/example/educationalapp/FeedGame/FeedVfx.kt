package com.example.educationalapp.FeedGame

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.random.Random

class FeedVfxState {
    val stars = mutableStateListOf<StarParticle>()
}

object FeedVfx {

    fun spawnStars(state: FeedVfxState, origin: Offset, count: Int = 7) {
        if (origin == Offset.Zero) return
        repeat(count.coerceIn(3, 12)) { state.stars.add(StarParticle.spawn(origin)) }
    }

    fun update(state: FeedVfxState, dt: Float) {
        state.stars.removeAll { !it.alive }
        state.stars.forEach { it.update(dt) }
    }

    fun draw(state: FeedVfxState, scope: DrawScope, starImg: ImageBitmap?) {
        state.stars.forEach { it.draw(scope, starImg) }
    }
}

class StarParticle(
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
    private val life = 1.0f

    fun update(dt: Float) {
        age += dt
        x += vx * dt
        y += vy * dt
        vy += 850f * dt
        rot += rotV * dt
        if (age >= life) alive = false
    }

    fun draw(scope: DrawScope, img: ImageBitmap?) {
        if (img == null) return
        val a = (1f - age / life).coerceIn(0f, 1f)
        val s = (scale * (1f - age * 0.35f)).coerceIn(0.25f, 1.2f)

        scope.withTransform({
            translate(x, y)
            rotate(rot * (180f / PI.toFloat()))
            scale(s, s)
        }) {
            drawImage(
                image = img,
                topLeft = Offset(-img.width / 2f, -img.height / 2f),
                alpha = a
            )
        }
    }

    companion object {
        fun spawn(origin: Offset): StarParticle {
            val dir = Random.nextFloat() * 2f * PI.toFloat()
            val spd = 320f + Random.nextFloat() * 520f
            val vx = kotlin.math.cos(dir) * spd
            val vy = -kotlin.math.abs(kotlin.math.sin(dir)) * spd - 220f
            val rotV = (Random.nextFloat() - 0.5f) * 10f
            val sc = 0.55f + Random.nextFloat() * 0.55f
            return StarParticle(
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
