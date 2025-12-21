package com.example.educationalapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import androidx.compose.ui.res.painterResource

@Composable
fun FrameAnimationPlayer(modifier: Modifier = Modifier, drawableId: Int, frameDelay: Long) {
    val frame = remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(frameDelay)
            frame.value = (frame.value + 1) % 10 // Assuming 10 frames
        }
    }

    Box(modifier = modifier) {
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = null
        )
    }
}