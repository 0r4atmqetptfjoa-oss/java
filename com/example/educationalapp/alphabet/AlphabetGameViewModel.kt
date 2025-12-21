package com.example.educationalapp.alphabet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.min

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

    private val MAX_ATTEMPTS = 2
    private var questionDeck: List<AlphabetItem> = emptyList()

    private val _uiState: MutableStateFlow<AlphabetGameUiState> = MutableStateFlow(createInitialState(true))
    val uiState: StateFlow<AlphabetGameUiState> = _uiState

    private fun buildDeck(total: Int): List<AlphabetItem> {
        val all = AlphabetAssets.items
        if (all.isEmpty()) return emptyList()
        val safeTotal = min(total, all.size)
        return all.shuffled().take(safeTotal)
    }

    private fun createInitialState(keepSoundOn: Boolean): AlphabetGameUiState {
        val totalQuestions = min(10, AlphabetAssets.items.size.coerceAtLeast(1))
        questionDeck = buildDeck(totalQuestions)

        // REPARATIE: Folosim argumente numite pentru a evita confuzia de tipuri si ordinea lor
        val firstItem = if (questionDeck.isNotEmpty()) {
            questionDeck[0]
        } else {
            // Aici era eroarea. Acum specificăm clar fiecare parametru.
            AlphabetItem(displayLetter = "A", word = "Albină", imageRes = 0)
        }
        
        val (q, opts) = if (questionDeck.isNotEmpty()) generateQuestion(0) else (firstItem to listOf("A", "B", "C"))

        return AlphabetGameUiState(
            currentQuestion = q,
            options = opts,
            questionIndex = 0,
            totalQuestions = totalQuestions,
            score = 0,
            stars = 0,
            mascotMood = MascotMood.THINKING,
            attemptsLeft = MAX_ATTEMPTS,
            soundOn = keepSoundOn
        )
    }

    private fun generateQuestion(questionIndex: Int): Pair<AlphabetItem, List<String>> {
        if (questionDeck.isEmpty()) {
            // REPARATIE: Argumente numite si aici
            return AlphabetItem(displayLetter = "?", word = "Error", imageRes = 0) to emptyList()
        }
        
        val current = questionDeck[questionIndex.coerceIn(questionDeck.indices)]
        val correctBase = AlphabetAssets.normalizeBase(current.displayLetter)

        val otherDisplays = AlphabetAssets.items
            .asSequence()
            .map { it.displayLetter }
            .distinct()
            .filter { AlphabetAssets.normalizeBase(it) != correctBase }
            .shuffled()
            .take(2)
            .toList()

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
                delay(900L)
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
                    delay(650L)
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
                    delay(900L)
                    moveToNextOrFinish()
                }
            }
        }
    }

    private fun moveToNextOrFinish() {
        val state = _uiState.value
        val currentIndex = state.questionIndex
        if (currentIndex + 1 >= state.totalQuestions) {
            _uiState.value = state.copy(
                isFinished = true,
                isInputLocked = false,
                mascotMood = MascotMood.CELEBRATE
            )
        } else {
            val nextIndex = currentIndex + 1
            val (nextQ, nextOpts) = generateQuestion(nextIndex)
            _uiState.value = state.copy(
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
        val currentSoundSetting = _uiState.value.soundOn
        _uiState.value = createInitialState(keepSoundOn = currentSoundSetting)
    }
}