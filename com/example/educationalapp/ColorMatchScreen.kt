package com.example.educationalapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
 * Represents a colour with a displayable name and an associated Compose [Color].
 */
data class NamedColour(val name: String, val color: Color)

private const val TOTAL_COLOUR_QUESTIONS = 10

/**
 * Generate three distinct colour options containing the correct colour and
 * two distractors drawn from the provided pool.  This helper is defined
 * outside the composable to avoid accidental reference to private functions
 * in other files (e.g. MathGameScreen.generateOptions()).  The returned
 * list is shuffled to randomise the order of the options.
 */
private fun generateColourOptions(correct: NamedColour, pool: List<NamedColour>): List<NamedColour> {
    val opts = mutableSetOf<NamedColour>()
    opts.add(correct)
    while (opts.size < 3) {
        val candidate = pool.random()
        if (candidate != correct) opts.add(candidate)
    }
    return opts.shuffled()
}

/**
 * A simplified colour matching game. The player is shown the name of a colour and
 * must choose the correct colour swatch from three options. Correct answers
 * yield points and a star; incorrect answers deduct points. After a fixed
 * number of questions the game ends.
 */
@Composable
fun ColorMatchScreen(navController: NavController, starState: MutableState<Int>) {
    val colours = remember {
        listOf(
            NamedColour("Roșu", Color.Red),
            NamedColour("Verde", Color.Green),
            NamedColour("Albastru", Color.Blue),
            NamedColour("Galben", Color.Yellow),
            NamedColour("Mov", Color.Magenta),
            NamedColour("Portocaliu", Color(0xFFFF9800)),
            NamedColour("Turcoaz", Color(0xFF00BCD4))
        )
    }
    var questionIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var current by remember { mutableStateOf(colours.random()) }
    var options by remember { mutableStateOf(generateColourOptions(current, colours)) }
    var showEndDialog by remember { mutableStateOf(false) }

    // Local helper removed.  The generateColourOptions() function above is used instead.

    fun nextQuestion(correct: Boolean) {
        if (correct) {
            score += 10
            starState.value += 1
        } else {
            score = (score - 5).coerceAtLeast(0)
        }
        if (questionIndex + 1 >= TOTAL_COLOUR_QUESTIONS) {
            showEndDialog = true
        } else {
            questionIndex++
            current = colours.random()
            options = generateColourOptions(current, colours)
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
                    current = colours.random()
                    options = generateColourOptions(current, colours)
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
            painter = painterResource(id = R.drawable.bg_game_colors),
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
                text = "Potrivire Culori",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = questionIndex / TOTAL_COLOUR_QUESTIONS.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Atinge culoarea: ${current.name}",
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
                    Button(
                        onClick = { nextQuestion(option == current) },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = option.color),
                        modifier = Modifier.size(80.dp)
                    ) {
                        // empty content; the button colour itself conveys the answer
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