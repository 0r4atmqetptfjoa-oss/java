package com.example.educationalapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class ShapeMatchViewModel : ViewModel() {

    private val shapes = listOf(
        NamedShape("Inimă", Icons.Default.Favorite, Color(0xFFE74C3C)),
        NamedShape("Stea", Icons.Default.Star, Color(0xFFF1C40F)),
        NamedShape("Info", Icons.Default.Info, Color(0xFF2ECC71)), // Placeholder
    )

    var score by mutableStateOf(0)
        private set

    var questionIndex by mutableStateOf(0)
        private set

    var currentQuestion by mutableStateOf(generateShapeQuestion(shapes))
        private set

    var selectedOption by mutableStateOf<NamedShape?>(null)
        private set

    var answerState by mutableStateOf(QuizAnswerState.UNANSWERED)
        private set

    fun nextQuestion() {
        if (questionIndex < TOTAL_SHAPE_QUESTIONS - 1) {
            questionIndex++
            currentQuestion = generateShapeQuestion(shapes)
            answerState = QuizAnswerState.UNANSWERED
            selectedOption = null
        } else {
            questionIndex++ // To trigger the dialog
        }
    }

    fun handleAnswer(option: NamedShape, onGameWon: (stars: Int) -> Unit) {
        if (answerState != QuizAnswerState.UNANSWERED) return

        selectedOption = option
        if (option.name == currentQuestion.shape.name) {
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
        currentQuestion = generateShapeQuestion(shapes)
        answerState = QuizAnswerState.UNANSWERED
        selectedOption = null
    }

    private fun generateShapeQuestion(shapes: List<NamedShape>): ShapeQuizQuestion {
        val correctShape = shapes.random()
        val options = mutableSetOf(correctShape)
        while (options.size < 3) {
            options.add(shapes.random())
        }
        return ShapeQuizQuestion(correctShape, options.shuffled())
    }
}

private const val TOTAL_SHAPE_QUESTIONS = 10
