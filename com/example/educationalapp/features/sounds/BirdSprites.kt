package com.example.educationalapp.features.sounds

data class BirdSpriteConfig(
    val name: String,
    val assetPath: String,
    val columns: Int,
    val frameCount: Int,
    val fps: Int
)

val birdSprites = listOf(
    BirdSpriteConfig("Corb", "menus/sounds/pasari/corb.webp", 6, 30, 30),
    BirdSpriteConfig("Păun", "menus/sounds/pasari/paun.webp", 6, 30, 30),
    BirdSpriteConfig("Cioară", "menus/sounds/pasari/cioara.webp", 6, 30, 30),
    BirdSpriteConfig("Vultur", "menus/sounds/pasari/vultur.webp", 6, 30, 30),
    BirdSpriteConfig("Bufniță", "menus/sounds/pasari/bufnita.webp", 6, 30, 30),
    BirdSpriteConfig("Papagal", "menus/sounds/pasari/papagal.webp", 6, 30, 30),
    BirdSpriteConfig("Pescăruș", "menus/sounds/pasari/Pescaruș.webp", 6, 30, 30),
    BirdSpriteConfig("Porumbel", "menus/sounds/pasari/porumbel.webp", 6, 30, 30),
    BirdSpriteConfig("Rândunică", "menus/sounds/pasari/randunica.webp", 6, 30, 30),
    BirdSpriteConfig("Ciocănitoare", "menus/sounds/pasari/ciocanitoare.webp", 6, 30, 30)
)
