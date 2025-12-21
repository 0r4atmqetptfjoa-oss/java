package com.example.educationalapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class NamedColor(val name: String, val color: Color)
data class ColorQuizQuestion(val color: NamedColor, val options: List<NamedColor>)

class ColorMatchViewModel : ViewModel() {

    private val colors = listOf(
        NamedColor("Roșu", Color(0xFFE74C3C)),
        NamedColor("Verde", Color(0xFF2ECC71)),
        NamedColor("Albastru", Color(0xFF3498DB)),
        NamedColor("Galben", Color(0xFFF1C40F)),
        NamedColor("Mov", Color(0xFF9B59B6)),
        NamedColor("Portocaliu", Color(0xFFE67E22)),
        NamedColor("Roz", Color(0xFFF5A9BC)),
    )

    var score by mutableStateOf(0)
        private set

    var questionIndex by mutableStateOf(0)
        private set

    var currentQuestion by mutableStateOf(generateColorQuestion(colors))
        private set

    var selectedOption by mutableStateOf<NamedColor?>(null)
        private set

    var answerState by mutableStateOf(QuizAnswerState.UNANSWERED)
        private set

    fun nextQuestion() {
        if (questionIndex < TOTAL_COLOR_QUESTIONS - 1) {
            questionIndex++
            currentQuestion = generateColorQuestion(colors)
            answerState = QuizAnswerState.UNANSWERED
            selectedOption = null
        } else {
            questionIndex++ // To trigger the dialog
        }
    }

    fun handleAnswer(option: NamedColor, onGameWon: (stars: Int) -> Unit) {
        if (answerState != QuizAnswerState.UNANSWERED) return

        selectedOption = option
        if (option.name == currentQuestion.color.name) {
            answerState = QuizAnswerState.CORRECT
            score += 10
            onGameWon(1)
        } else {
            answerState = QuizAnswerState.INCORRECT
            score = (score - 5).coerceAtLeast(0)
        }

        viewModelScope.launch {
            delay(1500)
            nextQuestion()
        }
    }

    fun restartGame() {
        score = 0
        questionIndex = 0
        currentQuestion = generateColorQuestion(colors)
        answerState = QuizAnswerState.UNANSWERED
        selectedOption = null
    }

    private fun generateColorQuestion(colors: List<NamedColor>): ColorQuizQuestion {
        val correctColor = colors.random()
        val options = mutableSetOf(correctColor)
        while (options.size < 3) {
            options.add(colors.random())
        }
        return ColorQuizQuestion(correctColor, options.shuffled())
    }
}

private const val TOTAL_COLOR_QUESTIONS = 10