package com.example.educationalapp.FeedGame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

@HiltViewModel
class FeedGameViewModel @Inject constructor() : ViewModel() {

    var score by mutableIntStateOf(0)
        private set

    var monsterState by mutableStateOf(MonsterState.IDLE)
        private set

    var wantedFood by mutableStateOf<FoodItem?>(null)
        private set

    /** crește ca event counter pentru UI (VFX) */
    var feedEvent by mutableIntStateOf(0)
        private set

    /** dacă ultimul feed a fost corect (pentru VFX / UI) */
    var lastFeedCorrect by mutableStateOf(false)
        private set

    val foods: List<FoodItem> = listOf(
        FoodItem(1, R.drawable.food_apple, "Apple", true),
        FoodItem(2, R.drawable.food_broccoli, "Broccoli", true),
        FoodItem(3, R.drawable.food_fish, "Fish", true),
        FoodItem(4, R.drawable.food_cookie, "Cookie", false),
        FoodItem(5, R.drawable.food_donut, "Donut", false),
    )

    private var eatJob: Job? = null

    init {
        pickNewWish()
    }

    fun pickNewWish() {
        wantedFood = foods.random()
    }

    fun onFed(food: FoodItem) {
        val wanted = wantedFood
        val correct = (wanted != null && food.id == wanted.id)

        lastFeedCorrect = correct
        if (correct) {
            score += 10
        } else {
            score = (score - 2).coerceAtLeast(0)
        }

        triggerEating()
        pickNewWish()
        feedEvent++
    }

    private fun triggerEating() {
        monsterState = MonsterState.EATING
        eatJob?.cancel()
        eatJob = viewModelScope.launch {
            delay(900)
            monsterState = MonsterState.IDLE
        }
    }
}
