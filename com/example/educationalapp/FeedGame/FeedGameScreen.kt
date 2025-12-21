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
import kotlinx.coroutines.withContext

@Composable
fun FeedGameScreen(
    viewModel: FeedGameViewModel = hiltViewModel(),
    onHome: () -> Unit
) {
    val context = LocalContext.current
    var monsterFrames by remember { mutableStateOf(listOf<androidx.compose.ui.graphics.ImageBitmap>()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val opts = BitmapFactory.Options().apply { inScaled = false }
            val sheet = BitmapFactory.decodeResource(context.resources, R.drawable.monster_sheet, opts)
            val frames = splitSpriteSheet(sheet, 4, 6).map { it.asImageBitmap() }
            withContext(Dispatchers.Main) { monsterFrames = frames }
        }
    }
    
    // Drag and Drop Logic UI ...
    // Aici vei folosi codul tău existent pentru DraggableFoodIcon, dar vei apela viewModel.onFed(food)
    
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.game_bg), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        
        // Monster
        if (monsterFrames.isNotEmpty()) {
            Image(monsterFrames[0], "Monster", Modifier.align(Alignment.CenterEnd).size(300.dp))
        }
        
        // Foods Row (Left)
        Column(Modifier.align(Alignment.CenterStart)) {
            viewModel.foods.forEach { food ->
                // DraggableFoodIcon(food, onDrop = { viewModel.onFed(food) })
                Image(painterResource(food.resId), null, Modifier.size(80.dp).clickable { viewModel.onFed(food) })
            }
        }
        
        // Home
        Image(painterResource(R.drawable.ui_button_home), "Home", Modifier.padding(16.dp).size(64.dp).clickable { onHome() })
    }
}