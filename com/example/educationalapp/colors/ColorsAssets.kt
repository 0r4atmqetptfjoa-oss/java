package com.example.educationalapp.colors

import androidx.compose.ui.graphics.Color
import com.example.educationalapp.R

data class ColorItem(
    val id: String, 
    val name: String,
    val colorValue: Color,
    val balloonImageRes: Int, 
    val sadImageRes: Int,     
    val happyImageRes: Int    
)

object ColorsAssets {

    val items = listOf(
        // --- SET 1 ---
        ColorItem("rosu_inima", "Roșu", Color(0xFFE53935), R.drawable.balloon_red, R.drawable.char_heart_sad, R.drawable.char_heart_happy),
        ColorItem("verde_broasca", "Verde", Color(0xFF43A047), R.drawable.balloon_green, R.drawable.char_frog_sad, R.drawable.char_frog_happy),
        ColorItem("galben_soare", "Galben", Color(0xFFFFD600), R.drawable.balloon_yellow, R.drawable.char_sun_sad, R.drawable.char_sun_happy),
        ColorItem("albastru_nor", "Albastru", Color(0xFF2962FF), R.drawable.balloon_blue, R.drawable.char_cloud_sad, R.drawable.char_cloud_happy),
        ColorItem("mov_caracatita", "Mov", Color(0xFFAA00FF), R.drawable.balloon_purple, R.drawable.char_octopus_sad, R.drawable.char_octopus_happy),
        ColorItem("portocaliu_fruct", "Portocaliu", Color(0xFFFF6D00), R.drawable.balloon_orange, R.drawable.char_fruit_sad, R.drawable.char_fruit_happy),

        // --- SET 2 ---
        ColorItem("rosu_capsuna", "Roșu", Color(0xFFE53935), R.drawable.balloon_red, R.drawable.char_strawberry_sad, R.drawable.char_strawberry_happy),
        ColorItem("verde_cactus", "Verde", Color(0xFF43A047), R.drawable.balloon_green, R.drawable.char_cactus_sad, R.drawable.char_cactus_happy),
        ColorItem("galben_banana", "Galben", Color(0xFFFFD600), R.drawable.balloon_yellow, R.drawable.char_banana_sad, R.drawable.char_banana_happy),
        ColorItem("albastru_picatura", "Albastru", Color(0xFF2962FF), R.drawable.balloon_blue, R.drawable.char_drop_sad, R.drawable.char_drop_happy),
        ColorItem("mov_strugure", "Mov", Color(0xFFAA00FF), R.drawable.balloon_purple, R.drawable.char_grapes_sad, R.drawable.char_grapes_happy),
        ColorItem("portocaliu_morcov", "Portocaliu", Color(0xFFFF6D00), R.drawable.balloon_orange, R.drawable.char_carrot_sad, R.drawable.char_carrot_happy)
    )

    // Returnează lista completă pentru a o amesteca în ViewModel
    fun getAllItems(): List<ColorItem> {
        return items
    }

    // Returnează un item random dintr-o anumită culoare (pentru butoanele greșite)
    // Ex: Dacă am nevoie de un buton "Verde" greșit, îmi dă ori Broasca ori Cactusul.
    fun getRandomItemByColor(colorName: String): ColorItem {
        return items.filter { it.name == colorName }.random()
    }
    
    // Lista unică de nume de culori (pentru a ști ce lipsește)
    val allColorNames = listOf("Roșu", "Verde", "Galben", "Albastru", "Mov", "Portocaliu")
}