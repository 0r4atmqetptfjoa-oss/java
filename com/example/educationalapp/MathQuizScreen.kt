package com.example.educationalapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@Composable
fun MathQuizScreen(
    navController: NavController, 
    starState: MutableState<Int>,
    viewModel: MathQuizViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) { viewModel.generateQuestion() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Quiz Matematic", modifier = Modifier.padding(bottom = 16.dp))
        Text(text = "Rezolvă: ${viewModel.question}", modifier = Modifier.padding(bottom = 16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            viewModel.options.forEach { opt ->
                Button(onClick = {
                    viewModel.onOptionSelected(opt) { starState.value++ }
                }, modifier = Modifier.weight(1f)) {
                    Text(text = opt.toString())
                }
            }
        }
        Text(text = "Scor: ${viewModel.score}", modifier = Modifier.padding(top = 16.dp))
        Text(text = viewModel.feedback, modifier = Modifier.padding(top = 8.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate(Screen.MainMenu.route) }) {
            Text(text = "Înapoi la Meniu")
        }
    }
}