package com.example.educationalapp.alphabet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class MascotMood { THINKING, HAPPY, SURPRISED, CELEBRATE, IDLE }

data class AlphabetGameUiState(
    val currentQuestion: AlphabetItem,
    val options: List<String>,
    val questionIndex: Int,
    val totalQuestions: Int,
    val score: Int,
    val stars: Int,
    val selectedOption: String? = null,
    val isAnswerCorrect: Boolean? = null,
    val isFinished: Boolean = false,
    val isInputLocked: Boolean = false,
    val mascotMood: MascotMood = MascotMood.THINKING,
    val attemptsLeft: Int = 2,
    val soundOn: Boolean = true
)

@HiltViewModel
class AlphabetGameViewModel @Inject constructor() : ViewModel() {

    private val totalQuestions = 10
    private val MAX_ATTEMPTS = 2

    private val _uiState: MutableStateFlow<AlphabetGameUiState> = MutableStateFlow(createInitialState())
    val uiState: StateFlow<AlphabetGameUiState> = _uiState

    private fun createInitialState(): AlphabetGameUiState {
        val (q, opts) = generateQuestion(0)
        return AlphabetGameUiState(
            currentQuestion = q,
            options = opts,
            questionIndex = 0,
            totalQuestions = totalQuestions,
            score = 0,
            stars = 0,
            mascotMood = MascotMood.THINKING,
            attemptsLeft = MAX_ATTEMPTS,
            soundOn = true
        )
    }

    private fun generateQuestion(questionIndex: Int): Pair<AlphabetItem, List<String>> {
        val all = AlphabetAssets.items
        val current = all.random()
        val correctBase = AlphabetAssets.normalizeBase(current.displayLetter)

        val otherDisplays = all
            .map { it.displayLetter }
            .distinct()
            .filter { AlphabetAssets.normalizeBase(it) != correctBase }
            .shuffled()
            .take(2)

        val options = (listOf(current.displayLetter) + otherDisplays).shuffled()
        return current to options
    }

    fun toggleSound() {
        _uiState.value = _uiState.value.copy(soundOn = !_uiState.value.soundOn)
    }

    fun selectAnswer(option: String) {
        val state = _uiState.value
        if (state.isInputLocked || state.isFinished) return

        val normalizedOption = AlphabetAssets.normalizeBase(option)
        val normalizedCorrect = AlphabetAssets.normalizeBase(state.currentQuestion.displayLetter)
        val isCorrect = normalizedOption == normalizedCorrect

        if (isCorrect) {
            val newScore = state.score + 10
            val newStars = state.stars + 1
            _uiState.value = state.copy(
                score = newScore,
                stars = newStars,
                selectedOption = option,
                isAnswerCorrect = true,
                isInputLocked = true,
                mascotMood = MascotMood.HAPPY
            )
            viewModelScope.launch {
                delay(1000L)
                moveToNextOrFinish()
            }
        } else {
            val remaining = state.attemptsLeft - 1
            if (remaining > 0) {
                _uiState.value = state.copy(
                    selectedOption = option,
                    isAnswerCorrect = false,
                    isInputLocked = true,
                    attemptsLeft = remaining,
                    mascotMood = MascotMood.SURPRISED
                )
                viewModelScope.launch {
                    delay(800L)
                    _uiState.value = _uiState.value.copy(
                        selectedOption = null,
                        isAnswerCorrect = null,
                        isInputLocked = false,
                        mascotMood = MascotMood.THINKING
                    )
                }
            } else {
                _uiState.value = state.copy(
                    selectedOption = option,
                    isAnswerCorrect = false,
                    isInputLocked = true,
                    attemptsLeft = 0,
                    mascotMood = MascotMood.SURPRISED
                )
                viewModelScope.launch {
                    delay(1000L)
                    moveToNextOrFinish()
                }
            }
        }
    }

    private fun moveToNextOrFinish() {
        val currentIndex = _uiState.value.questionIndex
        if (currentIndex + 1 >= totalQuestions) {
            _uiState.value = _uiState.value.copy(isFinished = true, isInputLocked = false, mascotMood = MascotMood.CELEBRATE)
        } else {
            val nextIndex = currentIndex + 1
            val (nextQ, nextOpts) = generateQuestion(nextIndex)
            _uiState.value = _uiState.value.copy(
                currentQuestion = nextQ,
                options = nextOpts,
                questionIndex = nextIndex,
                selectedOption = null,
                isAnswerCorrect = null,
                isInputLocked = false,
                mascotMood = MascotMood.THINKING,
                attemptsLeft = MAX_ATTEMPTS
            )
        }
    }

    fun resetGame() {
        _uiState.value = createInitialState()
    }
}
