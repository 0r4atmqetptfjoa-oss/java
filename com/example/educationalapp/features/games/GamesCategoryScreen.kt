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

// Games shown in the Games category.
// Note: many games reuse existing icons/backgrounds as placeholders to avoid new drawable dependencies.
val gamesList = listOf(
    GameItem("Peek-a-Boo", R.drawable.icon_game_hiddenobjects, R.drawable.bg_sunny_meadow, GameRoutes.PEEKABOO),
    GameItem("Alphabet", R.drawable.icon_game_alphabet, R.drawable.bg_game_alphabet, GameRoutes.ALPHABET),
    GameItem("Colors", R.drawable.icon_game_colors, R.drawable.bg_game_colors, GameRoutes.COLORS),
    GameItem("Shapes", R.drawable.icon_game_shapes, R.drawable.bg_game_shapes, GameRoutes.SHAPES),
    GameItem("Puzzle", R.drawable.icon_game_puzzle, R.drawable.bg_game_puzzle, GameRoutes.PUZZLE),
    GameItem("Memory", R.drawable.icon_game_memory, R.drawable.bg_game_memory, GameRoutes.MEMORY),
    GameItem("Hidden Objects", R.drawable.icon_game_hiddenobjects, R.drawable.bg_game_hiddenobjects, GameRoutes.HIDDEN_OBJECTS),
    GameItem("Sorting", R.drawable.icon_game_sorting, R.drawable.bg_game_sorting, GameRoutes.SORTING),
    GameItem("Sequence", R.drawable.icon_game_sequence, R.drawable.bg_game_sequence, GameRoutes.SEQUENCE),
    GameItem("Math", R.drawable.icon_game_math, R.drawable.bg_game_math, GameRoutes.MATH),

    // Newly linked mini-games (placeholders for icon/background)
    GameItem("Coding", R.drawable.icon_game_sequence, R.drawable.bg_sunny_meadow, GameRoutes.CODING),
    GameItem("Maze", R.drawable.icon_game_puzzle, R.drawable.bg_sunny_meadow, GameRoutes.MAZE),
    GameItem("Blocks", R.drawable.icon_game_shapes, R.drawable.bg_sunny_meadow, GameRoutes.BLOCKS),
    GameItem("Cooking", R.drawable.icon_game_colors, R.drawable.bg_sunny_meadow, GameRoutes.COOKING),
    GameItem("Shadow Match", R.drawable.icon_game_memory, R.drawable.bg_sunny_meadow, GameRoutes.SHADOW_MATCH),
    GameItem("Animal Sorting", R.drawable.icon_game_hiddenobjects, R.drawable.bg_sunny_meadow, GameRoutes.ANIMAL_SORTING),
    GameItem("Emotions", R.drawable.icon_game_alphabet, R.drawable.bg_sunny_meadow, GameRoutes.EMOTIONS),
    GameItem("Instruments", R.drawable.icon_game_instruments, R.drawable.bg_sunny_meadow, GameRoutes.INSTRUMENTS),

    // Imported mini-games previously not wired in this codebase
    GameItem("Egg Surprise", R.drawable.icon_game_puzzle, R.drawable.bg_sunny_meadow, GameRoutes.EGG_SURPRISE),
    GameItem("Feed Monster", R.drawable.icon_game_colors, R.drawable.bg_sunny_meadow, GameRoutes.FEED_MONSTER),
    GameItem("Animal Band", R.drawable.icon_game_instruments, R.drawable.bg_sunny_meadow, GameRoutes.ANIMAL_BAND)
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