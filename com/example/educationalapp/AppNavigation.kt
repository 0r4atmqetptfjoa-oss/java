package com.example.educationalapp

import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.educationalapp.navigation.* // Importă noile rute

// Importurile modulelor (rămân la fel)
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

// Funcție helper pentru a ne întoarce la meniul de jocuri
private fun backToGames(navController: NavController) {
    // Încercăm să scoatem totul până la GamesMenuRoute
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

    // Mapare opțională dacă încă folosești GameContainer pentru a arăta titluri/steluțe
    // Nota: Aici asociez Ruta (clasa) cu GameModel-ul, dacă e cazul.
    // Pentru simplitate, în acest exemplu folosesc direct GameContainer unde e nevoie.

    NavHost(
        navController = navController,
        startDestination = MainMenuRoute // Folosim obiectul, nu string-ul!
    ) {

        // --- MENIUL PRINCIPAL ---
        composable<MainMenuRoute> {
            MainMenuScreen(navController = navController, starCount = starCount)
        }

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

        composable<GamesMenuRoute> {
            GamesMenuScreen(navController = navController)
        }

        // Exemplu de sub-graf (Nested Navigation)
        navigation<AlphabetGraphRoute>(startDestination = AlphabetQuizRoute) {
            composable<AlphabetQuizRoute> {
                AlphabetGameScreen(
                    onBackToMenu = { backToGames(navController) }
                )
            }
        }

        // --- JOCURI STANDARD ---
        // Folosim GameContainer manual sau funcția wrap adaptată dacă vrei.
        // Aici am simplificat pentru claritate, dar poți reintroduce `wrap` dacă ai nevoie de logică extra.

        composable<PeekABooRoute> {
            PeekABooGame(onHome = { backToGames(navController) })
        }
        
        composable<ColorsRoute> { 
            ColorsGameScreen(onBack = { backToGames(navController) }) 
        }
        
        composable<ShapesRoute> { 
            ShapesGameScreen(onBack = { backToGames(navController) }) 
        }
        
        composable<PuzzleRoute> { 
            PuzzleGameScreen(onBack = { backToGames(navController) }) 
        }

        // --- JOCUL DE GĂTIT ---
        composable<CookingRoute> {
            CookingGameScreen(onBack = { backToGames(navController) })
        }

        // --- MAGIC GARDEN (2026) ---
        composable<MagicGardenRoute> {
            MagicGardenGameScreen(onBack = { backToGames(navController) })
        }

        // --- ALTE JOCURI ---
        composable<MemoryRoute> { MemoryGameScreen(onHome = { backToGames(navController) }) }
        composable<BalloonPopRoute> { BalloonGameScreen(onHome = { backToGames(navController) }) }
        composable<AnimalBandRoute> { AnimalBandGame(onHome = { backToGames(navController) }) }

        composable<HiddenObjectsRoute> { HiddenObjectsGameScreen(navController, starState) }
        composable<SortingRoute> { SortingGameScreen(navController, starState) }
        composable<InstrumentsGameRoute> { InstrumentsGameScreen(navController, starState) }
        composable<SequenceRoute> { SequenceMemoryGameScreen(navController, starState) }
        composable<MathRoute> { MathGameScreen(navController, starState) }

        // Placeholder pentru ecrane care poate nu sunt gata
        composable<BlocksRoute> { com.example.educationalapp.BlocksGameScreen(navController, starState) }
        composable<MazeRoute> { com.example.educationalapp.MazeGameScreen(navController, starState) }

        // --- Shadow Match (Upgradat 2026) ---
        composable<ShadowMatchRoute> {
            ShadowMatchGameScreen(onBack = { backToGames(navController) })
        }

        composable<AnimalSortingRoute> { com.example.educationalapp.AnimalSortingGameScreen(navController, starState) }
        composable<CodingRoute> { com.example.educationalapp.features.games.CodingGameScreen(navController, starState) }
        composable<EggSurpriseRoute> { EggGameScreen(onHome = { backToGames(navController) }) }
        composable<FeedMonsterRoute> { FeedGameScreen(onHome = { backToGames(navController) }) }

        // --- MENIURI SECUNDARE ---
        composable<SoundsMenuRoute> { SoundsMenuScreen(navController) }
        composable<InstrumentsMenuRoute> { InstrumentsMenuScreen(navController) }
        composable<StoriesMenuRoute> { StoriesMenuScreen(navController) }
        composable<SongsMenuRoute> { SongsMenuScreen(navController) }

        // --- SUNETE ---
        composable<WildSoundsRoute> { WildSoundsScreen() }
        composable<MarineSoundsRoute> { MarineSoundsScreen() }
        composable<FarmSoundsRoute> { FarmSoundsScreen() }
        composable<BirdSoundsRoute> { BirdSoundsScreen() }
        composable<VehicleSoundsRoute> { VehicleSoundsScreen() }

        // --- CÂNTECE ---
        // Aici `backStackEntry` nu mai este neapărat necesar pentru a extrage argumente, 
        // dar îl păstrăm dacă SongPlayerScreen are nevoie de el.
        composable<Song1Route> { entry -> SongPlayerScreen(navController, entry, starState) }
        composable<Song2Route> { entry -> SongPlayerScreen(navController, entry, starState) }
        composable<Song3Route> { entry -> SongPlayerScreen(navController, entry, starState) }
        composable<Song4Route> { entry -> SongPlayerScreen(navController, entry, starState) }
    }
}