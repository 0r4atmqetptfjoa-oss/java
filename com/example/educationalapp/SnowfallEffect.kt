package com.example.educationalapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

private data class SnowflakeState(
    val x: Float,           // Initial X position as a factor of width (0.0 to 1.0)
    val yOffset: Float,     // Initial Y position as a factor of height (0.0 to 1.0)
    val radius: Float,
    val speed: Float,         // Speed factor for falling
    val alpha: Float
)

/**
 * A Composable that renders a continuous, animated snowfall effect.
 * This implementation is stateless and driven by elapsed time for performance and stability.
 */
@Composable
fun SnowfallEffect(modifier: Modifier = Modifier) {
    // A list of snowflake properties, remembered across recompositions.
    val snowflakes = remember {
        (1..200).map {
            SnowflakeState(
                x = Random.nextFloat(),
                yOffset = Random.nextFloat(),
                radius = Random.nextFloat() * 4f + 2f,
                speed = Random.nextFloat() * 0.0001f + 0.00005f, // Adjusted for time-based animation
                alpha = Random.nextFloat() * 0.8f + 0.2f
            )
        }
    }

    // A state that holds the elapsed time. It's updated on every frame.
    var time by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        val startTime = withFrameNanos { it }
        while (true) {
            // Update the time on each animation frame.
            time = withFrameNanos { it } - startTime
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Calculate position based on the total elapsed time, making the animation smooth and stateless.
        val elapsedTime = time / 1_000_000_000f // Time in seconds

        snowflakes.forEach { snowflake ->
            // Calculate the current Y position.
            // The modulo operator creates a looping effect when the snowflake leaves the screen.
            val currentY = (canvasHeight * snowflake.yOffset + (elapsedTime * 100 * snowflake.speed * canvasHeight)) % (canvasHeight + snowflake.radius)

            drawCircle(
                color = Color.White.copy(alpha = snowflake.alpha),
                radius = snowflake.radius,
                center = Offset(
                    x = snowflake.x * canvasWidth,
                    y = currentY.toFloat()
                )
            )
        }
    }
}
