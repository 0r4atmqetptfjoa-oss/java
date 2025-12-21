package com.example.educationalapp.peekaboo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.random.Random

internal data class ConfettiParticleSpec(
    val startX: Float,
    val startY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val size: Float,
    val rotationSpeedDeg: Float,
    val isCircle: Boolean,
    val color: Color,
)

@Composable
fun ConfettiOverlay(
    visible: Boolean,
    burstKey: Int,
    modifier: Modifier = Modifier,
    durationMs: Int = 900,
    particleCount: Int = 40,
    originX: Float = 0.5f,
    originY: Float = 0.65f,
) {
    val particles = remember(burstKey) {
        val rnd = Random(burstKey)
        List(particleCount) {
            val angle = rnd.nextFloat() * (PI.toFloat() * 0.9f) + (PI.toFloat() * 0.05f)
            val speed = 0.20f + rnd.nextFloat() * 0.55f

            val vx = cos(angle) * speed * (if (rnd.nextBoolean()) 1f else -1f)
            val vy = -(0.35f + rnd.nextFloat() * 0.55f)

            ConfettiParticleSpec(
                startX = originX + (rnd.nextFloat() - 0.5f) * 0.06f,
                startY = originY + (rnd.nextFloat() - 0.5f) * 0.03f,
                velocityX = vx,
                velocityY = vy,
                size = 2.5f + rnd.nextFloat() * 5.5f,
                rotationSpeedDeg = (rnd.nextFloat() - 0.5f) * 540f,
                isCircle = rnd.nextFloat() < 0.35f,
                color = listOf(
                    Color(0xFFFFD54F),
                    Color(0xFFFF8A80),
                    Color(0xFF80D8FF),
                    Color(0xFFB9F6CA),
                    Color(0xFFE1BEE7),
                    Color.White,
                )[rnd.nextInt(6)]
            )
        }
    }

    // Progress 0..1
    val progress = remember(burstKey) { Animatable(0f) }

    LaunchedEffect(visible, burstKey) {
        if (visible) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMs, easing = LinearEasing)
            )
        } else {
            progress.snapTo(0f)
        }
    }

    if (!visible) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val t = progress.value
        val w = size.width
        val h = size.height
        val gravity = 1.25f

        particles.forEach { p ->
            val sx = p.startX * w
            val sy = p.startY * h

            val x = sx + (p.velocityX * w) * t
            val y = sy + (p.velocityY * h) * t + (gravity * h) * (t * t) * 0.18f

            val alpha = (1f - t).coerceIn(0f, 1f)
            val particleSizePx = p.size * (1f + t * 0.25f)
            val rotation = p.rotationSpeedDeg * t

            rotate(degrees = rotation, pivot = Offset(x, y)) {
                if (p.isCircle) {
                    drawCircle(
                        color = p.color.copy(alpha = alpha),
                        radius = particleSizePx,
                        center = Offset(x, y),
                    )
                } else {
                    drawRect(
                        color = p.color.copy(alpha = alpha),
                        topLeft = Offset(x - particleSizePx, y - particleSizePx),
                        size = Size(particleSizePx * 2f, particleSizePx * 1.2f)
                    )
                }
            }
        }
    }
}

internal data class SparkleSpec(
    val x: Float,
    val y: Float,
    val radius: Float,
    val speed: Float,
    val phase: Float,
)

@Composable
fun SparkleOverlay(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    sparkleCount: Int = 18,
) {
    if (!enabled) return

    val seed = remember { Random.nextInt() }
    val sparkles = remember(seed) {
        val rnd = Random(seed)
        List(sparkleCount) {
            SparkleSpec(
                x = rnd.nextFloat(),
                y = rnd.nextFloat(),
                radius = 1.2f + rnd.nextFloat() * 3.8f,
                speed = 0.03f + rnd.nextFloat() * 0.08f,
                phase = rnd.nextFloat()
            )
        }
    }

    val infinite = androidx.compose.animation.core.rememberInfiniteTransition(label = "sparkles")
    val t by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "sparkleProgress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        sparkles.forEach { s ->
            val y = ((s.y - (t * s.speed)) % 1f + 1f) % 1f
            val pulse = 0.35f + 0.65f * kotlin.math.abs(kotlin.math.sin((t + s.phase) * 2f * PI.toFloat()))

            drawCircle(
                color = Color.White.copy(alpha = (0.08f * pulse).coerceIn(0f, 0.12f)),
                radius = s.radius,
                center = Offset(s.x * w, y * h),
            )
        }
    }
}