package com.example.educationalapp.navigation

import kotlinx.serialization.Serializable

// --- Ecrane Principale ---
@Serializable object MainMenuRoute
@Serializable object GamesMenuRoute
@Serializable object SettingsRoute
@Serializable object PaywallRoute
@Serializable object ParentalGateRoute

// --- Meniuri Secundare ---
@Serializable object InstrumentsMenuRoute
@Serializable object SongsMenuRoute
@Serializable object StoriesMenuRoute
@Serializable object SoundsMenuRoute

// --- Sub-meniuri Sunete ---
@Serializable object WildSoundsRoute
@Serializable object MarineSoundsRoute
@Serializable object FarmSoundsRoute
@Serializable object BirdSoundsRoute
@Serializable object BirdSpritesRoute
@Serializable object VehicleSoundsRoute

// --- Rute pentru Cântece ---
@Serializable object Song1Route
@Serializable object Song2Route
@Serializable object Song3Route
@Serializable object Song4Route

// --- JOCURI ---
@Serializable object AlphabetQuizRoute
@Serializable object AlphabetGraphRoute // Pentru navigarea imbricată

@Serializable object PeekABooRoute
@Serializable object ColorsRoute
@Serializable object ShapesRoute
@Serializable object PuzzleRoute
@Serializable object CookingRoute
@Serializable object MagicGardenRoute
@Serializable object MemoryRoute
@Serializable object BalloonPopRoute
@Serializable object AnimalBandRoute
@Serializable object HiddenObjectsRoute
@Serializable object SortingRoute
@Serializable object InstrumentsGameRoute
@Serializable object SequenceRoute
@Serializable object MathRoute
@Serializable object BlocksRoute
@Serializable object MazeRoute
@Serializable object ShadowMatchRoute
@Serializable object AnimalSortingRoute
@Serializable object CodingRoute
@Serializable object EggSurpriseRoute
@Serializable object FeedMonsterRoute