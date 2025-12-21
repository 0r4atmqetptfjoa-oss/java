package com.example.educationalapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.navigation.NavController
import kotlin.random.Random

/**
 * A simple block matching game.  A target colour is shown as a square and the
 * child must select the matching coloured block from three options.  Stars
 * and points are awarded for correct matches.  Similar in spirit to the
 * colour match game but uses colour swatches rather than names.
 */
data class ColourBlock(val name: String, val color: Color)

@Composable
fun BlockGameScreen(navController: NavController, starState: MutableState<Int>) {
    val colours = listOf(
        ColourBlock("Roșu", Color.Red),
        ColourBlock("Verde", Color.Green),
        ColourBlock("Albastru", Color.Blue),
        ColourBlock("Galben", Color.Yellow),
        ColourBlock("Mov", Color.Magenta)
    )
    var target by remember { mutableStateOf(colours[0]) }
    var options by remember { mutableStateOf(listOf<ColourBlock>()) }
    var feedback by remember { mutableStateOf("") }
    var score by remember { mutableStateOf(0) }
    fun newRound() {
        feedback = ""
        target = colours.random()
        val set = mutableSetOf<ColourBlock>()
        val optionList = mutableListOf<ColourBlock>()
        val correctIndex = Random.nextInt(3)
        for (i in 0 until 3) {
            if (i == correctIndex) {
                optionList.add(target)
            } else {
                var c: ColourBlock
                do {
                    c = colours.random()
                } while (c == target || c in set)
                set.add(c)
                optionList.add(c)
            }
        }
        options = optionList
    }
    LaunchedEffect(Unit) { newRound() }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Joc Blocuri", modifier = Modifier.padding(bottom = 16.dp))
        Text(text = "Potrivește blocul de culoare", modifier = Modifier.padding(bottom = 8.dp))
        // Target display
        Box(modifier = Modifier.size(100.dp).background(target.color).padding(bottom = 16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                Button(onClick = {
                    if (option == target) {
                        feedback = "Corect!";
                        score += 10; starState.value += 1
                    } else {
                        feedback = "Greșit!";
                        score = (score - 5).coerceAtLeast(0)
                    }
                    newRound()
                }, modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.size(40.dp).background(option.color))
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