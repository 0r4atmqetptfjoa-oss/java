package com.example.educationalapp

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AvatarCreatorScreen(navController: NavController, starState: MutableState<Int>) {
    val hairColours = listOf(Color(0xFF4A90E2), Color(0xFFFFD700), Color(0xFF8B4513), Color(0xFF000000), Color(0xFFFF69B4))
    val eyeTypes = listOf("Rotunde", "Stele")
    val mouthTypes = listOf("Zâmbet", "Neutru", "Surprins")

    var hairColour by remember { mutableStateOf(hairColours[0]) }
    var selectedEye by remember { mutableStateOf(0) }
    var selectedMouth by remember { mutableStateOf(0) }
    var saved by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }

    fun saveAvatar() {
        if (!saved) {
            starState.value += 3
            feedback = "Avatar salvat! Ai primit 3 stele."
            saved = true
        } else {
            feedback = "Avatarul este deja salvat."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Creează Avatar", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Canvas(modifier = Modifier.size(200.dp)) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = size.minDimension / 2 * 0.8f
            drawCircle(color = Color(0xFFFFE0BD), center = Offset(centerX, centerY), radius = radius)
            drawOval(
                color = hairColour,
                topLeft = Offset(centerX - radius, centerY - radius * 1.1f),
                size = Size(radius * 2, radius * 1.2f)
            )
            val eyeY = centerY - radius * 0.3f
            val eyeXOffset = radius * 0.4f
            when (selectedEye) {
                0 -> {
                    drawCircle(color = Color.Black, center = Offset(centerX - eyeXOffset, eyeY), radius = radius * 0.08f)
                    drawCircle(color = Color.Black, center = Offset(centerX + eyeXOffset, eyeY), radius = radius * 0.08f)
                }
                1 -> {
                    fun drawStar(x: Float, y: Float) {
                        val starSize = radius * 0.12f
                        drawLine(Color.Black, Offset(x - starSize, y), Offset(x + starSize, y), strokeWidth = 3f)
                        drawLine(Color.Black, Offset(x, y - starSize), Offset(x, y + starSize), strokeWidth = 3f)
                    }
                    drawStar(centerX - eyeXOffset, eyeY)
                    drawStar(centerX + eyeXOffset, eyeY)
                }
            }
            val mouthY = centerY + radius * 0.3f
            when (selectedMouth) {
                0 -> {
                    drawArc(
                        color = Color.Black,
                        startAngle = 200f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(centerX - radius * 0.4f, mouthY - radius * 0.1f),
                        size = Size(radius * 0.8f, radius * 0.5f)
                    )
                }
                1 -> {
                    drawLine(
                        Color.Black,
                        Offset(centerX - radius * 0.3f, mouthY),
                        Offset(centerX + radius * 0.3f, mouthY),
                        strokeWidth = 6f
                    )
                }
                2 -> {
                    drawCircle(
                        color = Color.Black,
                        center = Offset(centerX, mouthY),
                        radius = radius * 0.08f
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Alege culoarea părului:")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            hairColours.forEach { colour ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(colour, shape = CircleShape)
                        .selectable(
                            selected = hairColour == colour,
                            onClick = { hairColour = colour }
                        )
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Alege ochii:")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            eyeTypes.forEachIndexed { index, label ->
                Button(onClick = { selectedEye = index }, modifier = Modifier.weight(1f).padding(4.dp)) {
                    Text(text = label)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Alege gura:")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            mouthTypes.forEachIndexed { index, label ->
                Button(onClick = { selectedMouth = index }, modifier = Modifier.weight(1f).padding(4.dp)) {
                    Text(text = label)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { saveAvatar() }) {
            Text(text = "Salvează Avatar")
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