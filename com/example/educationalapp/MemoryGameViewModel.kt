package com.example.educationalapp

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MemoryGameViewModel : ViewModel() {

    private val icons = listOf(
        "🐶", "🐱", "🐭", "🐹", "🐰", "🦊"
    )

    var cards by mutableStateOf(createShuffledCards(icons))
    var selectedCards by mutableStateOf<List<Int>>(emptyList())
    var moves by mutableStateOf(0)
    var isGameOver by mutableStateOf(false)

    fun resetGame() {
        cards = createShuffledCards(icons)
        selectedCards = emptyList()
        moves = 0
        isGameOver = false
    }

    fun onCardClicked(cardId: Int, onGameWon: (stars: Int) -> Unit) {
        if (selectedCards.size == 2 || cards.find { it.id == cardId }?.isFaceUp == true) return

        val newCards = cards.map {
            if (it.id == cardId) it.copy(isFaceUp = true) else it
        }
        cards = newCards
        selectedCards = selectedCards + cardId

        if (selectedCards.size == 2) {
            moves++
            val (firstCardId, secondCardId) = selectedCards
            val firstCard = cards.find { it.id == firstCardId }!!
            val secondCard = cards.find { it.id == secondCardId }!!

            if (firstCard.value == secondCard.value) {
                val matchedCards = cards.map {
                    if (it.id == firstCardId || it.id == secondCardId) it.copy(isMatched = true) else it
                }
                cards = matchedCards
                selectedCards = emptyList()

                if (cards.all { it.isMatched }) {
                    onGameWon(cards.size / 2) // Award stars
                    isGameOver = true
                }
            } else {
                viewModelScope.launch {
                    delay(1000)
                    val flippedBackCards = cards.map {
                        if (it.id == firstCardId || it.id == secondCardId) it.copy(isFaceUp = false) else it
                    }
                    cards = flippedBackCards
                    selectedCards = emptyList()
                }
            }
        }
    }

    private fun createShuffledCards(icons: List<String>): List<MemoryCard> {
        val gameIcons = (icons + icons).shuffled()
        return gameIcons.mapIndexed { index, icon -> MemoryCard(id = index, value = icon) }
    }
}
