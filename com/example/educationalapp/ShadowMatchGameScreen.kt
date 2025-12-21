package com.example.educationalapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlin.random.Random

/**
 * A simple shadow match game.  A silhouette of a shape is displayed in black and
 * the child must select the matching coloured shape from three options.  Stars
 * are awarded for correct matches.  This game reinforces shape recognition and
 * matching skills using familiar icons.
 */
data class ShadowItem(val name: String, val icon: ImageVector, val color: Color)

@Composable
fun ShadowMatchGameScreen(navController: NavController, starState: MutableState<Int>) {
    // Define a small set of shapes to choose from
    val shapes = listOf(
        ShadowItem("Inimă", Icons.Filled.Favorite, Color(0xFFE74C3C)),
        ShadowItem("Stea", Icons.Filled.Star, Color(0xFFF1C40F)),
        ShadowItem("Info", Icons.Filled.Info, Color(0xFF2ECC71)),
    )
    var current by remember { mutableStateOf(shapes[0]) }
    var options by remember { mutableStateOf(listOf<ShadowItem>()) }
    var feedback by remember { mutableStateOf("") }
    var score by remember { mutableStateOf(0) }

    // Helper to generate a new round with random options
    fun newRound() {
        feedback = ""
        current = shapes.random()
        val set = mutableSetOf<ShadowItem>()
        val list = mutableListOf<ShadowItem>()
        val correctIndex = Random.nextInt(3)
        for (i in 0 until 3) {
            if (i == correctIndex) {
                list.add(current)
            } else {
                var item: ShadowItem
                do {
                    item = shapes.random()
                } while (item == current || item in set)
                set.add(item)
                list.add(item)
            }
        }
        options = list
    }
    LaunchedEffect(Unit) { newRound() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Potrivire Umbre", modifier = Modifier.padding(bottom = 16.dp))
        Text(text = "Găsește forma corectă", modifier = Modifier.padding(bottom = 8.dp))
        // Show the silhouette (black icon of current shape)
        Card(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = current.icon,
                    contentDescription = current.name,
                    tint = Color.Black,
                    modifier = Modifier.size(80.dp)
                )
            }
        }
        // Option buttons with coloured shapes
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                Button(onClick = {
                    if (option == current) {
                        feedback = "Corect!"
                        score += 10
                        starState.value += 1
                    } else {
                        feedback = "Greșit!"
                        score = (score - 5).coerceAtLeast(0)
                    }
                    newRound()
                }, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = option.name,
                        tint = option.color,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
        Text(text = "Scor: $score", modifier = Modifier.padding(top = 16.dp))
        Text(text = feedback, modifier = Modifier.padding(top = 8.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate(Screen.MainMenu.route) }) {
            Text(text = "Înapoi la Meniu")
        }
    }
}