package com.example.educationalapp

import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.educationalapp.alphabet.AlphabetGameScreen
import com.example.educationalapp.alphabet.AlphabetMenuScreen
import com.example.educationalapp.colors.ColorsGameScreen
import com.example.educationalapp.features.games.*
import com.example.educationalapp.features.instruments.InstrumentsMenuScreen
import com.example.educationalapp.features.mainmenu.MainMenuScreen
import com.example.educationalapp.features.settings.SettingsScreen
import com.example.educationalapp.features.songs.SongsMenuScreen
import com.example.educationalapp.features.songs.SongPlayerScreen
import com.example.educationalapp.features.sounds.*
import com.example.educationalapp.features.stories.StoriesMenuScreen
import com.example.educationalapp.peekaboo.PeekABooGame
import com.example.educationalapp.puzzle.PuzzleGameScreen
import com.example.educationalapp.shapes.ShapesGameScreen

// IMPORTURI NOI:
import com.example.educationalapp.BalloonGame.BalloonGameScreen
import com.example.educationalapp.MemoryGame.MemoryGameScreen
// (Opțional: Egg, Feed, AnimalBand imports dacă nu sunt deja importate prin * )
import com.example.educationalapp.AnimalBandGame.AnimalBandGame
// import com.example.educationalapp.EggGame.EggGameScreen // asigură-te că există
// import com.example.educationalapp.FeedGame.FeedGameScreen // asigură-te că există

/**
 * Helper: încearcă să se întoarcă la GamesMenu dacă există în backstack,
 * altfel navighează acolo.
 */
private fun backToGames(navController: NavController) {
    val popped = navController.popBackStack(Screen.GamesMenu.route, false)
    if (!popped) {
        navController.navigate(Screen.GamesMenu.route)
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
            GamesCategoryScreen { selected ->
                navController.navigate(selected.destination)
            }
        }

        navigation(
            startDestination = GameRoutes.ALPHABET_MENU,
            route = GameRoutes.ALPHABET_GRAPH
        ) {
            composable(GameRoutes.ALPHABET_MENU) {
                AlphabetMenuScreen(
                    onPlayClick = { navController.navigate(GameRoutes.ALPHABET_QUIZ) },
                    onBackToHome = { navController.popBackStack(Screen.MainMenu.route, false) }
                )
            }
            composable(GameRoutes.ALPHABET_QUIZ) {
                AlphabetGameScreen(
                    onBackToMenu = { navController.popBackStack() }
                )
            }
        }

        // --- JOCURI STANDARD ---
        composable(GameRoutes.PEEKABOO) { wrap(GameRoutes.PEEKABOO) { PeekABooGame(onHome = { backToGames(navController) }) }() }
        composable(GameRoutes.COLORS) { wrap(GameRoutes.COLORS) { ColorsGameScreen(onBack = { backToGames(navController) }) }() }
        composable(GameRoutes.SHAPES) { wrap(GameRoutes.SHAPES) { ShapesGameScreen(onBack = { backToGames(navController) }) }() }
        composable(GameRoutes.PUZZLE) { wrap(GameRoutes.PUZZLE) { PuzzleGameScreen(onBack = { backToGames(navController) }) }() }

        // --- JOCURI ACTUALIZATE / NOI ---
        
        // Memory folosind noul MemoryGameScreen (MVVM)
        composable(GameRoutes.MEMORY) { 
            wrap(GameRoutes.MEMORY) { 
                MemoryGameScreen(
                    onHome = { backToGames(navController) }
                ) 
            }() 
        }

        // Balloon Pop (NOU)
        composable(GameRoutes.BALLOON_POP) {
            wrap(GameRoutes.BALLOON_POP) {
                BalloonGameScreen(
                    onHome = { backToGames(navController) }
                )
            }()
        }

        // Animal Band (Actualizat)
        composable(GameRoutes.ANIMAL_BAND) { 
            wrap(GameRoutes.ANIMAL_BAND) { 
                AnimalBandGame(
                    onHome = { backToGames(navController) }
                ) 
            }() 
        }

        // Restul jocurilor
        composable(GameRoutes.HIDDEN_OBJECTS) { wrap(GameRoutes.HIDDEN_OBJECTS) { HiddenObjectsGameScreen(navController, starState) }() }
        composable(GameRoutes.SORTING) { wrap(GameRoutes.SORTING) { SortingGameScreen(navController, starState) }() }
        composable(GameRoutes.INSTRUMENTS) { wrap(GameRoutes.INSTRUMENTS) { InstrumentsGameScreen(navController, starState) }() }
        composable(GameRoutes.SEQUENCE) { wrap(GameRoutes.SEQUENCE) { SequenceMemoryGameScreen(navController, starState) }() }
        composable(GameRoutes.MATH) { wrap(GameRoutes.MATH) { MathGameScreen(navController, starState) }() }

        // Placeholder pt jocurile Egg/Feed dacă nu sunt implementate complet, altfel lasă-le așa:
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

        composable(Screen.Song1.route) { backStackEntry -> SongPlayerScreen(navController, backStackEntry, starState) }
        composable(Screen.Song2.route) { backStackEntry -> SongPlayerScreen(navController, backStackEntry, starState) }
        composable(Screen.Song3.route) { backStackEntry -> SongPlayerScreen(navController, backStackEntry, starState) }
        composable(Screen.Song4.route) { backStackEntry -> SongPlayerScreen(navController, backStackEntry, starState) }
    }
}