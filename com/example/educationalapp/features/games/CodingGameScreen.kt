package com.example.educationalapp.features.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.educationalapp.R
import kotlinx.coroutines.delay

/**
 * A simple coding game where the player programs a robot to reach a goal on a grid.
 * Players build a sequence of commands using directional buttons, then press run to
 * execute the commands.  Stars are awarded for successfully reaching the goal.
 */
@Composable
fun CodingGameScreen(
    navController: NavController,
    starState: MutableState<Int>
) {
    // Define grid dimensions
    val gridSize = 5
    val robotPos = remember { mutableStateOf(0 to 0) } // row to col
    val goalPos = remember { 4 to 4 } // goal in bottom right
    val commands = remember { mutableStateListOf<Direction>() }
    val message = remember { mutableStateOf("Program the robot to reach the goal!") }
    val running = remember { mutableStateOf(false) }

    // When run flag becomes true, execute commands sequentially
    LaunchedEffect(running.value) {
        if (running.value) {
            // Reset robot to start position at beginning of run
            robotPos.value = 0 to 0
            for (dir in commands) {
                // Apply the command
                val (row, col) = robotPos.value
                robotPos.value = when (dir) {
                    Direction.UP -> (maxOf(row - 1, 0)) to col
                    Direction.DOWN -> (minOf(row + 1, gridSize - 1)) to col
                    Direction.LEFT -> row to maxOf(col - 1, 0)
                    Direction.RIGHT -> row to minOf(col + 1, gridSize - 1)
                }
                // Delay to simulate animation
                delay(300)
            }
            // Check if robot reached the goal
            if (robotPos.value == goalPos) {
                starState.value = starState.value + 2
                message.value = "Great job! You reached the goal. ⭐"
            } else {
                message.value = "Oops! The robot didn't reach the goal. Try again."
            }
            running.value = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Top bar with back button and title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier
                    .size(32.dp)
                    .clickable { navController.popBackStack() },
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Joc de codare",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Grid representation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0 until gridSize) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (col in 0 until gridSize) {
                            val isRobot = robotPos.value == row to col
                            val isGoal = goalPos == row to col
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .border(1.dp, Color.White)
                                    .background(
                                        when {
                                            isRobot -> Color(0xFF4CAF50) // green for robot
                                            isGoal -> Color(0xFFFFC107) // amber for goal
                                            else -> Color(0xFF607D8B) // blue grey
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                // Show labels or icons if necessary
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Command sequence display
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Secvență:", color = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            commands.forEach { dir ->
                Icon(
                    imageVector = when (dir) {
                        Direction.UP -> Icons.Default.ArrowUpward
                        Direction.DOWN -> Icons.Default.ArrowDownward
                        Direction.LEFT -> Icons.Default.ArrowLeft
                        Direction.RIGHT -> Icons.Default.ArrowRight
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Control buttons for adding commands
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = { if (!running.value) commands.add(Direction.UP) }) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Up", tint = Color.White)
            }
            IconButton(onClick = { if (!running.value) commands.add(Direction.DOWN) }) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "Down", tint = Color.White)
            }
            IconButton(onClick = { if (!running.value) commands.add(Direction.LEFT) }) {
                Icon(Icons.Default.ArrowLeft, contentDescription = "Left", tint = Color.White)
            }
            IconButton(onClick = { if (!running.value) commands.add(Direction.RIGHT) }) {
                Icon(Icons.Default.ArrowRight, contentDescription = "Right", tint = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Run and Clear buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            IconButton(onClick = {
                if (commands.isNotEmpty() && !running.value) {
                    running.value = true
                    message.value = "Running program..."
                }
            }) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Run",
                    tint = Color.Green,
                    modifier = Modifier.size(48.dp)
                )
            }
            IconButton(onClick = {
                if (!running.value) {
                    commands.clear()
                    robotPos.value = 0 to 0
                    message.value = "Program cleared."
                }
            }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Clear",
                    tint = Color.Red,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Message display
        Text(text = message.value, color = Color.White)
    }
}

/** Direction enum used to build command sequences for the robot. */
private enum class Direction { UP, DOWN, LEFT, RIGHT }