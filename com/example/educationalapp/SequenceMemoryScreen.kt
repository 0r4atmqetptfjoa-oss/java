package com.example.educationalapp

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun SequenceMemoryScreen(navController: NavController, starState: MutableState<Int>) {
    // Colors for sequence
    val colors = listOf(
        Color(0xFFE74C3C), // Red
        Color(0xFF3498DB), // Blue
        Color(0xFF2ECC71), // Green
        Color(0xFFF1C40F)  // Yellow
    )
    var sequence by remember { mutableStateOf(listOf<Int>()) }
    var userIndex by remember { mutableStateOf(0) }
    var isShowingSequence by remember { mutableStateOf(false) }
    var round by remember { mutableStateOf(1) }
    var feedback by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    // Highlight states for each color button
    val highlightStates = remember { List(colors.size) { Animatable(0f) } }

    fun generateSequence() {
        sequence = List(round) { Random.nextInt(colors.size) }
        userIndex = 0
        isShowingSequence = true
        feedback = ""
        // Show sequence
        scope.launch {
            delay(500)
            for (index in sequence) {
                // highlight
                highlightStates[index].animateTo(1f, animationSpec = tween(durationMillis = 200))
                delay(400)
                highlightStates[index].animateTo(0f, animationSpec = tween(durationMillis = 200))
                delay(200)
            }
            isShowingSequence = false
        }
    }

    fun resetGame() {
        round = 1
        generateSequence()
    }

    LaunchedEffect(Unit) { generateSequence() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Memorie Secvențe", modifier = Modifier.padding(bottom = 16.dp))
        Text(text = "Urmează secvența de culori", modifier = Modifier.padding(bottom = 16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colors.forEachIndexed { index, baseColor ->
                val highlightAlpha = highlightStates[index].value
                val displayColor = lerp(baseColor, Color.White, highlightAlpha)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(displayColor)
                        .clickable(enabled = !isShowingSequence) {
                            // If user click correct sequence
                            if (!isShowingSequence) {
                                if (sequence.isNotEmpty() && index == sequence[userIndex]) {
                                    userIndex++
                                    if (userIndex == sequence.size) {
                                        // Completed round
                                        feedback = "Corect!";
                                        starState.value += 2
                                        round++
                                        generateSequence()
                                    }
                                } else {
                                    feedback = "Greșit!";
                                    resetGame()
                                }
                            }
                        }
                )
            }
        }
        Text(text = feedback, modifier = Modifier.padding(top = 16.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { resetGame() }) {
            Text(text = "Repornește")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { navController.navigate(Screen.MainMenu.route) }) {
            Text(text = "Înapoi la Meniu")
        }
    }
}