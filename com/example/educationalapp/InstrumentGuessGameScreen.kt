package com.example.educationalapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/**
 * A simplified instrument guessing game.  In the original application this game
 * played a sound and asked the user to identify the instrument.  Without
 * access to audio playback in this environment we instead display an
 * instrument name and ask the player to pick the matching instrument from a
 * set of three.  Correct guesses award points and stars.
 */
data class Instrument(val name: String)

private const val TOTAL_INSTRUMENT_QUESTIONS = 8

/**
 * Generate a list of three instruments containing the correct one and two
 * distractors.  Defined at file scope to avoid name collisions with
 * other generateOptions functions.  The returned list is shuffled.
 */
private fun generateInstrumentOptions(correct: Instrument, pool: List<Instrument>): List<Instrument> {
    val opts = mutableSetOf<Instrument>()
    opts.add(correct)
    while (opts.size < 3) {
        val candidate = pool.random()
        if (candidate != correct) opts.add(candidate)
    }
    return opts.shuffled()
}

@Composable
fun InstrumentGuessGameScreen(navController: NavController, starState: MutableState<Int>) {
    val instruments = remember {
        listOf(
            Instrument("Pian"),
            Instrument("Chitară"),
            Instrument("Tobe"),
            Instrument("Vioară"),
            Instrument("Flaut"),
            Instrument("Saxofon"),
            Instrument("Trompetă"),
            Instrument("Xilofon")
        )
    }
    var questionIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var current by remember { mutableStateOf(instruments.random()) }
    var options by remember { mutableStateOf(generateInstrumentOptions(current, instruments)) }
    var showEndDialog by remember { mutableStateOf(false) }

    // Local helper removed.  Use generateInstrumentOptions() defined at file scope.

    fun nextQuestion(correct: Boolean) {
        if (correct) {
            score += 10
            starState.value += 1
        } else {
            score = (score - 5).coerceAtLeast(0)
        }
        if (questionIndex + 1 >= TOTAL_INSTRUMENT_QUESTIONS) {
            showEndDialog = true
        } else {
            questionIndex++
            current = instruments.random()
            options = generateInstrumentOptions(current, instruments)
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
                    current = instruments.random()
                    options = generateInstrumentOptions(current, instruments)
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
        Image(
            painter = painterResource(id = R.drawable.bg_game_instruments),
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
            Text(text = "Ghicește Instrumentul", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = questionIndex / TOTAL_INSTRUMENT_QUESTIONS.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Selectează instrumentul: ${current.name}", style = MaterialTheme.typography.displayMedium, color = Color.White, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            options.forEach { option ->
                Button(
                    onClick = { nextQuestion(option == current) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(option.name)
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