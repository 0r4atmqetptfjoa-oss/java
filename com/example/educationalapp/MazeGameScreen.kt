package com.example.educationalapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlin.random.Random

/**
 * A simple maze game implemented on a 5x5 grid.  The player starts at the
 * top‑left corner (0,0) and must reach the goal at the bottom‑right corner.
 * Random walls are generated each round to create obstacles.  Movement is
 * controlled via four arrow buttons.  Reaching the goal awards points and
 * stars, after which a new maze is generated.
 */
@Composable
fun MazeGameScreen(navController: NavController, starState: MutableState<Int>) {
    val size = 5
    val goalPos = Pair(size - 1, size - 1)
    var playerPos by remember { mutableStateOf(Pair(0, 0)) }
    var walls by remember { mutableStateOf<Set<Pair<Int, Int>>>(emptySet()) }
    var score by remember { mutableStateOf(0) }
    var feedback by remember { mutableStateOf("") }

    fun generateWalls(): Set<Pair<Int, Int>> {
        val set = mutableSetOf<Pair<Int, Int>>()
        while (set.size < 5) {
            val r = Random.nextInt(size)
            val c = Random.nextInt(size)
            val cell = Pair(r, c)
            if (cell != Pair(0, 0) && cell != goalPos) {
                set.add(cell)
            }
        }
        return set
    }

    fun newRound() {
        walls = generateWalls()
        playerPos = Pair(0, 0)
        feedback = ""
    }

    LaunchedEffect(Unit) { newRound() }

    fun move(deltaRow: Int, deltaCol: Int) {
        val newRow = playerPos.first + deltaRow
        val newCol = playerPos.second + deltaCol
        val newPos = Pair(newRow, newCol)
        if (newRow in 0 until size && newCol in 0 until size && newPos !in walls) {
            playerPos = newPos
            if (newPos == goalPos) {
                feedback = "Ai găsit ieșirea!"
                score += 10
                // Reaching the goal yields two stars since it may take more effort
                starState.value += 2
                newRound()
            } else {
                feedback = ""
            }
        } else {
            feedback = "Blocaj!"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Labirint", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        // Draw the maze grid.  Walls are grey, the goal is green, the player is blue.
        Column {
            for (r in 0 until size) {
                Row {
                    for (c in 0 until size) {
                        val pos = Pair(r, c)
                        val color = when {
                            pos == playerPos -> Color(0xFF3498DB) // player - blue
                            pos == goalPos -> Color(0xFF2ECC71) // goal - green
                            walls.contains(pos) -> Color(0xFF7F8C8D) // wall - grey
                            else -> Color(0xFFFFFFFF) // empty - white
                        }
                        Card(
                            modifier = Modifier
                                .size(40.dp)
                                .padding(2.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color)
                            ) {}
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Scor: $score")
        Text(text = feedback, color = Color.Red)
        Spacer(modifier = Modifier.height(16.dp))
        // Movement controls
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
                Spacer(modifier = Modifier.width(40.dp))
                Button(onClick = { move(-1, 0) }) { Text("Sus") }
                Spacer(modifier = Modifier.width(40.dp))
            }
            Row {
                Button(onClick = { move(0, -1) }) { Text("Stânga") }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { move(0, 1) }) { Text("Dreapta") }
            }
            Row {
                Spacer(modifier = Modifier.width(40.dp))
                Button(onClick = { move(1, 0) }) { Text("Jos") }
                Spacer(modifier = Modifier.width(40.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate(Screen.MainMenu.route) }) {
            Text("Înapoi la Meniu")
        }
    }
}