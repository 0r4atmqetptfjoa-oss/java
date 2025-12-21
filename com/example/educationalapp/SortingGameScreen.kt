package com.example.educationalapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.MutableState
import androidx.navigation.NavController

/**
 * UI for the number sorting game.  Numbers must be selected in ascending
 * order.  The level increases after each successful round, introducing more
 * numbers.  Stars are awarded via the [SortingGameViewModel] when a level
 * completes.
 */
@Composable
fun SortingGameScreen(navController: NavController, starState: MutableState<Int>, viewModel: SortingGameViewModel = hiltViewModel()) {
    // Capture state from the ViewModel
    val numbers = viewModel.numbers
    val level = viewModel.level
    val feedback = viewModel.feedback
    val score = viewModel.score

    fun onNumberClick(number: Int) {
        viewModel.onNumberClick(number) { stars ->
            starState.value += stars
        }
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
            Text(text = "Joc de Sortare", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Nivel: $level", color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Selectează numerele în ordine crescătoare", color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            // Display numbers in rows of 4 for better spacing
            numbers.chunked(4).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { num ->
                        Button(onClick = { onNumberClick(num) }, modifier = Modifier.weight(1f).padding(4.dp)) {
                            Text(num.toString())
                        }
                    }
                    // Fill empty slots to align rows
                    if (row.size < 4) {
                        repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Scor: $score", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = feedback, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.navigate(Screen.MainMenu.route) }) {
                Text("Înapoi la Meniu")
            }
        }
    }
}