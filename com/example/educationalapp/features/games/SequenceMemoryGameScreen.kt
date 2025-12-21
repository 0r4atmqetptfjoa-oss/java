package com.example.educationalapp.features.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

/**
 * A sequence memory game inspired by Simon Says.  The game flashes a sequence of
 * coloured squares which the player must then reproduce by tapping the same
 * coloured buttons in order.  Each successful round increases the sequence
 * length and awards a star.
 */
@Composable
fun SequenceMemoryGameScreen(
    navController: NavController,
    starState: MutableState<Int>
) {
    // Available colours for the sequence
    val colours = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow)
    // Current sequence of colour indices
    val sequence = remember { mutableStateListOf<Int>() }
    // Index of the next colour the user must tap
    val userIndex = remember { mutableStateOf(0) }
    // Flag to indicate whether the sequence is currently being shown (disables input)
    val showingSequence = remember { mutableStateOf(false) }
    // Index of the colour currently highlighted while showing the sequence
    val highlightIndex = remember { mutableStateOf<Int?>(null) }
    // Message displayed to the user (success/failure/instructions)
    val message = remember { mutableStateOf("Apasă Start pentru a începe!") }

    // Launch sequence display when showingSequence becomes true
    LaunchedEffect(showingSequence.value) {
        if (showingSequence.value) {
            // Wait a moment before showing the sequence
            delay(500)
            for (idx in sequence) {
                highlightIndex.value = idx
                delay(600)
                highlightIndex.value = null
                delay(200)
            }
            showingSequence.value = false
            userIndex.value = 0
            message.value = "Repetă secvența!"
        }
    }

    // Helper to start a new round
    fun startNewRound() {
        // Add a random colour to the sequence
        sequence.add((0 until colours.size).random())
        showingSequence.value = true
        message.value = "Privește secvența!"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Memorie Secvențe", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Text(text = message.value, color = Color.White)

        // Four coloured buttons for user input
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colours.forEachIndexed { index, colour ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .border(2.dp, Color.White)
                        .background(
                            when {
                                highlightIndex.value == index -> colour.copy(alpha = 0.5f)
                                else -> colour
                            }
                        )
                        .clickable(enabled = !showingSequence.value) {
                            // Handle user tapping a colour
                            if (!showingSequence.value && sequence.isNotEmpty()) {
                                if (sequence[userIndex.value] == index) {
                                    userIndex.value++
                                    // Completed sequence
                                    if (userIndex.value == sequence.size) {
                                        starState.value = starState.value + 1
                                        message.value = "Bravo! Secvență corectă."
                                        startNewRound()
                                    }
                                } else {
                                    // Wrong colour
                                    message.value = "Greșit! Secvența se resetează."
                                    sequence.clear()
                                    userIndex.value = 0
                                }
                            }
                        }
                )
            }
        }

        // Controls: Start/Reset and Back
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = {
                // If no sequence is in progress, start a new round
                if (!showingSequence.value) {
                    if (sequence.isEmpty()) {
                        startNewRound()
                    } else {
                        // Reset game if sequence exists
                        sequence.clear()
                        message.value = "Joc resetat. Apasă Start pentru a începe."
                    }
                }
            }) {
                Text(text = if (sequence.isEmpty()) "Start" else "Reset")
            }
            Button(onClick = { navController.popBackStack() }) {
                Text(text = "Înapoi")
            }
        }
    }
}