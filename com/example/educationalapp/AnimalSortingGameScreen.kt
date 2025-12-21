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
import androidx.compose.ui.unit.sp

/**
 * Represents an animal with a name, an emoji for display and a category
 * describing its habitat (land, water or air).  Used in the animal sorting
 * game where players categorise animals into their correct habitats.
 */

private const val TOTAL_ANIMAL_QUESTIONS = 10

/**
 * A simple animal sorting game.  The player is shown a random animal and must
 * select whether it lives on land, in the water or in the air.  Correct
 * answers award points and stars; incorrect answers deduct points.  After a
 * fixed number of questions the game ends.
 */
@Composable
fun AnimalSortingGameScreen(navController: NavController, starState: MutableState<Int>) {
    val animals = remember {
        listOf(
            Animal("Leu", "🦁", "Pământ"),
            Animal("Pește", "🐟", "Apă"),
            Animal("Vultur", "🦅", "Aer"),
            Animal("Căprioară", "🦌", "Pământ"),
            Animal("Delfin", "🐬", "Apă"),
            Animal("Bufniță", "🦉", "Aer"),
            Animal("Cangur", "🦘", "Pământ"),
            Animal("Rechin", "🦈", "Apă"),
            Animal("Liliac", "🦇", "Aer")
        )
    }
    val categories = listOf("Pământ", "Apă", "Aer")
    var questionIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var current by remember { mutableStateOf(animals.random()) }
    var showEndDialog by remember { mutableStateOf(false) }

    fun nextQuestion(correct: Boolean) {
        if (correct) {
            score += 10
            starState.value += 1
        } else {
            score = (score - 5).coerceAtLeast(0)
        }
        if (questionIndex + 1 >= TOTAL_ANIMAL_QUESTIONS) {
            showEndDialog = true
        } else {
            questionIndex++
            current = animals.random()
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
                    current = animals.random()
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
            painter = painterResource(id = R.drawable.bg_game_sorting),
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
            Text(text = "Sortare Animale", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = questionIndex / TOTAL_ANIMAL_QUESTIONS.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = current.emoji, fontSize = 64.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = current.name, style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))
            categories.forEach { category ->
                Button(
                    onClick = { nextQuestion(category == current.category) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(category)
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
