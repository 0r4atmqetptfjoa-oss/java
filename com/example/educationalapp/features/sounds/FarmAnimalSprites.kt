package com.example.educationalapp.features.sounds

data class FarmAnimalSpriteConfig(
    val name: String,
    val assetPath: String,
    val columns: Int,
    val frameCount: Int,
    val fps: Int
)

val farmAnimalSprites = listOf(
    FarmAnimalSpriteConfig("Cal", "menus/sounds/ferma/cal.webp", 6, 30, 30),
    FarmAnimalSpriteConfig("Oaie", "menus/sounds/ferma/oaie.webp", 6, 30, 30),
    FarmAnimalSpriteConfig("Porc", "menus/sounds/ferma/porc.webp", 6, 30, 30),
    FarmAnimalSpriteConfig("Rață", "menus/sounds/ferma/rata.webp", 6, 30, 30),
    FarmAnimalSpriteConfig("Vacă", "menus/sounds/ferma/vaca.webp", 6, 30, 30),
    FarmAnimalSpriteConfig("Câine", "menus/sounds/ferma/caine.webp", 6, 30, 30),
    FarmAnimalSpriteConfig("Capră", "menus/sounds/ferma/capra.webp", 6, 30, 30),
    FarmAnimalSpriteConfig("Găină", "menus/sounds/ferma/gaina.webp", 6, 30, 30),
    FarmAnimalSpriteConfig("Gâscă", "menus/sounds/ferma/gasca.webp", 6, 30, 30),
    FarmAnimalSpriteConfig("Măgar", "menus/sounds/ferma/magar.webp", 6, 30, 30),
    FarmAnimalSpriteConfig("Curcan", "menus/sounds/ferma/curcan.webp", 6, 30, 30),
    FarmAnimalSpriteConfig("Pisică", "menus/sounds/ferma/pisica.webp", 6, 30, 30)
)
