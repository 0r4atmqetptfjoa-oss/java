package com.example.educationalapp.features.games

/**
 * Centralized route names for all mini-games.
 *
 * Why this exists:
 * - Prevents route collisions between module menus (e.g. "instruments" menu) and games.
 * - Eliminates brittle index-based lookups (gamesList[4], etc.).
 */
object GameRoutes {
    const val ALPHABET = "game_alphabet"
    const val ALPHABET_GRAPH = "game_alphabet_graph"
    const val ALPHABET_MENU = "game_alphabet_menu"
    const val ALPHABET_GAME = "game_alphabet_game"

    const val PEEKABOO = "game_peekaboo"
    const val COLORS = "game_colors"
    const val SHAPES = "game_shapes"
    const val PUZZLE = "game_puzzle"
    const val MEMORY = "game_memory"
    const val HIDDEN_OBJECTS = "game_hidden_objects"
    const val SORTING = "game_sorting"
    const val INSTRUMENTS = "game_instruments"
    const val SEQUENCE = "game_sequence"
    const val MATH = "game_math"
    const val CODING = "game_coding"
    const val BLOCKS = "game_blocks"
    const val COOKING = "game_cooking"
    const val MAZE = "game_maze"
    const val SHADOW_MATCH = "game_shadow_match"
    const val ANIMAL_SORTING = "game_animal_sorting"
    const val EMOTIONS = "game_emotions"
    const val INSTRUMENT_GUESS = "game_instrument_guess"

    // Imported/extra mini-games
    const val EGG_SURPRISE = "game_egg_surprise"
    const val FEED_MONSTER = "game_feed_monster"
    const val ANIMAL_BAND = "game_animal_band"
}
