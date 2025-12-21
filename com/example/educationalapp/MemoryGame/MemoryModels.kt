package com.example.educationalapp.MemoryGame

data class MemoryCard(
    val id: Int,
    val imageRes: Int,
    val isFlipped: Boolean = false,
    val isMatched: Boolean = false
)