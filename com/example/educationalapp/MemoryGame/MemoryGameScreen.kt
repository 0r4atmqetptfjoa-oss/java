package com.example.educationalapp.MemoryGame

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.educationalapp.R

@Composable
fun MemoryGameScreen(
    viewModel: MemoryViewModel = hiltViewModel(),
    onHome: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // A. FUNDAL
        Image(
            painter = painterResource(id = R.drawable.bg_game_memory),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // B. GRID-UL DE CĂRȚI
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.widthIn(max = 600.dp)
            ) {
                items(viewModel.cards.size) { index ->
                    val card = viewModel.cards[index]
                    MemoryCardView(
                        card = card,
                        onClick = { viewModel.onCardClick(card) }
                    )
                }
            }
        }

        // C. MESAJ VICTORIE
        if (viewModel.isGameWon()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.mascot_robot_happy),
                    contentDescription = "Win",
                    modifier = Modifier.size(250.dp)
                )
                Text(
                    text = "BRAVO!",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // D. BUTON HOME
        Image(
            painter = painterResource(id = R.drawable.ui_btn_home),
            contentDescription = "Home",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(64.dp)
                .clickable { onHome() }
        )
    }
}

@Composable
fun MemoryCardView(card: MemoryCard, onClick: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (card.isFlipped || card.isMatched) 180f else 0f,
        animationSpec = tween(400), label = "FlipAnim"
    )

    Box(
        modifier = Modifier
            .aspectRatio(0.8f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable(enabled = !card.isFlipped && !card.isMatched) { onClick() }
    ) {
        if (rotation <= 90f) {
            // SPATE (Neîntors)
            Image(
                painter = painterResource(id = R.drawable.alphabet_letter_card_blank),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // FAȚĂ (Întors)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.shape_square_cracker),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().alpha(0.9f),
                    contentScale = ContentScale.FillBounds
                )
                Image(
                    painter = painterResource(id = card.imageRes),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center).fillMaxSize(0.7f)
                )
            }
        }
    }
}