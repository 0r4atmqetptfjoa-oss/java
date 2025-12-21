package com.example.educationalapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlin.random.Random

/**
 * A simple parental gate.  Adults must solve a basic math question to
 * continue.  When the correct answer is provided the user is returned to
 * the main menu.  In a more complex app you could pass a destination
 * argument so the parent can unlock a specific screen.  No stars are
 * awarded here.
 */
@Composable
fun ParentalGateScreen(navController: NavController) {
    // Generate a random math question on first composition
    var operand1 by remember { mutableStateOf(Random.nextInt(1, 10)) }
    var operand2 by remember { mutableStateOf(Random.nextInt(1, 10)) }
    var userAnswer by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    fun refreshQuestion() {
        operand1 = Random.nextInt(1, 10)
        operand2 = Random.nextInt(1, 10)
        userAnswer = ""
        feedback = ""
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Poarta Părinte", modifier = Modifier.padding(bottom = 16.dp))
        Text(text = "Rezolvă calculul pentru a continua:", modifier = Modifier.padding(bottom = 8.dp))
        Text(text = "$operand1 + $operand2 = ?", modifier = Modifier.padding(bottom = 8.dp))
        TextField(
            value = userAnswer,
            onValueChange = { userAnswer = it.filter { ch -> ch.isDigit() } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            placeholder = { Text(text = "Răspunsul tău") }
        )
        Button(onClick = {
            val answer = userAnswer.toIntOrNull()
            if (answer == operand1 + operand2) {
                feedback = "Corect! Acces permis."
                navController.navigate(Screen.MainMenu.route)
            } else {
                feedback = "Greșit. Încearcă din nou."
                refreshQuestion()
            }
        }, modifier = Modifier.padding(vertical = 8.dp)) {
            Text(text = "Verifică")
        }
        if (feedback.isNotEmpty()) {
            Text(text = feedback, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate(Screen.MainMenu.route) }) {
            Text(text = "Înapoi la Meniu")
        }
    }
}