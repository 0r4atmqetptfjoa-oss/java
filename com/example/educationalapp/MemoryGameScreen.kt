package com.example.educationalapp

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Shuffle a list of emoji strings into pairs of [MemoryCard] objects.  Each
 * emoji appears twice in the resulting list.  Cards are given unique
 * identifiers and the list is shuffled to randomise their positions.  This
 * helper is defined at file scope so that it can be referenced when
 * initialising state inside the composable.
 */
private fun shuffleMemoryCards(values: List<String>): List<MemoryCard> {
    val list = (values + values).mapIndexed { index, value -> MemoryCard(id = index, value = value) }
    return list.shuffled()
}

/**
 * A classic memory matching game. Cards are laid face down in a grid.  The
 * player flips two cards; if they match they remain face up and the player
 * earns points and a star.  If they do not match they are flipped back over.
 * The game ends when all pairs are matched.
 */
@Composable
fun MemoryGameScreen(navController: NavController, starState: MutableState<Int>) {
    // List of emoji pairs to use in the game
    val emojis = listOf("🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼")
    // Initialise cards with two of each emoji
    var cards by remember { mutableStateOf(shuffleMemoryCards(emojis)) }
    var selectedIndices by remember { mutableStateOf(listOf<Int>()) }
    var score by remember { mutableStateOf(0) }
    var showEndDialog by remember { mutableStateOf(false) }

    // Local shuffleCards function removed.  Use shuffleMemoryCards() defined at file scope.

    suspend fun checkMatch() {
        // If two cards are selected, check for match
        if (selectedIndices.size == 2) {
            val first = selectedIndices[0]
            val second = selectedIndices[1]
            if (cards[first].value == cards[second].value) {
                // Match found
                cards = cards.toMutableList().also {
                    it[first] = it[first].copy(isMatched = true)
                    it[second] = it[second].copy(isMatched = true)
                }
                score += 20
                starState.value += 1
                if (cards.all { it.isMatched }) {
                    showEndDialog = true
                }
            } else {
                // Pause briefly to show the two cards, then flip back
                delay(1000)
                cards = cards.toMutableList().also {
                    it[first] = it[first].copy(isFaceUp = false)
                    it[second] = it[second].copy(isFaceUp = false)
                }
                score = (score - 5).coerceAtLeast(0)
            }
            selectedIndices = emptyList()
        }
    }

    // Launch effect to monitor selections and check for matches
    LaunchedEffect(selectedIndices) {
        if (selectedIndices.size == 2) {
            checkMatch()
        }
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { navController.navigate(Screen.MainMenu.route) },
            title = { Text("Felicitări!", textAlign = TextAlign.Center) },
            text = { Text("Ai găsit toate perechile. Scorul tău este $score.", textAlign = TextAlign.Center) },
            confirmButton = {
                Button(onClick = {
                    // Restart game
                    cards = shuffleMemoryCards(emojis)
                    score = 0
                    selectedIndices = emptyList()
                    showEndDialog = false
                }) {
                    Text("Joacă din nou")
                }
            },
            dismissButton = {
                Button(onClick = { navController.navigate(Screen.MainMenu.route) }) {
                    Text("Meniu Principal")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_game_memory),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Joc de Memorie", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Găsește toate perechile", color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            // Display cards in a 4x4 grid
            for (row in 0 until 4) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (col in 0 until 4) {
                        val index = row * 4 + col
                        val card = cards[index]
                        MemoryCardView(card = card) {
                            // Only allow clicking unmatched and face down cards, and prevent selecting more than 2
                            if (!card.isFaceUp && !card.isMatched && selectedIndices.size < 2) {
                                cards = cards.toMutableList().also { it[index] = it[index].copy(isFaceUp = true) }
                                selectedIndices = selectedIndices + index
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Scor: $score", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.navigate(Screen.MainMenu.route) }) {
                Text("Înapoi la Meniu")
            }
        }
    }
}

@Composable
private fun MemoryCardView(card: MemoryCard, onClick: () -> Unit) {
    val alpha by animateFloatAsState(targetValue = if (card.isFaceUp || card.isMatched) 1f else 0f, label = "cardFlip")
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .size(60.dp)
            .padding(4.dp)
            .background(color = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Show emoji when card is face up or matched; otherwise show a coloured back
            if (card.isFaceUp || card.isMatched) {
                Text(text = card.value, fontSize = 24.sp)
            } else {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.secondary)
                )
            }
        }
    }
}