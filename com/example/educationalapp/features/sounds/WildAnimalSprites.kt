
package com.example.educationalapp.features.sounds

data class WildAnimalSpriteConfig(
    val name: String,
    val assetPath: String,
    val columns: Int,
    val frameCount: Int,
    val fps: Int
)

val wildAnimalSprites = listOf(
    WildAnimalSpriteConfig("Elefant", "menus/sounds/salbatice/elefant_sheet.webp", 6, 30, 30),
    WildAnimalSpriteConfig("Girafă", "menus/sounds/salbatice/girafa_sheet.webp", 6, 30, 30),
    WildAnimalSpriteConfig("Gorilă", "menus/sounds/salbatice/gorila_sheet.webp", 6, 30, 30),
    WildAnimalSpriteConfig("Hienă", "menus/sounds/salbatice/hiena_sheet.webp", 6, 30, 30),
    WildAnimalSpriteConfig("Koala", "menus/sounds/salbatice/koala_sheet.webp", 6, 30, 30),
    WildAnimalSpriteConfig("Leopard", "menus/sounds/salbatice/leopard_sheet.webp", 6, 30, 30),
    WildAnimalSpriteConfig("Leu", "menus/sounds/salbatice/leu_sheet.webp", 6, 30, 30),
    WildAnimalSpriteConfig("Porc Mistreț", "menus/sounds/salbatice/porc mistret_sheet.webp", 6, 30, 30),
    WildAnimalSpriteConfig("Rinocer", "menus/sounds/salbatice/rinocer_sheet.webp", 6, 30, 30),
    WildAnimalSpriteConfig("Tigru", "menus/sounds/salbatice/tigru_sheet.webp", 6, 30, 30),
    WildAnimalSpriteConfig("Urs", "menus/sounds/salbatice/urs_sheet.webp", 6, 30, 30),
    WildAnimalSpriteConfig("Vulpe", "menus/sounds/salbatice/vulpe_sheet.webp", 6, 30, 30),
    WildAnimalSpriteConfig("Zebră", "menus/sounds/salbatice/zebra_sheet.webp", 6, 30, 30)
)
