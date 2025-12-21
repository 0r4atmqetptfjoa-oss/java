package com.example.educationalapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.educationalapp.alphabet.AlphabetGameScreen
import com.example.educationalapp.alphabet.AlphabetMenuScreen
import com.example.educationalapp.colors.ColorsGameScreen
import com.example.educationalapp.features.games.CodingGameScreen
import com.example.educationalapp.features.games.GameContainer
import com.example.educationalapp.features.games.GameRoutes
import com.example.educationalapp.features.games.GamesCategoryScreen
import com.example.educationalapp.features.games.SequenceMemoryGameScreen
import com.example.educationalapp.features.games.gamesList
import com.example.educationalapp.features.instruments.InstrumentsMenuScreen
import com.example.educationalapp.features.mainmenu.MainMenuScreen
import com.example.educationalapp.features.songs.SongsMenuScreen
import com.example.educationalapp.features.songs.SongPlayerScreen
import com.example.educationalapp.features.sounds.BirdSoundsScreen
import com.example.educationalapp.features.sounds.FarmSoundsScreen
import com.example.educationalapp.features.sounds.MarineSoundsScreen
import com.example.educationalapp.features.sounds.SoundsMenuScreen
import com.example.educationalapp.features.sounds.WildSoundsScreen
import com.example.educationalapp.features.stories.StoriesMenuScreen
import com.example.educationalapp.peekaboo.PeekABooGame
import com.example.educationalapp.puzzle.PuzzleGameScreen
import com.example.educationalapp.shapes.ShapesGameScreen
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()

    // Collect state from the ViewModel
    val starCount by viewModel.starCount.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val musicEnabled by viewModel.musicEnabled.collectAsState()
    val hardModeEnabled by viewModel.hardModeEnabled.collectAsState()

    // Local star state used by games.
    val starState = remember { mutableStateOf(starCount) }

    // One-way sync: ViewModel -> local state
    LaunchedEffect(starCount) {
        if (starState.value != starCount) starState.value = starCount
    }

    // One-way sync: local state -> ViewModel (only when changed)
    LaunchedEffect(Unit) {
        snapshotFlow { starState.value }
            .distinctUntilChanged()
            .collect { viewModel.setStarCount(it) }
    }

    val gameByRoute = remember { gamesList.associateBy { it.destination } }

    NavHost(
        navController = navController,
        startDestination = Screen.MainMenu.route
    ) {
        composable(Screen.MainMenu.route) {
            MainMenuScreen(
                navController = navController,
                starCount = starCount
            )
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

        // --- Games menu ---
        composable(Screen.GamesMenu.route) {
            GamesCategoryScreen { selected ->
                if (selected.destination == GameRoutes.ALPHABET) {
                    navController.navigate(GameRoutes.ALPHABET_GRAPH) { launchSingleTop = true }
                } else {
                    navController.navigate(selected.destination) { launchSingleTop = true }
                }
            }
        }

        // --- Alphabet game (nested graph) ---
        navigation(startDestination = GameRoutes.ALPHABET_MENU, route = GameRoutes.ALPHABET_GRAPH) {
            composable(GameRoutes.ALPHABET_MENU) {
                AlphabetMenuScreen(
                    onPlayClick = { navController.navigate(GameRoutes.ALPHABET_GAME) },
                    onBackToHome = { navController.popBackStack(Screen.MainMenu.route, inclusive = false) }
                )
            }
            composable(GameRoutes.ALPHABET_GAME) {
                AlphabetGameScreen(onBackToMenu = { navController.popBackStack() })
            }
        }

        // --- Games ---
        composable(GameRoutes.PEEKABOO) {
            // Peek-a-Boo handles its own background.
            PeekABooGame(onHome = { navController.popBackStack() })
        }

        composable(GameRoutes.COLORS) { ColorsGameScreen(onBack = { navController.popBackStack() }) }
        composable(GameRoutes.SHAPES) { ShapesGameScreen(onBack = { navController.popBackStack() }) }
        composable(GameRoutes.PUZZLE) { PuzzleGameScreen(onBack = { navController.popBackStack() }) }

        composable(GameRoutes.MEMORY) {
            val game = gameByRoute.getValue(GameRoutes.MEMORY)
            GameContainer(game) { MemoryGameScreen(navController = navController, starState = starState) }
        }

        composable(GameRoutes.HIDDEN_OBJECTS) {
            val game = gameByRoute.getValue(GameRoutes.HIDDEN_OBJECTS)
            GameContainer(game) { HiddenObjectsGameScreen(navController = navController, starState = starState) }
        }

        composable(GameRoutes.SORTING) {
            val game = gameByRoute.getValue(GameRoutes.SORTING)
            GameContainer(game) { SortingGameScreen(navController = navController, starState = starState) }
        }

        composable(GameRoutes.SEQUENCE) {
            val game = gameByRoute.getValue(GameRoutes.SEQUENCE)
            GameContainer(game) { SequenceMemoryGameScreen(navController = navController, starState = starState) }
        }

        composable(GameRoutes.MATH) {
            val game = gameByRoute.getValue(GameRoutes.MATH)
            GameContainer(game) { MathGameScreen(navController = navController, starState = starState) }
        }

        composable(GameRoutes.CODING) {
            val game = gameByRoute.getValue(GameRoutes.CODING)
            GameContainer(game) { CodingGameScreen(navController = navController, starState = starState) }
        }

        composable(GameRoutes.MAZE) {
            val game = gameByRoute.getValue(GameRoutes.MAZE)
            GameContainer(game) { MazeGameScreen(navController = navController, starState = starState) }
        }

        composable(GameRoutes.BLOCKS) {
            val game = gameByRoute.getValue(GameRoutes.BLOCKS)
            GameContainer(game) { BlocksGameScreen(navController = navController, starState = starState) }
        }

        composable(GameRoutes.COOKING) {
            val game = gameByRoute.getValue(GameRoutes.COOKING)
            GameContainer(game) { CookingGameScreen(navController = navController, starState = starState) }
        }

        composable(GameRoutes.SHADOW_MATCH) {
            val game = gameByRoute.getValue(GameRoutes.SHADOW_MATCH)
            GameContainer(game) { ShadowMatchGameScreen(navController = navController, starState = starState) }
        }

        composable(GameRoutes.ANIMAL_SORTING) {
            val game = gameByRoute.getValue(GameRoutes.ANIMAL_SORTING)
            GameContainer(game) { AnimalSortingGameScreen(navController = navController, starState = starState) }
        }

        composable(GameRoutes.EMOTIONS) {
            val game = gameByRoute.getValue(GameRoutes.EMOTIONS)
            GameContainer(game) { EmotionsGameScreen(navController = navController, starState = starState) }
        }

        composable(GameRoutes.INSTRUMENTS) {
            val game = gameByRoute.getValue(GameRoutes.INSTRUMENTS)
            GameContainer(game) { InstrumentsGameScreen(navController = navController, starState = starState) }
        }

        // Imported games (were in repo, but not wired in navigation)
        composable(GameRoutes.EGG_SURPRISE) {
            EggSurpriseGame(onHome = { navController.popBackStack() })
        }
        composable(GameRoutes.FEED_MONSTER) {
            FeedMonsterGame(onHome = { navController.popBackStack() })
        }
        composable(GameRoutes.ANIMAL_BAND) {
            AnimalBandGame(onHome = { navController.popBackStack() })
        }

        // --- Menu screens ---
        composable(Screen.SoundsMenu.route) { SoundsMenuScreen(navController) }
        composable(Screen.InstrumentsMenu.route) { InstrumentsMenuScreen(navController) }
        composable(Screen.StoriesMenu.route) { StoriesMenuScreen(navController) }
        composable(Screen.SongsMenu.route) { SongsMenuScreen(navController) }

        // --- Individual sound screens ---
        composable(Screen.WildSounds.route) { WildSoundsScreen() }
        composable(Screen.MarineSounds.route) { MarineSoundsScreen() }
        composable(Screen.FarmSounds.route) { FarmSoundsScreen() }
        composable(Screen.BirdSounds.route) { BirdSoundsScreen() }

        // --- Song player screens ---
        composable(Screen.Song1.route) { backStackEntry ->
            SongPlayerScreen(navController = navController, backStackEntry = backStackEntry, starState = starState)
        }
        composable(Screen.Song2.route) { backStackEntry ->
            SongPlayerScreen(navController = navController, backStackEntry = backStackEntry, starState = starState)
        }
        composable(Screen.Song3.route) { backStackEntry ->
            SongPlayerScreen(navController = navController, backStackEntry = backStackEntry, starState = starState)
        }
        composable(Screen.Song4.route) { backStackEntry ->
            SongPlayerScreen(navController = navController, backStackEntry = backStackEntry, starState = starState)
        }
    }
}