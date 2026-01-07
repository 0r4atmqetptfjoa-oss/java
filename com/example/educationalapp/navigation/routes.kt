package com.example.educationalapp.navigation

import kotlinx.serialization.Serializable

/**
 * Definește toate rutele aplicației folosind Type-Safe Navigation.
 * Adio String-uri, bun venit Obiecte!
 */

// --- Ecrane Principale ---
@Serializable object MainMenuRoute
@Serializable object GamesMenuRoute
@Serializable object SettingsRoute
@Serializable object PaywallRoute

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
@Serializable object VehicleSoundsRoute

// --- Rute pentru Cântece Individuale (putem transmite și parametri dacă vrei pe viitor) ---
@Serializable object Song1Route
@Serializable object Song2Route
@Serializable object Song3Route
@Serializable object Song4Route

// --- JOCURI (Toate jocurile tale minunate) ---
@Serializable object AlphabetQuizRoute
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

// --- Graf-uri de Navigare (pentru grupuri de ecrane) ---
@Serializable object AlphabetGraphRoute