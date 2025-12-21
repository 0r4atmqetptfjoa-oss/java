package com.example.educationalapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Square
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

private const val TOTAL_SHAPE_QUESTIONS = 10

/**
 * Generate three shape options containing the correct shape and two
 * distractors.  This helper is defined at file scope to avoid collisions
 * with similarly named functions in other screens.  The result is
 * shuffled to randomise order.
 */
private fun generateShapeOptions(correct: NamedShape, pool: List<NamedShape>): List<NamedShape> {
    val opts = mutableSetOf<NamedShape>()
    opts.add(correct)
    while (opts.size < 3) {
        val candidate = pool.random()
        if (candidate != correct) opts.add(candidate)
    }
    return opts.shuffled()
}

/**
 * A simplified shape matching game. The player sees the name of a shape and
 * must pick the correct icon from three options. Correct answers award
 * points and a star; incorrect answers deduct points. After a fixed number
 * of questions the game ends with a summary dialog.
 */
@Composable
fun ShapeMatchScreen(navController: NavController, starState: MutableState<Int>) {
    val shapes = remember {
        listOf(
            NamedShape("Cerc", Icons.Default.Circle, Color(0xFF42A5F5)),
            NamedShape("Pătrat", Icons.Default.Square, Color(0xFF66BB6A)),
            NamedShape("Triunghi", Icons.Default.ChangeHistory, Color(0xFFEF5350))
        )
    }
    var questionIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var current by remember { mutableStateOf(shapes.random()) }
    var options by remember { mutableStateOf(generateShapeOptions(current, shapes)) }
    var showEndDialog by remember { mutableStateOf(false) }

    // Local helper removed.  Use generateShapeOptions() defined at file scope.

    fun nextQuestion(correct: Boolean) {
        if (correct) {
            score += 10
            starState.value += 1
        } else {
            score = (score - 5).coerceAtLeast(0)
        }
        if (questionIndex + 1 >= TOTAL_SHAPE_QUESTIONS) {
            showEndDialog = true
        } else {
            questionIndex++
            current = shapes.random()
            options = generateShapeOptions(current, shapes)
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
                    current = shapes.random()
                    options = generateShapeOptions(current, shapes)
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
            painter = painterResource(id = R.drawable.bg_game_shapes),
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
                text = "Potrivire Forme",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = questionIndex / TOTAL_SHAPE_QUESTIONS.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Atinge forma: ${current.name}",
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
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = option.name,
                            tint = option.color,
                            modifier = Modifier.size(40.dp)
                        )
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
