package com.example.educationalapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlin.random.Random

/**
 * ViewModel backing the number sorting game.  The game consists of selecting
 * numbers in ascending order.  Each time all numbers are selected correctly
 * a new level begins with more numbers.  The difficulty therefore scales
 * gradually.  A star is awarded whenever a level is completed.
 */
class SortingGameViewModel : ViewModel() {
    // Current game level.  Increases after each successful round.
    var level by mutableStateOf(1)
        private set
    // List of numbers to display in the current round
    var numbers by mutableStateOf(generateNumbers(level))
        private set
    // Feedback text shown after each selection
    var feedback by mutableStateOf("")
        private set
    // Player's score
    var score by mutableStateOf(0)
        private set

    /**
     * Handles a number click.  If the clicked number is the smallest of the
     * remaining numbers it is removed.  Otherwise the player loses points.
     * When all numbers have been removed, the level increments and a star
     * callback is invoked to update the global star count.
     */
    fun onNumberClick(number: Int, onLevelComplete: (stars: Int) -> Unit) {
        val min = numbers.minOrNull() ?: return
        if (number == min) {
            feedback = "Corect!"
            score += 10
            numbers = numbers.filter { it != number }
            if (numbers.isEmpty()) {
                // Level completed
                level++
                feedback = "Nivel completat!"
                onLevelComplete(1)
                numbers = generateNumbers(level)
            }
        } else {
            feedback = "Greșit!"
            score = (score - 5).coerceAtLeast(0)
        }
    }

    companion object {
        /**
         * Generates a list of random integers.  The number of integers
         * increases with the level.  Values range from 1 to 99.
         */
        fun generateNumbers(level: Int): List<Int> {
            val count = level + 4 // start with 5 numbers at level 1
            return List(count) { Random.nextInt(1, 100) }
        }
    }
}