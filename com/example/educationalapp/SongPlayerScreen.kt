package com.example.educationalapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

/**
 * Screen for playing a selected song.  Uses MediaPlayer to play an audio
 * resource stored in res/raw.  When the song completes, the player awards
 * one star.
 *
 * @param songId Index of the song selected in the SongsMenuScreen.
 */
@Composable
fun SongPlayerScreen(
    navController: NavController, 
    starState: MutableState<Int>, 
    songId: Int,
    viewModel: SongPlayerViewModel = hiltViewModel()
) {
    val title = viewModel.songTitles.getOrElse(songId) { "Melodie" }
    val isPlaying = viewModel.isPlaying

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = title, modifier = Modifier.padding(bottom = 16.dp))
        Row(modifier = Modifier.padding(bottom = 16.dp)) {
            Button(onClick = { viewModel.onPlayPauseClick(songId) { starState.value++ } }) {
                Text(text = if (isPlaying) "Pauză" else "Redă")
            }
        }
        Button(onClick = { navController.navigate(Screen.SongsMenu.route) }) {
            Text(text = "Înapoi la Melodii")
        }
    }
}