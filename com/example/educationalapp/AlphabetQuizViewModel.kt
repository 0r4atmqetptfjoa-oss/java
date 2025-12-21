package com.example.educationalapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class QuizQuestion(val letter: Char, val options: List<Char>)

class AlphabetQuizViewModel : ViewModel() {

    var score by mutableStateOf(0)
        private set

    var questionIndex by mutableStateOf(0)
        private set

    var currentQuestion by mutableStateOf(generateQuestion())
        private set

    var selectedOption by mutableStateOf<Char?>(null)
        private set

    var answerState by mutableStateOf(QuizAnswerState.UNANSWERED)
        private set

    fun nextQuestion() {
        if (questionIndex < TOTAL_QUESTIONS - 1) {
            questionIndex++
            currentQuestion = generateQuestion()
            answerState = QuizAnswerState.UNANSWERED
            selectedOption = null
        } else {
            questionIndex++ // To trigger the dialog
        }
    }

    fun handleAnswer(option: Char, onGameWon: (stars: Int) -> Unit) {
        if (answerState != QuizAnswerState.UNANSWERED) return

        selectedOption = option
        if (option == currentQuestion.letter) {
            answerState = QuizAnswerState.CORRECT
            score += 10
            onGameWon(1)
        } else {
            answerState = QuizAnswerState.INCORRECT
            score = (score - 5).coerceAtLeast(0)
        }

        viewModelScope.launch {
            delay(1500) // Wait for animations
            nextQuestion()
        }
    }

    fun restartGame() {
        score = 0
        questionIndex = 0
        currentQuestion = generateQuestion()
        answerState = QuizAnswerState.UNANSWERED
        selectedOption = null
    }

    private fun generateQuestion(): QuizQuestion {
        val letters = ('A'..'Z').toList()
        val correctLetter = letters.random()
        val options = mutableSetOf(correctLetter)
        while (options.size < 3) {
            options.add(letters.random())
        }
        return QuizQuestion(correctLetter, options.shuffled())
    }
}

private const val TOTAL_QUESTIONS = 10