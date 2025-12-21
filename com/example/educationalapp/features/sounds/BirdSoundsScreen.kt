package com.example.educationalapp.features.sounds

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.educationalapp.ui.components.SpriteAnimation
import com.example.educationalapp.ui.components.rememberAssetSheet
import com.example.educationalapp.ui.media.rememberSoundPlayer
import com.example.educationalapp.utils.toSafeFileName

@Composable
fun BirdSoundsScreen() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 128.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(birdSprites) { bird ->
            var isPlaying by remember { mutableStateOf(false) }
            val sheet = rememberAssetSheet(path = bird.assetPath)

            val soundFileName = bird.name.toSafeFileName()
            val soundUri = remember(soundFileName) {
                // Assuming sounds are in assets/sounds/pasari
                Uri.parse("asset:///sounds/pasari/$soundFileName.mp3")
            }

            val soundPlayer = rememberSoundPlayer(soundUri = soundUri) {
                isPlaying = false // Stop animation when sound ends
            }

            Card(
                modifier = Modifier.clickable {
                    if (!isPlaying) {
                        soundPlayer.seekTo(0)
                        soundPlayer.play()
                        isPlaying = true
                    } else {
                        soundPlayer.seekTo(0)
                        soundPlayer.play()
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    if (sheet != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            SpriteAnimation(
                                sheet = sheet,
                                frameCount = bird.frameCount,
                                columns = bird.columns,
                                fps = bird.fps,
                                isPlaying = isPlaying,
                                modifier = Modifier.fillMaxSize(0.9f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = bird.name)
                }
            }
        }
    }
}
