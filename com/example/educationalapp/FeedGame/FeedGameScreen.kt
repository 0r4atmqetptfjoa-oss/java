package com.example.educationalapp.FeedGame

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
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
import androidx.compose.runtime.withFrameNanos

@Composable
fun FeedGameScreen(
    viewModel: FeedGameViewModel = hiltViewModel(),
    onHome: () -> Unit
) {
    val context = LocalContext.current
    var monsterFrames by remember { mutableStateOf(listOf<androidx.compose.ui.graphics.ImageBitmap>()) }
    var loaded by remember { mutableStateOf(false) }

    // --- FIX CRITIC: inScaled = false ---
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val opts = BitmapFactory.Options().apply { inScaled = false }
            val sheet = BitmapFactory.decodeResource(context.resources, R.drawable.monster_sheet, opts)
            val frames = splitSpriteSheet(sheet, 4, 6).map { it.asImageBitmap() }
            withContext(Dispatchers.Main) { 
                monsterFrames = frames 
                loaded = true
            }
        }
    }
    
    // Animatie Monstru
    var frameIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(loaded, viewModel.monsterState) {
        var acc = 0f
        while(isActive && loaded) {
            val dt = 0.016f
            withFrameNanos { }
            acc += dt
            if(acc >= 0.12f) {
                acc = 0f
                frameIndex++
            }
        }
    }
    
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.game_bg), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        
        // --- MONSTRU MARE (400.dp) ---
        if (monsterFrames.isNotEmpty()) {
            val currentFrame = if (viewModel.monsterState == "EATING") {
                (frameIndex % 12) + 12 // Randul 2 (Eating)
            } else {
                frameIndex % 12 // Randul 1 (Idle)
            }
            
            Image(
                bitmap = monsterFrames[currentFrame % monsterFrames.size], 
                contentDescription = "Monster", 
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 50.dp)
                    .size(400.dp) // L-am facut MARE
            )
        }
        
        // Foods Row (Left)
        Column(Modifier.align(Alignment.CenterStart).padding(start = 20.dp)) {
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
        
        Image(painterResource(R.drawable.ui_button_home), "Home", Modifier.padding(16.dp).size(64.dp).clickable { onHome() })
    }
}