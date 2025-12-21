package com.example.educationalapp

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import kotlinx.coroutines.flow.distinctUntilChanged

// --- JOCURI REFACTORIZATE ---
import com.example.educationalapp.puzzle.PuzzleGameScreen
import com.example.educationalapp.AnimalBandGame.AnimalBandGame
import com.example.educationalapp.EggGame.EggGameScreen
import com.example.educationalapp.FeedGame.FeedGameScreen

// --- JOCURI SI ECRANE DIN PACHETE SPECIFICE ---
import com.example.educationalapp.alphabet.AlphabetGameScreen
import com.example.educationalapp.alphabet.AlphabetMenuScreen
import com.example.educationalapp.colors.ColorsGameScreen
import com.example.educationalapp.shapes.ShapesGameScreen
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

// --- NOTA: Ecranele de mai jos sunt in acelasi pachet sau in root, deci nu necesita import special
// MemoryGameScreen, HiddenObjectsGameScreen, etc.

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

    LaunchedEffect(Unit) {
        snapshotFlow { starState.value }
            .distinctUntilChanged()
            .collect { viewModel.setStarCount(it) }
    }

    val gameByRoute = remember { gamesList.associateBy { it.destination } }

    NavHost(
        navController = navController,
        startDestination = Screen.MainMenu.route,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(500)) + fadeIn(tween(500)) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(500)) + fadeOut(tween(500)) },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(500)) + fadeIn(tween(500)) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(500)) + fadeOut(tween(500)) }
    ) {
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
                if (selected.destination == GameRoutes.ALPHABET) {
                    navController.navigate(GameRoutes.ALPHABET_GRAPH) { launchSingleTop = true }
                } else {
                    navController.navigate(selected.destination) { launchSingleTop = true }
                }
            }
        }

        // --- REFACTORIZATE ---
        composable(GameRoutes.PUZZLE) { PuzzleGameScreen(onBack = { navController.popBackStack() }) }
        composable(GameRoutes.ANIMAL_BAND) { AnimalBandGame(onHome = { navController.popBackStack() }) }
        composable(GameRoutes.EGG_SURPRISE) { EggGameScreen(onHome = { navController.popBackStack() }) }
        composable(GameRoutes.FEED_MONSTER) { FeedGameScreen(onHome = { navController.popBackStack() }) }

        // --- EXISTENTE ---
        composable(GameRoutes.PEEKABOO) { PeekABooGame(onHome = { navController.popBackStack() }) }
        composable(GameRoutes.COLORS) { ColorsGameScreen(onBack = { navController.popBackStack() }) }
        composable(GameRoutes.SHAPES) { ShapesGameScreen(onBack = { navController.popBackStack() }) }
        
        composable(GameRoutes.CODING) { 
            val game = gameByRoute.getValue(GameRoutes.CODING)
            GameContainer(game) { CodingGameScreen(navController = navController, starState = starState) } 
        }
        
        composable(GameRoutes.SEQUENCE) { 
            val game = gameByRoute.getValue(GameRoutes.SEQUENCE)
            GameContainer(game) { SequenceMemoryGameScreen(navController = navController, starState = starState) } 
        }
        
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
        
        composable(GameRoutes.MATH) { 
            val game = gameByRoute.getValue(GameRoutes.MATH)
            GameContainer(game) { MathGameScreen(navController = navController, starState = starState) } 
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

        // ALPHABET
        navigation(startDestination = GameRoutes.ALPHABET_MENU, route = GameRoutes.ALPHABET_GRAPH) {
            composable(GameRoutes.ALPHABET_MENU) { AlphabetMenuScreen(onPlayClick = { navController.navigate(GameRoutes.ALPHABET_GAME) }, onBackToHome = { navController.popBackStack(Screen.MainMenu.route, inclusive = false) }) }
            composable(GameRoutes.ALPHABET_GAME) { AlphabetGameScreen(onBackToMenu = { navController.popBackStack() }) }
        }

        // MENIURI
        composable(Screen.SoundsMenu.route) { SoundsMenuScreen(navController) }
        composable(Screen.InstrumentsMenu.route) { InstrumentsMenuScreen(navController) }
        composable(Screen.StoriesMenu.route) { StoriesMenuScreen(navController) }
        composable(Screen.SongsMenu.route) { SongsMenuScreen(navController) }

        // ECRANE SUNETE
        composable(Screen.WildSounds.route) { WildSoundsScreen() }
        composable(Screen.MarineSounds.route) { MarineSoundsScreen() }
        composable(Screen.FarmSounds.route) { FarmSoundsScreen() }
        composable(Screen.BirdSounds.route) { BirdSoundsScreen() }

        // PLAYER MUZICAL
        composable(Screen.Song1.route) { SongPlayerScreen(navController = navController, backStackEntry = it, starState = starState) }
        composable(Screen.Song2.route) { SongPlayerScreen(navController = navController, backStackEntry = it, starState = starState) }
        composable(Screen.Song3.route) { SongPlayerScreen(navController = navController, backStackEntry = it, starState = starState) }
        composable(Screen.Song4.route) { SongPlayerScreen(navController = navController, backStackEntry = it, starState = starState) }
    }
}