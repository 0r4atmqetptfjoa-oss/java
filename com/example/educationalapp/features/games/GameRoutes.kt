package com.example.educationalapp.features.games

/**
 * Rute centralizate pentru mini-jocuri.
 * IMPORTANT: Valorile de aici trebuie să fie IDENTICE cu cele din Screen.kt
 * pentru ca navigarea din Meniu să funcționeze corect.
 */
object GameRoutes {
    // Graph-uri
    const val ALPHABET_GRAPH = "game_alphabet_graph"

    // Jocuri
    const val ALPHABET_QUIZ = "alphabet_quiz" // Era "game_alphabet_quiz"
    const val MATH = "math_game"              // Era "game_math"
    const val COLORS = "color_match"          // Era "game_colors"
    const val SHAPES = "shape_match"          // Era "game_shapes"
    const val PUZZLE = "puzzle"               // Era "game_puzzle"
    const val MEMORY = "memory_game"          // Era "game_memory"
    const val ANIMAL_SORTING = "animal_sorting_game"
    
    // FIXAT: Acum coincide cu Screen.CookingGame.route ("cooking_game")
    const val COOKING = "cooking_game"        
    
    const val INSTRUMENTS = "instruments_game"
    const val BLOCKS = "blocks_game"
    const val MAZE = "maze_game"
    const val HIDDEN_OBJECTS = "hidden_objects_game"
    const val SORTING = "sorting_game"
    const val SHADOW_MATCH = "shadow_match_game"
    const val CODING = "coding_game"
    const val SEQUENCE = "sequence_memory_game"

    // Jocuri Extra / Noi (acestea nu par să fie în Screen.kt, deci le lăsăm așa)
    const val PEEKABOO = "game_peekaboo" 
    const val EGG_SURPRISE = "game_egg_surprise"
    const val FEED_MONSTER = "game_feed_monster"
    const val ANIMAL_BAND = "game_animal_band"
    const val BALLOON_POP = "game_balloon_pop"
}