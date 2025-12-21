package com.example.educationalapp.FeedGame

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.educationalapp.AnimalBandGame.splitSpriteSheet
import com.example.educationalapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@Composable
fun FeedGameScreen(
    viewModel: FeedGameViewModel = hiltViewModel(),
    onHome: () -> Unit
) {
    val context = LocalContext.current
    var monsterFrames by remember { mutableStateOf(listOf<androidx.compose.ui.graphics.ImageBitmap>()) }
    var loaded by remember { mutableStateOf(false) }

    // --- FIX CRITIC: inScaled = false + CORECT GRID: 4 x 8 ---
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val opts = BitmapFactory.Options().apply { inScaled = false }
            val sheet = BitmapFactory.decodeResource(context.resources, R.drawable.monster_sheet, opts)
            val frames = splitSpriteSheet(sheet, 4, 8).map { it.asImageBitmap() } // 32 frames
            withContext(Dispatchers.Main) {
                monsterFrames = frames
                loaded = true
            }
        }
    }

    // Animatie Monstru (delta real)
    val cols = 8
    val idleCount = cols * 2          // 16 (primele 2 rânduri)
    val eatCount = cols * 2           // 16 (ultimele 2 rânduri)
    val isEating = viewModel.monsterState == FeedGameViewModel.STATE_EATING
    val baseIndex = if (isEating) idleCount else 0
    val animCount = if (isEating) eatCount else idleCount

    var localFrame by remember { mutableIntStateOf(0) }

    LaunchedEffect(loaded, viewModel.monsterState) {
        if (!loaded) return@LaunchedEffect
        localFrame = 0

        val framePeriod = if (isEating) 0.10f else 0.14f // mănâncă puțin mai rapid, idle mai lent
        var last = 0L
        var acc = 0f

        while (isActive) {
            val now = withFrameNanos { it }
            if (last == 0L) {
                last = now
                continue
            }
            val dt = ((now - last).toDouble() / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
            last = now

            acc += dt
            if (acc >= framePeriod) {
                acc -= framePeriod
                localFrame = (localFrame + 1) % animCount
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Image(
            painterResource(R.drawable.game_bg),
            null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // MONSTRU (mai mare)
        if (monsterFrames.isNotEmpty()) {
            val idx = (baseIndex + localFrame) % monsterFrames.size
            Image(
                bitmap = monsterFrames[idx],
                contentDescription = "Monster",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 40.dp)
                    .size(450.dp) // mai mare decât 400.dp
            )
        }

        // Foods (stânga)
        Column(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp)
        ) {
            viewModel.foods.forEach { food ->
                Image(
                    painter = painterResource(food.resId),
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .padding(10.dp)
                        .clickable { viewModel.onFed(food) }
                )
            }
        }

        Image(
            painterResource(R.drawable.ui_button_home),
            "Home",
            Modifier
                .padding(16.dp)
                .size(64.dp)
                .clickable { onHome() }
        )
    }
}
