package com.example.educationalapp

/**
 * Defines all of the navigation routes used throughout the application. Each route
 * corresponds to a specific screen. Additional game routes have been added to
 * support the new mini‑games implemented in this update.
 */
sealed class Screen(val route: String) {
    object MainMenu : Screen("main_menu")
    object GamesMenu : Screen("games")
    object InstrumentsMenu : Screen("instruments")
    object SongsMenu : Screen("songs")
    object SoundsMenu : Screen("sounds")
    object StoriesMenu : Screen("stories")
    object Paywall : Screen("paywall")
    object SettingsScreen : Screen("settings")

    // Sound routes
    object WildSounds : Screen("wild_sounds")
    object MarineSounds : Screen("marine_sounds")
    object FarmSounds : Screen("farm_sounds")
    object BirdSounds : Screen("bird_sounds")
    object VehicleSounds : Screen("vehicle_sounds")

    // Game routes
    object AlphabetQuiz : Screen("alphabet_quiz")
    object MathGame : Screen("math_game")
    object ColorMatch : Screen("color_match")
    object ShapeMatch : Screen("shape_match")
    object Puzzle : Screen("puzzle")
    object MemoryGame : Screen("memory_game")
    object AnimalSortingGame : Screen("animal_sorting_game")
    object CookingGame : Screen("cooking_game")
    object InstrumentsGame : Screen("instruments_game")
    object BlocksGame : Screen("blocks_game")
    object MazeGame : Screen("maze_game")
    // Newly added games
    object HiddenObjectsGame : Screen("hidden_objects_game")
    object SortingGame : Screen("sorting_game")
    object ShadowMatchGame : Screen("shadow_match_game")

    // New educational coding game: guide a robot through a maze using command sequences
    object CodingGame : Screen("coding_game")

    // Sequence memory game where players repeat increasingly long sequences of colours
    object SequenceMemoryGame : Screen("sequence_memory_game")

    // Story routes
    object StoryBook : Screen("story_book")

    // Song routes – individual song screens
    object Song1 : Screen("song1")
    object Song2 : Screen("song2")
    object Song3 : Screen("song3")
    object Song4 : Screen("song4")

    // Other routes
    object ParentalGate : Screen("parental_gate")
}