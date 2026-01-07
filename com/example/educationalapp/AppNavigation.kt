package com.example.educationalapp

import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.educationalapp.navigation.* // Importăm noile rute obligatoriu

// Importurile modulelor tale
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
import com.example.educationalapp.BalloonGame.BalloonGameScreen
import com.example.educationalapp.MemoryGame.MemoryGameScreen
import com.example.educationalapp.AnimalBandGame.AnimalBandGame
import com.example.educationalapp.EggGame.EggGameScreen
import com.example.educationalapp.FeedGame.FeedGameScreen

// Helper pentru întoarcerea la meniul de jocuri folosind RUTELE NOI
private fun backToGames(navController: NavController) {
    // Folosim obiectul GamesMenuRoute, nu string-ul vechi
    val popped = navController.popBackStack<GamesMenuRoute>(inclusive = false)
    if (!popped) {
        navController.navigate(GamesMenuRoute) {
            popUpTo<MainMenuRoute>()
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

    // --- NAV HOST PRINCIPAL (Folosim Type-Safe Routes) ---
    NavHost(
        navController = navController,
        startDestination = MainMenuRoute // Pornim cu OBIECTUL MainMenuRoute
    ) {

        // 1. MENIUL PRINCIPAL
        composable<MainMenuRoute> {
            MainMenuScreen(navController = navController, starCount = starCount)
        }

        // 2. SETĂRI
        composable<SettingsRoute> {
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

        // 3. MENIUL DE JOCURI
        composable<GamesMenuRoute> {
            GamesMenuScreen(navController = navController)
        }

        // 4. ALFABET (Sub-graf)
        navigation<AlphabetGraphRoute>(startDestination = AlphabetQuizRoute) {
            composable<AlphabetQuizRoute> {
                AlphabetGameScreen(
                    onBackToMenu = { backToGames(navController) }
                )
            }
        }

        // 5. JOCURI INDIVIDUALE - Acum folosim composable<RutaObiect>
        composable<PeekABooRoute> { PeekABooGame(onHome = { backToGames(navController) }) }
        composable<ColorsRoute> { ColorsGameScreen(onBack = { backToGames(navController) }) }
        composable<ShapesRoute> { ShapesGameScreen(onBack = { backToGames(navController) }) }
        composable<PuzzleRoute> { PuzzleGameScreen(onBack = { backToGames(navController) }) }
        
        composable<CookingRoute> { 
            CookingGameScreen(onBack = { backToGames(navController) }) 
        }

        composable<MagicGardenRoute> {
            MagicGardenGameScreen(
                hasFullVersion = false, 
                onBack = { backToGames(navController) }
            )
        }

        composable<MemoryRoute> { MemoryGameScreen(onHome = { backToGames(navController) }) }
        composable<BalloonPopRoute> { BalloonGameScreen(onHome = { backToGames(navController) }) }
        composable<AnimalBandRoute> { AnimalBandGame(onHome = { backToGames(navController) }) }

        composable<HiddenObjectsRoute> { HiddenObjectsGameScreen(navController, starState) }
        composable<SortingRoute> { SortingGameScreen(navController, starState) }
        composable<InstrumentsGameRoute> { InstrumentsGameScreen(navController, starState) }
        composable<SequenceRoute> { SequenceMemoryGameScreen(navController, starState) }
        composable<MathRoute> { MathGameScreen(navController, starState) }

        // Jocuri în lucru (dacă nu ai șters fișierele vechi, ar trebui să meargă importurile)
        composable<BlocksRoute> { com.example.educationalapp.BlocksGameScreen(navController, starState) }
        composable<MazeRoute> { com.example.educationalapp.MazeGameScreen(navController, starState) }

        composable<ShadowMatchRoute> { 
            ShadowMatchGameScreen(onBack = { backToGames(navController) }) 
        }

        composable<AnimalSortingRoute> { com.example.educationalapp.AnimalSortingGameScreen(navController, starState) }
        composable<CodingRoute> { com.example.educationalapp.features.games.CodingGameScreen(navController, starState) }
        composable<EggSurpriseRoute> { EggGameScreen(onHome = { backToGames(navController) }) }
        composable<FeedMonsterRoute> { FeedGameScreen(onHome = { backToGames(navController) }) }

        // 6. MENIURI SECUNDARE
        composable<SoundsMenuRoute> { SoundsMenuScreen(navController) }
        composable<InstrumentsMenuRoute> { InstrumentsMenuScreen(navController) }
        composable<StoriesMenuRoute> { StoriesMenuScreen(navController) }
        composable<SongsMenuRoute> { SongsMenuScreen(navController) }

        // 7. ECRANE SUNETE
        composable<WildSoundsRoute> { WildSoundsScreen() }
        composable<MarineSoundsRoute> { MarineSoundsScreen() }
        composable<FarmSoundsRoute> { FarmSoundsScreen() }
        composable<BirdSoundsRoute> { BirdSoundsScreen() }
        composable<VehicleSoundsRoute> { VehicleSoundsScreen() }

        // 8. PLAYER MUZICAL
        composable<Song1Route> { backStackEntry -> SongPlayerScreen(navController, backStackEntry, starState) }
        composable<Song2Route> { backStackEntry -> SongPlayerScreen(navController, backStackEntry, starState) }
        composable<Song3Route> { backStackEntry -> SongPlayerScreen(navController, backStackEntry, starState) }
        composable<Song4Route> { backStackEntry -> SongPlayerScreen(navController, backStackEntry, starState) }
        
        // 9. ALTELE
        composable<PaywallRoute> { PaywallScreen(navController) }
    }
}