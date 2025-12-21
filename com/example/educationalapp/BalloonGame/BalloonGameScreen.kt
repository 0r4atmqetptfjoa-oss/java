package com.example.educationalapp.BalloonGame

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.educationalapp.R
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

@Composable
fun BalloonGameScreen(
    viewModel: BalloonViewModel = hiltViewModel(),
    onHome: () -> Unit
) {
    // Configurare Dimensiuni Ecran
    val config = LocalConfiguration.current
    val screenH = config.screenHeightDp.toFloat() // Aproximativ, în dp
    val screenW = config.screenWidthDp.toFloat()

    // --- GAME LOOP (Driver-ul principal) ---
    LaunchedEffect(Unit) {
        var lastTime = withFrameNanos { it }
        while (isActive) {
            val currentTime = withFrameNanos { it }
            val dt = (currentTime - lastTime) / 1_000_000_000f // Delta time în secunde
            lastTime = currentTime
            
            // Pasăm controlul ViewModel-ului să calculeze pozițiile noi
            viewModel.updateGame(dt, screenH * 2.5f, screenW * 2.5f) // Multiplicator pt conversie pixeli/dp brut
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // A. FUNDAL
        Image(
            painter = painterResource(id = R.drawable.bg_alphabet_sky),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // B. BALOANE
        // Notă: Folosim forEach simplu deoarece lista e mică
        viewModel.balloons.forEach { balloon ->
            if (!balloon.isPopped) {
                Image(
                    painter = painterResource(id = balloon.imageRes),
                    contentDescription = null,
                    modifier = Modifier
                        .offset { IntOffset(balloon.startX.roundToInt(), balloon.y.roundToInt()) }
                        .size(90.dp, 120.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { viewModel.onBalloonTap(balloon) }
                )
            }
        }

        // C. PARTICULE
        viewModel.particles.forEach { p ->
            Image(
                painter = painterResource(id = R.drawable.vfx_star),
                contentDescription = null,
                modifier = Modifier
                    .offset { IntOffset(p.currentX.roundToInt(), p.currentY.roundToInt()) }
                    .size(30.dp)
                    .alpha(p.alpha)
            )
        }

        // D. SCOR (HUD)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ui_score_container),
                contentDescription = null,
                modifier = Modifier.size(120.dp, 60.dp)
            )
            Text(
                text = "${viewModel.score.value}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // E. BUTON HOME
        Image(
            painter = painterResource(id = R.drawable.ui_button_home),
            contentDescription = "Home",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(64.dp)
                .clickable { onHome() }
        )
    }
}