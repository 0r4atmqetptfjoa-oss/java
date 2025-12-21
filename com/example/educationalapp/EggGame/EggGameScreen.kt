package com.example.educationalapp.EggGame

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.withFrameNanos

@Composable
fun EggGameScreen(
    viewModel: EggGameViewModel = hiltViewModel(),
    onHome: () -> Unit
) {
    val context = LocalContext.current
    var dragonFrames by remember { mutableStateOf(listOf<androidx.compose.ui.graphics.ImageBitmap>()) }
    var loaded by remember { mutableStateOf(false) }
    val eggScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    // --- FIX CRITIC: inScaled = false ---
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val opts = BitmapFactory.Options().apply { inScaled = false }
            val sheet = BitmapFactory.decodeResource(context.resources, R.drawable.dragon_sheet, opts)
            // Folosim functia din AnimalBandGame sau o duplicam aici daca e private
            val frames = splitSpriteSheet(sheet, 4, 6).map { it.asImageBitmap() }
            withContext(Dispatchers.Main) {
                dragonFrames = frames
                loaded = true
            }
        }
    }
    
    // --- FIX VITEZA DRAGON ---
    var dragonFrame by remember { mutableIntStateOf(0) }
    LaunchedEffect(loaded) {
        var acc = 0f
        while(isActive && loaded) {
            val dt = 0.016f
            withFrameNanos { }
            acc += dt
            if (acc >= 0.12f) { // 8 FPS - Incetinit
                acc = 0f
                dragonFrame++
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.bg_magic_forest), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())

        if (viewModel.gameState != EggState.DRAGON) {
            val res = when(viewModel.gameState) {
                EggState.INTACT -> R.drawable.egg_0_intact
                EggState.CRACK1 -> R.drawable.egg_1_crack
                EggState.CRACK2 -> R.drawable.egg_2_crack
                EggState.BROKEN -> R.drawable.egg_3_broken
                else -> R.drawable.egg_0_intact
            }
            Image(
                painter = painterResource(res),
                contentDescription = "Egg",
                modifier = Modifier
                    .size(300.dp)
                    .scale(eggScale.value)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        viewModel.onTapEgg(System.nanoTime())
                        scope.launch { 
                            eggScale.animateTo(1.1f, tween(100))
                            eggScale.animateTo(1f, tween(100))
                        }
                    }
            )
        } else {
            if (dragonFrames.isNotEmpty()) {
                // Afisam dragonul care iese din ou
                val frameIndex = if (viewModel.dragonMode == DragonAnim.RISE) {
                    (dragonFrame % 12) + 12 // Randul 2
                } else {
                    dragonFrame % 12 // Randul 1 (Idle)
                }
                
                Image(
                    bitmap = dragonFrames[frameIndex % dragonFrames.size], 
                    contentDescription = "Dragon", 
                    modifier = Modifier
                        .size(380.dp) // Marime generoasa
                        .clickable { viewModel.onTapEgg(0) }
                )
            }
        }
        
         Image(
            painter = painterResource(id = R.drawable.ui_button_home),
            contentDescription = "Home",
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp).size(64.dp).clickable { onHome() }
        )
    }
}