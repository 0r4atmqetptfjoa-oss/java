package com.example.educationalapp.data

/**
 * Config centralizat pentru structura de asset‑uri pe jocuri / ecrane.
 *
 * AICI NU se încarcă resursele, doar se definesc căile și convențiile
 * astfel încât să știi exact unde să pui fișierele în `src/main/assets`.
 *
 * Exemplu de acces dintr-un ViewModel / Composable:
 *
 * val basePath = GameAssets.byRoute(Screen.AlphabetQuiz.route).backgroundDir
 * val fullPath = "$basePath/bg_alphabet_quiz_1920x1080.png"
 * context.assets.open(fullPath) // decodezi manual imaginea dacă vrei să folosești assets în loc de R.drawable
 */
object GameAssets {

    data class Config(
        val route: String,
        val backgroundDir: String,
        val spriteDir: String,
        val sfxDir: String,
        val voiceDir: String,
        val uiDir: String
    )

    private val allConfigs: List<Config> = listOf(
        Config(
            route = "alphabet_quiz",
            backgroundDir = "games/alphabet_quiz/backgrounds",
            spriteDir = "games/alphabet_quiz/sprites",
            sfxDir = "games/alphabet_quiz/audio/sfx",
            voiceDir = "games/alphabet_quiz/audio/voice",
            uiDir = "games/alphabet_quiz/ui"
        ),
        Config(
            route = "math_game",
            backgroundDir = "games/math_game/backgrounds",
            spriteDir = "games/math_game/sprites",
            sfxDir = "games/math_game/audio/sfx",
            voiceDir = "games/math_game/audio/voice",
            uiDir = "games/math_game/ui"
        ),
        Config(
            route = "color_match",
            backgroundDir = "games/color_match/backgrounds",
            spriteDir = "games/color_match/sprites",
            sfxDir = "games/color_match/audio/sfx",
            voiceDir = "games/color_match/audio/voice",
            uiDir = "games/color_match/ui"
        ),
        Config(
            route = "shape_match",
            backgroundDir = "games/shape_match/backgrounds",
            spriteDir = "games/shape_match/sprites",
            sfxDir = "games/shape_match/audio/sfx",
            voiceDir = "games/shape_match/audio/voice",
            uiDir = "games/shape_match/ui"
        ),
        Config(
            route = "puzzle",
            backgroundDir = "games/puzzle/backgrounds",
            spriteDir = "games/puzzle/sprites",
            sfxDir = "games/puzzle/audio/sfx",
            voiceDir = "games/puzzle/audio/voice",
            uiDir = "games/puzzle/ui"
        ),
        Config(
            route = "memory_game",
            backgroundDir = "games/memory_game/backgrounds",
            spriteDir = "games/memory_game/sprites",
            sfxDir = "games/memory_game/audio/sfx",
            voiceDir = "games/memory_game/audio/voice",
            uiDir = "games/memory_game/ui"
        ),
        Config(
            route = "animal_sorting_game",
            backgroundDir = "games/animal_sorting_game/backgrounds",
            spriteDir = "games/animal_sorting_game/sprites",
            sfxDir = "games/animal_sorting_game/audio/sfx",
            voiceDir = "games/animal_sorting_game/audio/voice",
            uiDir = "games/animal_sorting_game/ui"
        ),
        Config(
            route = "cooking_game",
            backgroundDir = "games/cooking_game/backgrounds",
            spriteDir = "games/cooking_game/sprites",
            sfxDir = "games/cooking_game/audio/sfx",
            voiceDir = "games/cooking_game/audio/voice",
            uiDir = "games/cooking_game/ui"
        ),
        Config(
            route = "instruments_game",
            backgroundDir = "games/instruments_game/backgrounds",
            spriteDir = "games/instruments_game/sprites",
            sfxDir = "games/instruments_game/audio/sfx",
            voiceDir = "games/instruments_game/audio/voice",
            uiDir = "games/instruments_game/ui"
        ),
        Config(
            route = "blocks_game",
            backgroundDir = "games/blocks_game/backgrounds",
            spriteDir = "games/blocks_game/sprites",
            sfxDir = "games/blocks_game/audio/sfx",
            voiceDir = "games/blocks_game/audio/voice",
            uiDir = "games/blocks_game/ui"
        ),
        Config(
            route = "maze_game",
            backgroundDir = "games/maze_game/backgrounds",
            spriteDir = "games/maze_game/sprites",
            sfxDir = "games/maze_game/audio/sfx",
            voiceDir = "games/maze_game/audio/voice",
            uiDir = "games/maze_game/ui"
        )
    )

    /**
     * Obții config-ul unui joc după route (ex: "alphabet_quiz").
     * Dacă nu există, aruncă excepție – mai bine afli la dezvoltare decât să fie null în runtime.
     */
    fun byRoute(route: String): Config =
        allConfigs.firstOrNull { it.route == route }
            ?: error("Nu există GameAssets.Config pentru route=$route")
}
