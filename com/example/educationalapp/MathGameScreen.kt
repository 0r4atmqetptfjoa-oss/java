package com.example.educationalapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlin.random.Random

private const val TOTAL_MATH_QUESTIONS = 10

/**
 * Represents a simple arithmetic question consisting of two operands and a
 * plus or minus operator.  The question text is human‑readable and the answer
 * is computed when the question is generated.
 */
data class MathQuestion(val text: String, val answer: Int)

/**
 * A basic math quiz game.  The player is presented with a series of addition
 * and subtraction problems.  For each question three answer options are
 * generated; selecting the correct one yields points and a star.  After
 * completing [TOTAL_MATH_QUESTIONS] questions the game ends and displays the
 * total score with options to restart or return to the main menu.
 */
@Composable
fun MathGameScreen(navController: NavController, starState: MutableState<Int>) {
    var questionIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var question by remember { mutableStateOf(generateMathQuestion()) }
    var options by remember { mutableStateOf(generateOptions(question.answer)) }
    var showEndDialog by remember { mutableStateOf(false) }

    fun nextQuestion(correct: Boolean) {
        // Update score and stars based on correctness
        if (correct) {
            score += 10
            starState.value += 1
        } else {
            score = (score - 5).coerceAtLeast(0)
        }
        // Advance to next question or finish
        if (questionIndex + 1 >= TOTAL_MATH_QUESTIONS) {
            showEndDialog = true
        } else {
            questionIndex++
            question = generateMathQuestion()
            options = generateOptions(question.answer)
        }
    }

    // Show final dialog when the quiz is over
    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { navController.navigate(Screen.MainMenu.route) },
            title = { Text("Joc Terminat!", textAlign = TextAlign.Center) },
            text = { Text("Felicitări! Scorul tău este $score.", textAlign = TextAlign.Center) },
            confirmButton = {
                Button(onClick = {
                    // Restart game
                    questionIndex = 0
                    score = 0
                    question = generateMathQuestion()
                    options = generateOptions(question.answer)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Joc Matematică",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Progress indicator shows how many questions have been completed
        LinearProgressIndicator(
            progress = questionIndex / TOTAL_MATH_QUESTIONS.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = question.text,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        // Display answer options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            options.forEach { option ->
                Button(onClick = { nextQuestion(option == question.answer) }) {
                    Text(option.toString())
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Scor: $score", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate(Screen.MainMenu.route) }) {
            Text("Înapoi la Meniu")
        }
    }
}

// Generate a random arithmetic question.  Operands range from 0 to 9 and the
// operator is either addition or subtraction.  For subtraction the result is
// allowed to be negative to introduce variety.
private fun generateMathQuestion(): MathQuestion {
    val a = Random.nextInt(0, 10)
    val b = Random.nextInt(0, 10)
    val op = if (Random.nextBoolean()) "+" else "-"
    val result = if (op == "+") a + b else a - b
    val text = "$a $op $b = ?"
    return MathQuestion(text, result)
}

// Generate a set of three options including the correct answer and two
// distractors.  Distractors are generated by adding or subtracting up to 10.
private fun generateOptions(correct: Int): List<Int> {
    val opts = mutableSetOf<Int>()
    opts.add(correct)
    while (opts.size < 3) {
        val candidate = correct + Random.nextInt(-10, 11)
        // Ensure we don't duplicate the correct answer
        if (candidate != correct) {
            opts.add(candidate)
        }
    }
    return opts.shuffled()
}