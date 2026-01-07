package com.example.educationalapp.features.games

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.educationalapp.R
import com.example.educationalapp.navigation.*

/**
 * Data model actualizat: 'route' este Any (obiectul rutei), nu String.
 */
data class Game(val name: String, val icon: Int, val route: Any)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesMenuScreen(navController: NavController) {
    val games = remember {
        listOf(
            Game("Culori", R.drawable.icon_game_colors, ColorsRoute),
            Game("Forme", R.drawable.icon_game_shapes, ShapesRoute),
            Game("Alfabet", R.drawable.icon_game_alphabet, AlphabetQuizRoute),
            Game("Numere", R.drawable.icon_game_math, MathRoute),
            Game("Sortare", R.drawable.icon_game_sorting, SortingRoute),
            Game("Puzzle", R.drawable.icon_game_puzzle, PuzzleRoute),
            Game("Memorie", R.drawable.icon_game_memory, MemoryRoute),
            Game("Secvențe", R.drawable.icon_game_sequence, SequenceRoute),
            Game("Blocuri", R.drawable.main_menu_icon_jocuri, BlocksRoute),
            Game("Gătit", R.drawable.main_menu_icon_jocuri, CookingRoute),
            Game("Labirint", R.drawable.main_menu_icon_jocuri, MazeRoute),
            Game("Ascunse", R.drawable.icon_game_hiddenobjects, HiddenObjectsRoute),

            // NOI (2026)
            Game("Grădină Magică", R.drawable.main_menu_icon_jocuri, MagicGardenRoute),
            Game("Umbre", R.drawable.main_menu_icon_jocuri, ShadowMatchRoute),

            Game("Animale", R.drawable.icon_game_animals, AnimalSortingRoute),
            Game("Instrumente", R.drawable.icon_game_instruments, InstrumentsGameRoute),
            Game("Codare", R.drawable.main_menu_icon_jocuri, CodingRoute)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_category_games),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(id = R.string.main_menu_button_games),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 112.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
            ) {
                items(games) { game ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            navController.navigate(game.route)
                        }
                    ) {
                        Image(
                            painter = painterResource(id = game.icon),
                            contentDescription = game.name,
                            modifier = Modifier.size(80.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = game.name,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun GamesMenuScreenPreview() {
    GamesMenuScreen(rememberNavController())
}