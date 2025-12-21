package com.example.educationalapp

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.educationalapp.alphabet.AlphabetGameScreen
import com.example.educationalapp.alphabet.AlphabetMenuScreen
import com.example.educationalapp.features.games.*
import com.example.educationalapp.features.instruments.InstrumentsMenuScreen
import com.example.educationalapp.features.mainmenu.MainMenuScreen
import com.example.educationalapp.features.songs.SongsMenuScreen
import com.example.educationalapp.features.songs.SongPlayerScreen
import com.example.educationalapp.features.sounds.*
import com.example.educationalapp.features.stories.StoriesMenuScreen

// >>> IMPORTURI JOCURI NOI <<<
import com.example.educationalapp.colors.ColorsGameScreen 
import com.example.educationalapp.shapes.ShapesGameScreen
import com.example.educationalapp.puzzle.PuzzleGameScreen
import com.example.educationalapp.peekaboo.PeekABooGame // Importul pentru jocul nou

object Routes {
    const val ALPHABET_GRAPH = "alphabet_graph"
    const val ALPHABET_MENU = "alphabet_menu"
    const val ALPHABET_GAME = "alphabet_game"
}

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
    
    LaunchedEffect(starCount) {
        if (starState.value != starCount) {
            starState.value = starCount
        }
    }
    
    LaunchedEffect(starState.value) {
        viewModel.setStarCount(starState.value)
    }

    NavHost(navController = navController, startDestination = Screen.MainMenu.route) {
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

        composable("games") {
            GamesCategoryScreen { selected ->
                if (selected.destination == "alphabet") {
                    navController.navigate(Routes.ALPHABET_GRAPH)
                } else {
                    navController.navigate(selected.destination)
                }
            }
        }

        // --- NAVIGATION FOR ALPHABET GAME ---
        navigation(startDestination = Routes.ALPHABET_MENU, route = Routes.ALPHABET_GRAPH) {
            composable(Routes.ALPHABET_MENU) {
                AlphabetMenuScreen(
                    onPlayClick = { navController.navigate(Routes.ALPHABET_GAME) },
                    onBackToHome = { navController.popBackStack(Screen.MainMenu.route, false) }
                )
            }
            composable(Routes.ALPHABET_GAME) {
                AlphabetGameScreen(
                    onBackToMenu = { navController.popBackStack() }
                )
            }
        }

        // >>> JOCURILE NOI <<<

        composable("peekaboo") {
            // Nu folosim GameContainer aici neapărat pentru că PeekABoo are fundalul lui propriu integrat
            PeekABooGame(onHome = { navController.popBackStack() })
        }

        composable("colors") {
            ColorsGameScreen(onBack = { navController.popBackStack() })
        }

        composable("shapes") {
            ShapesGameScreen(onBack = { navController.popBackStack() })
        }

        composable("puzzle") {
            PuzzleGameScreen(onBack = { navController.popBackStack() })
        }

        // Restul jocurilor
        composable("memory") { GameContainer(gamesList[4]) { MemoryGameScreen(navController = navController, starState = starState) } }
        composable("hidden") { GameContainer(gamesList[5]) { HiddenObjectsGameScreen(navController = navController, starState = starState) } }
        composable("sorting") { GameContainer(gamesList[6]) { SortingGameScreen(navController = navController, starState = starState) } }
        composable("instruments") { GameContainer(gamesList[7]) { InstrumentsGameScreen(navController = navController, starState = starState) } }
        composable("sequence") { GameContainer(gamesList[9]) { SequenceMemoryGameScreen(navController = navController, starState = starState) } }
        composable("math") { GameContainer(gamesList[10]) { MathGameScreen(navController = navController, starState = starState) } }

        // Menu screens
        composable(Screen.SoundsMenu.route) { SoundsMenuScreen(navController) }
        composable(Screen.InstrumentsMenu.route) { InstrumentsMenuScreen(navController) }
        composable(Screen.StoriesMenu.route) { StoriesMenuScreen(navController) }
        composable(Screen.SongsMenu.route) { SongsMenuScreen(navController) }

        // Individual sound screens
        composable(Screen.WildSounds.route) { WildSoundsScreen() }
        composable(Screen.MarineSounds.route) { MarineSoundsScreen() }
        composable(Screen.FarmSounds.route) { FarmSoundsScreen() }
        composable(Screen.BirdSounds.route) { BirdSoundsScreen() }

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