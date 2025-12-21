package com.example.educationalapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlin.random.Random

class MathQuizViewModel : ViewModel() {

    var question by mutableStateOf("1 + 1")
        private set

    var answer by mutableStateOf(2)
        private set

    var options by mutableStateOf(listOf<Int>())
        private set

    var feedback by mutableStateOf("")
        private set

    var score by mutableStateOf(0)
        private set

    fun generateQuestion() {
        feedback = ""
        // Random arithmetic operation between 1 and 10
        val a = Random.nextInt(1, 10)
        val b = Random.nextInt(1, 10)
        when (Random.nextInt(3)) {
            0 -> { question = "$a + $b"; answer = a + b }
            1 -> { question = "$a - $b"; answer = a - b }
            else -> { question = "$a × $b"; answer = a * b }
        }
        // Generate options
        val set = mutableSetOf(answer)
        val optionList = mutableListOf<Int>()
        val correctIndex = Random.nextInt(3)
        for (i in 0 until 3) {
            if (i == correctIndex) {
                optionList.add(answer)
            } else {
                var opt: Int
                do {
                    opt = answer + Random.nextInt(-5, 6)
                } while (opt in set)
                set.add(opt)
                optionList.add(opt)
            }
        }
        options = optionList
    }

    fun onOptionSelected(option: Int, onGameWon: (stars: Int) -> Unit) {
        if (option == answer) {
            feedback = "Corect!"
            score += 10
            onGameWon(1)
        } else {
            feedback = "Greșit!"
            score = (score - 5).coerceAtLeast(0)
        }
        generateQuestion()
    }
}