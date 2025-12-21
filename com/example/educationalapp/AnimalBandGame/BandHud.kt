package com.example.educationalapp.AnimalBandGame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BandHud(
    beatPhase: Float,
    jam: Float,
    finalJam: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier.width(320.dp)) {
        // Beat indicator
        Canvas(Modifier.width(320.dp).height(10.dp)) {
            drawRect(Color.Gray.copy(alpha = 0.45f))
            val x = size.width * beatPhase.coerceIn(0f, 1f)
            drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), strokeWidth = 4f)
        }

        Spacer(Modifier.height(6.dp))

        // Jam bar
        Canvas(Modifier.width(320.dp).height(10.dp)) {
            drawRect(Color.Gray.copy(alpha = 0.45f))
            val w = size.width * jam.coerceIn(0f, 1f)
            drawRect(if (finalJam) Color.Red else Color.Cyan, size = size.copy(width = w))
        }
    }
}
