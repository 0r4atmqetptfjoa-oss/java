package com.example.educationalapp.features.songs

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.example.educationalapp.R

/**
 * A simple player screen for songs.  This screen displays the song name and a play/pause button.
 * When the play button is pressed for the first time, a star is awarded via [starState].  A
 * placeholder image is shown as album art.  The caller should navigate to this screen with
 * arguments containing the song name under the key "songName".  In a real implementation you
 * could also pass a resource ID or file path for the audio to be played.
 */
@Composable
fun SongPlayerScreen(
    navController: NavController,
    backStackEntry: NavBackStackEntry,
    starState: MutableState<Int>
) {
    // Retrieve the song name from the navigation arguments.  Fallback to a default value if none
    // was provided.
    val songName = backStackEntry.arguments?.getString("songName") ?: stringResource(id = R.string.song_default_title)

    // Local state to track whether playback is active.  Initially not playing.
    val isPlaying = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Back navigation
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Înapoi",
                modifier = Modifier
                    .size(32.dp)
                    .clickable { navController.popBackStack() },
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.song_player_title),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Placeholder album art image
        Spacer(modifier = Modifier.height(200.dp))

        // Song title
        Text(
            text = songName,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        // Play/Pause button
        IconButton(onClick = {
            isPlaying.value = !isPlaying.value
            // Award a star only when playback starts for the first time
            if (isPlaying.value) {
                starState.value = starState.value + 1
            }
        }) {
            Icon(
                imageVector = if (isPlaying.value) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                contentDescription = if (isPlaying.value) "Pauză" else "Redare",
                tint = Color.White,
                modifier = Modifier.size(96.dp)
            )
        }

        // Playback status text
        Text(
            text = if (isPlaying.value) stringResource(id = R.string.song_playing_label) else stringResource(id = R.string.song_paused_label),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
    }
}

// Preview is omitted because NavController requires a valid context.