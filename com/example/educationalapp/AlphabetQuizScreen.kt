package com.example.educationalapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

/**
 * A simple alphabet quiz game. The player is presented with a target letter and
 * must select the matching letter from three options. Correct selections
 * increase the score and award stars. After ten questions the game ends and
 * displays the final score with options to restart or return to the menu.
 */
private const val TOTAL_ALPHABET_QUESTIONS = 10

/**
 * Generate a list of three unique letters containing the correct letter and
 * two random distractors.  The options are shuffled to randomise their
 * positions.  Defined at top level to avoid name clashes with other
 * generateOptions functions in the project.
 */
private fun generateLetterOptions(correct: Char, pool: List<Char>): List<Char> {
    // Build a set containing the correct letter and two distinct distractors.
    val opts = mutableSetOf<Char>()
    opts.add(correct)
    while (opts.size < 3) {
        val candidate = pool.random()
        if (candidate != correct) opts.add(candidate)
    }
    // Shuffle the options so the correct answer isn't always in the same position
    return opts.shuffled()
}

@Composable
fun AlphabetQuizScreen(navController: NavController, starState: MutableState<Int>) {
    // List of uppercase letters for questions
    val alphabet = remember { ('A'..'Z').toList() }
    var questionIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var currentLetter by remember { mutableStateOf(alphabet.random()) }
    var options by remember { mutableStateOf(generateLetterOptions(currentLetter, alphabet)) }
    var showEndDialog by remember { mutableStateOf(false) }

    // Local helper removed: use the top‑level generateLetterOptions() to avoid name clashes

    fun nextQuestion(correct: Boolean) {
        if (correct) {
            score += 10
            starState.value += 1
        } else {
            score = (score - 5).coerceAtLeast(0)
        }
        if (questionIndex + 1 >= TOTAL_ALPHABET_QUESTIONS) {
            showEndDialog = true
        } else {
            questionIndex++
            currentLetter = alphabet.random()
            options = generateLetterOptions(currentLetter, alphabet)
        }
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { navController.navigate(Screen.MainMenu.route) },
            title = { Text("Joc Terminat!", textAlign = TextAlign.Center) },
            text = { Text("Felicitări! Scorul tău este $score.", textAlign = TextAlign.Center) },
            confirmButton = {
                Button(onClick = {
                    questionIndex = 0
                    score = 0
                    currentLetter = alphabet.random()
                    options = generateLetterOptions(currentLetter, alphabet)
                    showEndDialog = false
                }) {
                    Text("Joacă din nou")
                }
            },
            dismissButton = {
                Button(onClick = { navController.navigate(Screen.MainMenu.route) }) {
                    Text("Meniu Principal")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Use the alphabet quiz background.
        Image(
            painter = painterResource(id = R.drawable.bg_game_alphabet),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Quiz Alfabet",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = questionIndex / TOTAL_ALPHABET_QUESTIONS.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Selectează litera: $currentLetter",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                options.forEach { option ->
                    Button(onClick = { nextQuestion(option == currentLetter) }) {
                        Text(option.toString(), fontSize = 32.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Scor: $score", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.navigate(Screen.MainMenu.route) }) {
                Text("Înapoi la Meniu")
            }
        }
    }
}
