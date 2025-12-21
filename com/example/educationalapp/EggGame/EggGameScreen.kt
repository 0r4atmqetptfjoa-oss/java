package com.example.educationalapp.EggGame

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
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
import kotlinx.coroutines.withFrameNanos

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

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val opts = BitmapFactory.Options().apply { inScaled = false }
            val sheet = BitmapFactory.decodeResource(context.resources, R.drawable.dragon_sheet, opts)
            val frames = splitSpriteSheet(sheet, 4, 6).map { it.asImageBitmap() }
            withContext(Dispatchers.Main) {
                dragonFrames = frames
                loaded = true
            }
        }
    }
    
    var dragonFrame by remember { mutableIntStateOf(0) }
    LaunchedEffect(loaded) {
        while(isActive && loaded) {
            withFrameNanos { }
            // Animatie simpla
            dragonFrame++
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
                            eggScale.animateTo(1.1f)
                            eggScale.animateTo(1f)
                        }
                    }
            )
        } else {
            if (dragonFrames.isNotEmpty()) {
                Image(dragonFrames[dragonFrame % dragonFrames.size], "Dragon", 
                    modifier = Modifier.size(350.dp).clickable { viewModel.onTapEgg(0) })
            }
        }
        
         Image(
            painter = painterResource(id = R.drawable.ui_button_home),
            contentDescription = "Home",
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp).size(64.dp).clickable { onHome() }
        )
    }
}