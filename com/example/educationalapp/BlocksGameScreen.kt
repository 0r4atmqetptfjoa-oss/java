package com.example.educationalapp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun BlocksGameScreen(navController: NavController, starState: MutableState<Int>) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Blocks Game Screen Placeholder")
        Button(onClick = { navController.navigate(Screen.GamesMenu.route) }) {
            Text(text = "Back to Games Menu")
        }
    }
}