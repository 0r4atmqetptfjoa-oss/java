package com.example.educationalapp

import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController

// Importurile modulelor
import com.example.educationalapp.alphabet.AlphabetGameScreen
import com.example.educationalapp.colors.ColorsGameScreen
import com.example.educationalapp.features.games.*
import com.example.educationalapp.features.instruments.InstrumentsMenuScreen
import com.example.educationalapp.features.mainmenu.MainMenuScreen
import com.example.educationalapp.features.songs.SongsMenuScreen
import com.example.educationalapp.features.songs.SongPlayerScreen
import com.example.educationalapp.features.sounds.*
import com.example.educationalapp.features.stories.StoriesMenuScreen
import com.example.educationalapp.peekaboo.PeekABooGame
import com.example.educationalapp.puzzle.PuzzleGameScreen
import com.example.educationalapp.shapes.ShapesGameScreen

// JOCURI NOI IMPORTATE
import com.example.educationalapp.BalloonGame.BalloonGameScreen
import com.example.educationalapp.MemoryGame.MemoryGameScreen
import com.example.educationalapp.AnimalBandGame.AnimalBandGame
import com.example.educationalapp.EggGame.EggGameScreen
import com.example.educationalapp.FeedGame.FeedGameScreen

private fun backToGames(navController: NavController) {
    val popped = navController.popBackStack(Screen.GamesMenu.route, false)
    if (!popped) {
        navController.navigate(Screen.GamesMenu.route) {
            popUpTo(Screen.MainMenu.route)
        }
    }
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()

    val starCount by viewModel.starCount.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val musicEnabled by viewModel.musicEnabled.collectAsState()
    val hardModeEnabled by viewModel.hardModeEnabled.collectAsState()

    val starState = remember { mutableStateOf(starCount) }

    LaunchedEffect(starCount) {
        if (starState.value != starCount) starState.value = starCount
    }
    LaunchedEffect(starState.value) {
        viewModel.setStarCount(starState.value)
    }

    val gameByRoute = remember {
        gamesList.associateBy { it.destination }
    }

    fun wrap(route: String, content: @Composable () -> Unit): @Composable () -> Unit = {
        val game = gameByRoute[route]
        if (game != null) {
            GameContainer(game) { content() }
        } else {
            content()
        }
    }

    NavHost(navController = navController, startDestination = Screen.MainMenu.route) {

        composable(Screen.MainMenu.route) {
            MainMenuScreen(navController = navController, starCount = starCount)
        }

        composable(Screen.SettingsScreen.route) {
            SettingsScreen(
                navController = navController,
                soundEnabled = soundEnabled,
                musicEnabled = musicEnabled,
                hardModeEnabled = hardModeEnabled,
                onSoundChanged = { viewModel.toggleSound() },
                onMusicChanged = { viewModel.toggleMusic() },
                onHardModeChanged = { viewModel.toggleHardMode() }
            )
        }

        composable(Screen.GamesMenu.route) {
            GamesMenuScreen(navController = navController)
        }

        navigation(
            startDestination = GameRoutes.ALPHABET_QUIZ,
            route = GameRoutes.ALPHABET_GRAPH
        ) {
            composable(GameRoutes.ALPHABET_QUIZ) {
                AlphabetGameScreen(
                    onBackToMenu = { backToGames(navController) }
                )
            }
        }

        // --- JOCURI STANDARD ---
        composable(GameRoutes.PEEKABOO) { wrap(GameRoutes.PEEKABOO) { PeekABooGame(onHome = { backToGames(navController) }) }() }
        composable(GameRoutes.COLORS) { wrap(GameRoutes.COLORS) { ColorsGameScreen(onBack = { backToGames(navController) }) }() }
        composable(GameRoutes.SHAPES) { wrap(GameRoutes.SHAPES) { ShapesGameScreen(onBack = { backToGames(navController) }) }() }
        composable(GameRoutes.PUZZLE) { wrap(GameRoutes.PUZZLE) { PuzzleGameScreen(onBack = { backToGames(navController) }) }() }

        // --- JOCUL DE GĂTIT ---
        composable(GameRoutes.COOKING) {
            wrap(GameRoutes.COOKING) {
                CookingGameScreen(
                    onBack = { backToGames(navController) }
                )
            }()
        }

        // --- MAGIC GARDEN ---
        composable(GameRoutes.MAGIC_GARDEN) {
            wrap(GameRoutes.MAGIC_GARDEN) {
                MagicGardenGameScreen(
                    hasFullVersion = false, // Acum este corect, deoarece am actualizat MagicGardenGameScreen sa accepte acest parametru
                    onBack = { backToGames(navController) }
                )
            }()
        }

        // --- ALTE JOCURI ---
        composable(GameRoutes.MEMORY) { wrap(GameRoutes.MEMORY) { MemoryGameScreen(onHome = { backToGames(navController) }) }() }
        composable(GameRoutes.BALLOON_POP) { wrap(GameRoutes.BALLOON_POP) { BalloonGameScreen(onHome = { backToGames(navController) }) }() }
        composable(GameRoutes.ANIMAL_BAND) { wrap(GameRoutes.ANIMAL_BAND) { AnimalBandGame(onHome = { backToGames(navController) }) }() }

        composable(GameRoutes.HIDDEN_OBJECTS) { wrap(GameRoutes.HIDDEN_OBJECTS) { HiddenObjectsGameScreen(navController, starState) }() }
        composable(GameRoutes.SORTING) { wrap(GameRoutes.SORTING) { SortingGameScreen(navController, starState) }() }
        composable(GameRoutes.INSTRUMENTS) { wrap(GameRoutes.INSTRUMENTS) { InstrumentsGameScreen(navController, starState) }() }
        composable(GameRoutes.SEQUENCE) { wrap(GameRoutes.SEQUENCE) { SequenceMemoryGameScreen(navController, starState) }() }
        composable(GameRoutes.MATH) { wrap(GameRoutes.MATH) { MathGameScreen(navController, starState) }() }

        // Asigură-te că aceste ecrane există sau comentează-le dacă nu sunt implementate încă
        composable(GameRoutes.BLOCKS) { wrap(GameRoutes.BLOCKS) { com.example.educationalapp.BlocksGameScreen(navController, starState) }() }
        composable(GameRoutes.MAZE) { wrap(GameRoutes.MAZE) { com.example.educationalapp.MazeGameScreen(navController, starState) }() }

        // --- Shadow Match (upgradat 2026) ---
        composable(GameRoutes.SHADOW_MATCH) {
            wrap(GameRoutes.SHADOW_MATCH) {
                ShadowMatchGameScreen(
                    onBack = { backToGames(navController) }
                )
            }()
        }

        composable(GameRoutes.ANIMAL_SORTING) { wrap(GameRoutes.ANIMAL_SORTING) { com.example.educationalapp.AnimalSortingGameScreen(navController, starState) }() }
        composable(GameRoutes.CODING) { wrap(GameRoutes.CODING) { com.example.educationalapp.features.games.CodingGameScreen(navController, starState) }() }

        composable(GameRoutes.EGG_SURPRISE) { wrap(GameRoutes.EGG_SURPRISE) { EggGameScreen(onHome = { backToGames(navController) }) }() }
        composable(GameRoutes.FEED_MONSTER) { wrap(GameRoutes.FEED_MONSTER) { FeedGameScreen(onHome = { backToGames(navController) }) }() }

        // --- MENIURI SECUNDARE ---
        composable(Screen.SoundsMenu.route) { SoundsMenuScreen(navController) }
        composable(Screen.InstrumentsMenu.route) { InstrumentsMenuScreen(navController) }
        composable(Screen.StoriesMenu.route) { StoriesMenuScreen(navController) }
        composable(Screen.SongsMenu.route) { SongsMenuScreen(navController) }

        composable(Screen.WildSounds.route) { WildSoundsScreen() }
        composable(Screen.MarineSounds.route) { MarineSoundsScreen() }
        composable(Screen.FarmSounds.route) { FarmSoundsScreen() }
        composable(Screen.BirdSounds.route) { BirdSoundsScreen() }
        composable(Screen.VehicleSounds.route) { VehicleSoundsScreen() }

        composable(Screen.Song1.route) { backStackEntry -> SongPlayerScreen(navController, backStackEntry, starState) }
        composable(Screen.Song2.route) { backStackEntry -> SongPlayerScreen(navController, backStackEntry, starState) }
        composable(Screen.Song3.route) { backStackEntry -> SongPlayerScreen(navController, backStackEntry, starState) }
        composable(Screen.Song4.route) { backStackEntry -> SongPlayerScreen(navController, backStackEntry, starState) }
    }
}