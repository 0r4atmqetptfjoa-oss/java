package com.example.educationalapp.puzzle

import com.example.educationalapp.R

data class PuzzleTheme(
    val id: String,
    val resId: Int // Imaginea completă
)

object PuzzleAssets {
    // Lista cu toate puzzle-urile disponibile
    val themes = listOf(
        PuzzleTheme("forest", R.drawable.puzzle_forest),
        PuzzleTheme("space", R.drawable.puzzle_space),
        PuzzleTheme("ocean", R.drawable.puzzle_ocean),
        PuzzleTheme("dino", R.drawable.puzzle_dino),
        PuzzleTheme("city", R.drawable.puzzle_city),
        PuzzleTheme("unicorn", R.drawable.puzzle_unicorn),
        PuzzleTheme("fruits", R.drawable.puzzle_fruits)
    )

    fun getRandomTheme(): PuzzleTheme {
        return themes.random()
    }
}