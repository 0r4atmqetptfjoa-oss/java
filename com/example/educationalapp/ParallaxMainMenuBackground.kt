package com.example.educationalapp

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Creates a multi-layered parallax background with a gentle, continuous horizontal panning animation.
 *
 * For the effect to work correctly, the drawable resources should be wider than the
 * screen to avoid showing empty edges during translation.
 */
@Composable
fun ParallaxMainMenuBackground() {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val infiniteTransition = rememberInfiniteTransition(label = "parallaxTransition")

    // Animates a value from -1f to 1f and back again, creating a seamless loop.
    val parallaxOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 30000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "parallaxOffset"
    )

    // The maximum horizontal movement in Dp. Images should be wider than the screen
    // by at least twice this amount to avoid visible edges.
    val maxTranslation = screenWidth.value * 0.1f

    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: Sky (moves the least)
        ParallaxLayer(
            drawableRes = R.drawable.bg_parallax_sky,
            translation = parallaxOffset * maxTranslation * 0.2f
        )

        // Layer 2: Village (moves a bit more)
        ParallaxLayer(
            drawableRes = R.drawable.bg_parallax_village,
            translation = parallaxOffset * maxTranslation * 0.4f
        )

        // New Layer 3: Tree (now further back)
        ParallaxLayer(
            drawableRes = R.drawable.bg_parallax_tree,
            translation = parallaxOffset * maxTranslation * 0.5f, // Moves slower, appears further
            offsetX = 40.dp, // Moved more to the right
            offsetY = 30.dp
        )

        // New Layer 4: Kids (now in front of the tree)
        ParallaxLayer(
            drawableRes = R.drawable.bg_parallax_kids,
            translation = parallaxOffset * maxTranslation * 0.6f
        )

        // Layer 5: Foreground (moves the most)
        ParallaxLayer(
            drawableRes = R.drawable.bg_parallax_foreground,
            translation = parallaxOffset * maxTranslation * 1.2f
        )
    }
}

@Composable
private fun ParallaxLayer(
    drawableRes: Int,
    translation: Float,
    alpha: Float = 1.0f,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp
) {
    Image(
        painter = painterResource(id = drawableRes),
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = translation + offsetX.toPx()
                translationY = offsetY.toPx()
                this.alpha = alpha
            },
        contentScale = ContentScale.Crop
    )
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun ParallaxMainMenuBackgroundPreview() {
    // This preview will likely fail if the drawable resources are not available.
    // It is here to help check for compilation errors.
    ParallaxMainMenuBackground()
}
