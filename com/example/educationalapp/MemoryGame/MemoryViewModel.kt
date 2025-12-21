package com.example.educationalapp.MemoryGame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.educationalapp.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoryViewModel @Inject constructor() : ViewModel() {

    // --- STATE ---
    var cards by mutableStateOf<List<MemoryCard>>(emptyList())
        private set
    
    var isProcessing by mutableStateOf(false) // Blochează click-urile în timpul verificării
    
    private var selectedCards = mutableListOf<MemoryCard>()

    init {
        resetGame()
    }

    fun resetGame() {
        // Lista de imagini (Food + Chocolate din manifest)
        val images = listOf(
            R.drawable.food_apple,
            R.drawable.food_cookie,
            R.drawable.food_donut,
            R.drawable.food_broccoli,
            R.drawable.food_fish,
            R.drawable.shape_rect_chocolate
        )
        // Dublăm lista și amestecăm
        cards = (images + images).shuffled().mapIndexed { index, res ->
            MemoryCard(id = index, imageRes = res)
        }
        selectedCards.clear()
        isProcessing = false
    }

    fun onCardClick(card: MemoryCard) {
        if (isProcessing || card.isFlipped || card.isMatched) return

        // 1. Întoarcem cartea vizual
        updateCardState(card.id, isFlipped = true)
        selectedCards.add(card)

        // 2. Verificăm perechea dacă avem 2 cărți
        if (selectedCards.size == 2) {
            isProcessing = true
            checkForMatch()
        }
    }

    private fun checkForMatch() {
        viewModelScope.launch {
            val c1 = selectedCards[0]
            val c2 = selectedCards[1]

            delay(1000) // Pauză de suspans

            if (c1.imageRes == c2.imageRes) {
                // MATCH! Rămân întoarse
                updateCardState(c1.id, isMatched = true)
                updateCardState(c2.id, isMatched = true)
            } else {
                // FAIL! Se întorc înapoi
                updateCardState(c1.id, isFlipped = false)
                updateCardState(c2.id, isFlipped = false)
            }

            selectedCards.clear()
            isProcessing = false
        }
    }

    // Helper pentru actualizarea imuabilă a listei
    private fun updateCardState(id: Int, isFlipped: Boolean? = null, isMatched: Boolean? = null) {
        cards = cards.map { 
            if (it.id == id) {
                it.copy(
                    isFlipped = isFlipped ?: it.isFlipped,
                    isMatched = isMatched ?: it.isMatched
                )
            } else it
        }
    }
    
    fun isGameWon(): Boolean = cards.isNotEmpty() && cards.all { it.isMatched }
}