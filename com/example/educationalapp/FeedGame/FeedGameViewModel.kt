package com.example.educationalapp.FeedGame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.educationalapp.R // Asigura-te ca ai R importat
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random

data class FoodItem(val id: Int, val resId: Int, val isHealthy: Boolean)

@HiltViewModel
class FeedGameViewModel @Inject constructor() : ViewModel() {
    var score by mutableStateOf(0)
    var monsterState by mutableStateOf("IDLE") // IDLE, EATING, PARTY
    var wantedFood by mutableStateOf<FoodItem?>(null)
    
    val foods = listOf(
        FoodItem(1, R.drawable.food_apple, true),
        FoodItem(2, R.drawable.food_cookie, false), // Treat
        FoodItem(3, R.drawable.food_broccoli, true)
    )
    
    init {
        pickNewWish()
    }
    
    fun pickNewWish() {
        wantedFood = foods.random()
    }
    
    fun onFed(food: FoodItem) {
        if (food == wantedFood) {
            score += 10
            monsterState = "EATING"
            // Trigger logic to go back to IDLE after delay (handled in UI usually or with suspend here)
        } else {
            // Wrong food logic
        }
        pickNewWish()
    }
}