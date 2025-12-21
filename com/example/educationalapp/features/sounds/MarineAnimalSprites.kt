package com.example.educationalapp.features.sounds

data class MarineAnimalSpriteConfig(
    val name: String,
    val assetPath: String,
    val columns: Int,
    val frameCount: Int,
    val fps: Int
)

val marineAnimalSprites = listOf(
    MarineAnimalSpriteConfig("Crab", "menus/sounds/marine/Crab.webp", 6, 30, 30),
    MarineAnimalSpriteConfig("Foca", "menus/sounds/marine/Foca.webp", 6, 30, 30),
    MarineAnimalSpriteConfig("Orca", "menus/sounds/marine/orca.webp", 6, 30, 30),
    MarineAnimalSpriteConfig("Balenă", "menus/sounds/marine/balena.webp", 6, 30, 30),
    MarineAnimalSpriteConfig("Delfin", "menus/sounds/marine/delfin.webp", 6, 30, 30),
    MarineAnimalSpriteConfig("Meduză", "menus/sounds/marine/meduza.webp", 6, 30, 30),
    MarineAnimalSpriteConfig("Rechin", "menus/sounds/marine/rechin.webp", 6, 30, 30),
    MarineAnimalSpriteConfig("Anghilă", "menus/sounds/marine/Anghila.webp", 6, 30, 30),
    MarineAnimalSpriteConfig("Calamar", "menus/sounds/marine/Calamar.webp", 6, 30, 30),
    MarineAnimalSpriteConfig("Crevete", "menus/sounds/marine/crevete.webp", 6, 30, 30),
    MarineAnimalSpriteConfig("Pescăruș", "menus/sounds/pasari/Pescaruș.webp", 6, 30, 30),
    MarineAnimalSpriteConfig("Caracatiță", "menus/sounds/marine/caracatita.webp", 6, 30, 30),
    MarineAnimalSpriteConfig("Lamantin", "menus/sounds/marine/Lamantinul.webp", 6, 30, 30),
    MarineAnimalSpriteConfig("Pește Spadă", "menus/sounds/marine/peste spada.webp", 6, 30, 30),
    MarineAnimalSpriteConfig("Arici de Mare", "menus/sounds/marine/arici de mare.webp", 6, 30, 30),
    MarineAnimalSpriteConfig("Căluț de Mare", "menus/sounds/marine/calut de mare.webp", 6, 30, 30),
    MarineAnimalSpriteConfig("Broască Țestoasă", "menus/sounds/marine/broasca testoasa.webp", 6, 30, 30)
)
