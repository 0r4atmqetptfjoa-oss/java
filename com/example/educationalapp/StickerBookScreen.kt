package com.example.educationalapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

/**
 * Displays a grid of stickers which unlock as the player earns more stars.
 * Each sticker requires a certain number of stars to unlock.  Unlocked
 * stickers are colourful; locked stickers are greyed out with a lock icon.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StickerBookScreen(
    navController: NavController, 
    starState: MutableState<Int>,
    viewModel: StickerBookViewModel = hiltViewModel()
) {
    val feedback by viewModel.feedback

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Album de Stickere", modifier = Modifier.padding(bottom = 16.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f)) {
            items(viewModel.stickers) { sticker ->
                val unlocked = starState.value >= sticker.requiredStars
                Card(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clickable { viewModel.onStickerClick(sticker, starState.value) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (unlocked) Color(0xFFFFF9C4) else Color(0xFFE0E0E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unlocked) sticker.emoji else "🔒",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
        if (feedback.isNotEmpty()) {
            Text(text = feedback, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate(Screen.MainMenu.route) }) {
            Text(text = "Înapoi la Meniu")
        }
    }
}