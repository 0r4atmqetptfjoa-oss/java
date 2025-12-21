package com.example.educationalapp.FeedGame

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

    var assets by remember { mutableStateOf<FeedLoadedAssets?>(null) }
    var loaded by remember { mutableStateOf(false) }

    // VFX
    val vfx = remember { FeedVfxState() }
    var monsterAnchor by remember { mutableStateOf(Offset.Zero) }

    // Load assets
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val a = FeedAssets.load(context)
            withContext(Dispatchers.Main) {
                assets = a
                loaded = true
            }
        }
    }

    // VFX spawn on feedEvent
    val feedEvent = viewModel.feedEvent
    LaunchedEffect(feedEvent) {
        if (feedEvent <= 0) return@LaunchedEffect
        if (viewModel.lastFeedCorrect) {
            FeedVfx.spawnStars(vfx, monsterAnchor, count = 8)
        }
    }

    // VFX loop
    LaunchedEffect(Unit) {
        var last = 0L
        while (isActive) {
            val now = withFrameNanos { it }
            if (last == 0L) { last = now; continue }
            val dt = ((now - last).toDouble() / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
            last = now
            FeedVfx.update(vfx, dt)
        }
    }

    // Monster anim loop
    var localFrame by remember { mutableIntStateOf(0) }
    LaunchedEffect(loaded, viewModel.monsterState) {
        val a = assets ?: return@LaunchedEffect
        if (!loaded || a.monsterFrames.isEmpty()) {
            localFrame = 0
            return@LaunchedEffect
        }

        localFrame = 0
        var last = 0L
        var acc = 0f

        while (isActive) {
            val now = withFrameNanos { it }
            if (last == 0L) { last = now; continue }
            val dt = ((now - last).toDouble() / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
            last = now

            val eating = viewModel.monsterState == MonsterState.EATING
            val spec = a.monsterSpec
            val range = if (eating) spec.eat else spec.idle
            val period = if (eating) spec.eatFramePeriodSec else spec.idleFramePeriodSec

            acc += dt
            if (acc >= period) {
                acc -= period
                localFrame = (localFrame + 1) % (range.count.coerceAtLeast(1))
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.game_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Image(
            painter = painterResource(id = R.drawable.ui_button_home),
            contentDescription = "Home",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(64.dp)
                .clickable { onHome() }
        )

        // Wanted food (sus dreapta)
        val wanted = viewModel.wantedFood
        if (wanted != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.vfx_star),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.height(6.dp))
                Image(
                    painter = painterResource(id = wanted.resId),
                    contentDescription = wanted.name,
                    modifier = Modifier.size(86.dp)
                )
            }
        }

        // Monster (dreapta, mare)
        val a = assets
        if (a != null && a.monsterFrames.isNotEmpty()) {
            val eating = viewModel.monsterState == MonsterState.EATING
            val range = if (eating) a.monsterSpec.eat else a.monsterSpec.idle
            val idx = range.index(localFrame).coerceIn(0, a.monsterFrames.lastIndex)

            Image(
                bitmap = a.monsterFrames[idx],
                contentDescription = "Monster",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 28.dp)
                    .size(470.dp)
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        val size = coords.size
                        // aproximăm "gura" pentru VFX
                        monsterAnchor = Offset(pos.x + size.width * 0.55f, pos.y + size.height * 0.45f)
                    }
            )
        }

        // Foods (jos stanga)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, bottom = 18.dp)
        ) {
            viewModel.foods.forEach { food ->
                Image(
                    painter = painterResource(id = food.resId),
                    contentDescription = food.name,
                    modifier = Modifier
                        .size(96.dp)
                        .padding(8.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { viewModel.onFed(food) }
                )
            }
        }
    }

    // VFX layer on top (separat ca sa fie mereu deasupra)
    if (assets?.starImage != null) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            FeedVfx.draw(vfx, this, assets?.starImage)
        }
    }
}
