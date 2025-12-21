package com.example.educationalapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlin.random.Random

/**
 * A simple game that teaches children to recognise basic emotions.  A target
 * emotion is shown by name and the player must select the matching face.
 */
data class Emotion(val name: String, val emoji: String)

@Composable
fun EmotionsGameScreen(navController: NavController, starState: MutableState<Int>) {
    val emotions = listOf(
        Emotion("Fericit", "😀"),
        Emotion("Trist", "😢"),
        Emotion("Surprins", "😲"),
        Emotion("Furios", "😠"),
        Emotion("Speriat", "😱")
    )
    var currentEmotion by remember { mutableStateOf(emotions[0]) }
    var options by remember { mutableStateOf(listOf<Emotion>()) }
    var feedback by remember { mutableStateOf("") }
    var score by remember { mutableStateOf(0) }

    fun newRound() {
        feedback = ""
        currentEmotion = emotions.random()
        val optionList = mutableListOf<Emotion>()
        val used = mutableSetOf<Emotion>()
        val correctIndex = Random.nextInt(3)
        for (i in 0 until 3) {
            if (i == correctIndex) {
                optionList.add(currentEmotion)
            } else {
                var e: Emotion
                do {
                    e = emotions.random()
                } while (e == currentEmotion || e in used)
                used.add(e)
                optionList.add(e)
            }
        }
        options = optionList
    }
    LaunchedEffect(Unit) { newRound() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Joc Emoții", modifier = Modifier.padding(bottom = 16.dp))
        Text(text = "Arată emoția: ${currentEmotion.name}", modifier = Modifier.padding(bottom = 16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { emotion ->
                Button(onClick = {
                    if (emotion == currentEmotion) {
                        feedback = "Corect!"; score += 10; starState.value += 1
                    } else {
                        feedback = "Greșit!"; score = (score - 5).coerceAtLeast(0)
                    }
                    newRound()
                }, modifier = Modifier.weight(1f)) {
                    Text(text = emotion.emoji)
                }
            }
        }
        Text(text = "Scor: $score", modifier = Modifier.padding(top = 16.dp))
        if (feedback.isNotEmpty()) {
            Text(text = feedback, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = { navController.navigate(Screen.MainMenu.route) }) {
            Text(text = "Înapoi la Meniu")
        }
    }
}