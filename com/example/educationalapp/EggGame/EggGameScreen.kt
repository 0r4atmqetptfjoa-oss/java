package com.example.educationalapp.EggGame

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.educationalapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EggGameScreen(
    viewModel: EggGameViewModel = hiltViewModel(),
    onHome: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var assets by remember { mutableStateOf<EggLoadedAssets?>(null) }
    var loaded by remember { mutableStateOf(false) }

    val vfx = remember { EggVfxState() }

    val eggScale = remember { Animatable(1f) }
    var eggCenter by remember { mutableStateOf(Offset(540f, 960f)) } // fallback, actualizat din layout mai jos

    // Load assets
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val a = EggAssets.load(context)
            withContext(Dispatchers.Main) {
                assets = a
                loaded = true
            }
        }
    }

    // VFX: spawn on crack events
    val crackEvent = viewModel.crackEvent
    LaunchedEffect(crackEvent) {
        if (crackEvent <= 0) return@LaunchedEffect
        val level = viewModel.crackLevel
        if (level <= 2) EggVfx.spawnCrack(vfx, eggCenter, intensity = level)
        else EggVfx.spawnBurst(vfx, eggCenter)
    }

    // VFX update loop
    LaunchedEffect(Unit) {
        var last = 0L
        while (isActive) {
            val now = withFrameNanos { it }
            if (last == 0L) { last = now; continue }
            val dt = ((now - last).toDouble() / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
            last = now
            EggVfx.update(vfx, dt)
        }
    }

    // Dragon animation loop
    var dragonLocalFrame by remember { mutableIntStateOf(0) }
    LaunchedEffect(loaded, viewModel.eggState, viewModel.dragonAnim) {
        val a = assets ?: return@LaunchedEffect
        if (!loaded || viewModel.eggState != EggState.DRAGON || a.dragonFrames.isEmpty()) {
            dragonLocalFrame = 0
            return@LaunchedEffect
        }

        val spec = a.dragonSpec
        dragonLocalFrame = 0
        var last = 0L
        var acc = 0f

        while (isActive && viewModel.eggState == EggState.DRAGON) {
            val now = withFrameNanos { it }
            if (last == 0L) { last = now; continue }
            val dt = ((now - last).toDouble() / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
            last = now

            val hop = viewModel.dragonAnim == DragonAnim.HOP
            val range = if (hop) spec.hop else spec.idle
            val period = if (hop) spec.hopFramePeriodSec else spec.idleFramePeriodSec

            acc += dt
            if (acc >= period) {
                acc -= period
                dragonLocalFrame++

                // dacă am terminat un ciclu de hop, revino la idle
                if (hop && (dragonLocalFrame % range.count == 0)) {
                    viewModel.onHopFinished()
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_magic_forest),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Home button
        Image(
            painter = painterResource(id = R.drawable.ui_button_home),
            contentDescription = "Home",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(64.dp)
                .clickable { onHome() }
        )

        // VFX layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            EggVfx.draw(vfx, this, assets?.shellPiece)
        }

        // Egg / Dragon
        val tapModifier = Modifier
            .align(Alignment.Center)
            .size(320.dp)
            .scale(eggScale.value)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                viewModel.onEggTap()
                scope.launch {
                    eggScale.animateTo(1.10f, tween(120))
                    eggScale.animateTo(1.00f, tween(140))
                }
            }

        // Actualizează eggCenter aproximativ (pentru VFX)
        LaunchedEffect(Unit) {
            // aproximare stabilă pentru majoritatea ecranelor; dacă vrei exact,
            // putem calcula din LayoutCoordinates, dar așa e suficient pentru VFX.
            eggCenter = Offset(540f, 960f)
        }

        when (viewModel.eggState) {
            EggState.INTACT -> Image(painterResource(R.drawable.egg_0_intact), "Egg", tapModifier)
            EggState.CRACK1 -> Image(painterResource(R.drawable.egg_1_crack), "Egg", tapModifier)
            EggState.CRACK2 -> Image(painterResource(R.drawable.egg_2_crack), "Egg", tapModifier)
            EggState.BROKEN -> Image(painterResource(R.drawable.egg_3_broken), "Egg", tapModifier)
            EggState.DRAGON -> {
                val a = assets
                if (a != null && a.dragonFrames.isNotEmpty()) {
                    val spec = a.dragonSpec
                    val hop = viewModel.dragonAnim == DragonAnim.HOP
                    val range = if (hop) spec.hop else spec.idle
                    val idx = range.index(dragonLocalFrame).coerceIn(0, a.dragonFrames.lastIndex)

                    Image(
                        bitmap = a.dragonFrames[idx],
                        contentDescription = "Dragon",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(380.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { viewModel.onDragonTap() }
                    )
                }
            }
        }
    }
}
