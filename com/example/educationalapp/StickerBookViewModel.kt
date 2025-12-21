package com.example.educationalapp

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

data class Sticker(val name: String, val emoji: String, val requiredStars: Int)

class StickerBookViewModel : ViewModel() {

    val stickers = listOf(
        Sticker("Stea", "⭐", 0),
        Sticker("Cățel", "🐶", 2),
        Sticker("Pisică", "🐱", 4),
        Sticker("Mașină", "🚗", 6),
        Sticker("Măr", "🍎", 8),
        Sticker("Balon", "🎈", 10),
        Sticker("Muzică", "🎵", 12),
        Sticker("Curcubeu", "🌈", 15)
    )

    val feedback = mutableStateOf("")

    fun onStickerClick(sticker: Sticker, currentStars: Int) {
        if (currentStars >= sticker.requiredStars) {
            feedback.value = "Ai selectat stickerul ${sticker.name}!"
        } else {
            feedback.value = "Stickerul ${sticker.name} este blocat. Obține ${sticker.requiredStars} stele."
        }
    }
}