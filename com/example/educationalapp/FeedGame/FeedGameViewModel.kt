package com.example.educationalapp.FeedGame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.educationalapp.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FoodItem(val id: Int, val resId: Int, val isHealthy: Boolean)

@HiltViewModel
class FeedGameViewModel @Inject constructor() : ViewModel() {

    companion object {
        const val STATE_IDLE = "IDLE"
        const val STATE_EATING = "EATING"
    }

    var score by mutableStateOf(0)
        private set

    var monsterState by mutableStateOf(STATE_IDLE)
        private set

    var wantedFood by mutableStateOf<FoodItem?>(null)
        private set

    val foods = listOf(
        FoodItem(1, R.drawable.food_apple, true),
        FoodItem(2, R.drawable.food_cookie, false),
        FoodItem(3, R.drawable.food_broccoli, true)
    )

    private var eatJob: Job? = null

    init {
        pickNewWish()
    }

    fun pickNewWish() {
        wantedFood = foods.random()
    }

    fun onFed(food: FoodItem) {
        if (food == wantedFood) {
            score += 10
            triggerEating()
        }
        pickNewWish()
    }

    private fun triggerEating() {
        monsterState = STATE_EATING
        eatJob?.cancel()
        eatJob = viewModelScope.launch {
            delay(900) // revine automat la idle
            monsterState = STATE_IDLE
        }
    }
}
