package com.example.educationalapp

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

/**
 * A simple sound board that plays animal sounds when tapped.  Children receive
 * a star after playing a number of different sounds.  Audio files must be
 * placed in res/raw with names matching the soundResId provided (e.g.
 * R.raw.sound_cat).
 */
data class SoundItem(val name: String, val emoji: String, val soundResId: Int)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimalSoundBoardScreen(
    navController: NavController, 
    starState: MutableState<Int>,
    viewModel: AnimalSoundBoardViewModel = hiltViewModel()
) {
    val feedback = viewModel.feedback.value

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Panou Sunete Animale", modifier = Modifier.padding(bottom = 16.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f)) {
            items(viewModel.sounds) { item ->
                Card(modifier = Modifier.padding(8.dp).fillMaxWidth().aspectRatio(1f)) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(onClick = { viewModel.onSoundItemClick(item, starState) }) {
                            Text(text = "${item.emoji}\n${item.name}")
                        }
                    }
                }
            }
        }
        if (feedback.isNotEmpty()) {
            Text(text = feedback, modifier = Modifier.padding(bottom = 8.dp))
        }
        Button(onClick = { navController.navigate(Screen.MainMenu.route) }) {
            Text(text = "Înapoi la Meniu")
        }
    }
}