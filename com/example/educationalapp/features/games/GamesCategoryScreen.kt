package com.example.educationalapp.features.games

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.educationalapp.R

data class GameItem(
    val title: String,
    val icon: Int,
    val background: Int,
    val destination: String
)

// AICI ADĂUGĂM JOCURILE NOI
val gamesList = listOf(
    // Folosim icon_game_hiddenobjects ca placeholder pentru Peek-a-Boo momentan
    GameItem("Peek-a-Boo", R.drawable.icon_game_hiddenobjects, R.drawable.bg_sunny_meadow, "peekaboo"),
    GameItem("Alphabet", R.drawable.icon_game_alphabet, R.drawable.bg_game_alphabet, "alphabet"),
    GameItem("Colors", R.drawable.icon_game_colors, R.drawable.bg_game_colors, "colors"),
    GameItem("Shapes", R.drawable.icon_game_shapes, R.drawable.bg_game_shapes, "shapes"),
    GameItem("Puzzle", R.drawable.icon_game_puzzle, R.drawable.bg_game_puzzle, "puzzle"),
    GameItem("Memory", R.drawable.icon_game_memory, R.drawable.bg_game_memory, "memory"),
    GameItem("Hidden Objects", R.drawable.icon_game_hiddenobjects, R.drawable.bg_game_hiddenobjects, "hidden"),
    GameItem("Sorting", R.drawable.icon_game_sorting, R.drawable.bg_game_sorting, "sorting"),
    GameItem("Instruments", R.drawable.icon_game_instruments, R.drawable.bg_game_instruments, "instruments"),
    GameItem("Sequence", R.drawable.icon_game_sequence, R.drawable.bg_game_sequence, "sequence"),
    GameItem("Math", R.drawable.icon_game_math, R.drawable.bg_game_math, "math")
)

@Composable
fun GamesCategoryScreen(onSelect: (GameItem) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .paint(
                painterResource(R.drawable.bg_category_games),
                contentScale = ContentScale.Crop
            )
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            items(gamesList) { item ->
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .clickable { onSelect(item) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painterResource(item.icon),
                        contentDescription = item.title,
                        modifier = Modifier.size(120.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun GameContainer(game: GameItem, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .paint(
                painterResource(game.background),
                contentScale = ContentScale.Crop
            )
    ) {
        content()
    }
}