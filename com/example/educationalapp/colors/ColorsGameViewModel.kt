package com.example.educationalapp.colors

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Constantele rămân la fel
const val PROJECTILE_DURATION = 600L
const val POP_DURATION = 200L

enum class GameState { WAITING_INPUT, PROJECTILE_FLYING, IMPACT, CELEBRATE }

data class ColorsUiState(
    val currentTarget: ColorItem,
    val options: List<ColorItem>,
    val score: Int = 0,
    val gameState: GameState = GameState.WAITING_INPUT,
    
    val projectileStart: Offset = Offset.Zero,
    val projectileEnd: Offset = Offset.Zero,
    val projectileColor: Color = Color.White,
    
    val isAnswerCorrect: Boolean? = null,
    val wrongSelectionId: String? = null
)

@HiltViewModel
class ColorsGameViewModel @Inject constructor() : ViewModel() {

    // --- FIX CRITIC: Declaram coada LA ÎNCEPUT ---
    // Trebuie să existe înainte să creăm starea inițială!
    private val questionQueue = ArrayDeque<ColorItem>()

    // Acum putem declara uiState, pentru că questionQueue există deja
    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<ColorsUiState> = _uiState

    init {
        // Nu mai e strict necesar să apelăm refillQueue aici dacă îl apelăm 
        // dinamic în getNextTargetFromQueue, dar nu strică.
        if (questionQueue.isEmpty()) {
            refillQueue()
        }
    }

    private fun refillQueue() {
        val allItems = ColorsAssets.getAllItems().toMutableList()
        allItems.shuffle() // Le amestecăm bine
        questionQueue.addAll(allItems)
    }

    private fun getNextTargetFromQueue(): ColorItem {
        // Dacă s-a golit sacul (sau e prima rulare), îl umplem
        if (questionQueue.isEmpty()) {
            refillQueue()
        }
        return questionQueue.removeFirst()
    }

    private fun createInitialState(): ColorsUiState {
        // Acum asta va funcționa corect pentru că questionQueue este inițializat
        val target = getNextTargetFromQueue()
        return ColorsUiState(
            currentTarget = target,
            options = generateOptionsForTarget(target)
        )
    }

    // Generăm opțiunile astfel încât butonul corect să fie FIX varianta țintei
    private fun generateOptionsForTarget(target: ColorItem): List<ColorItem> {
        val options = mutableListOf<ColorItem>()
        
        // 1. Adăugăm răspunsul corect
        options.add(target)
        
        // 2. Adăugăm celelalte 5 culori greșite
        ColorsAssets.allColorNames.forEach { color ->
            if (color != target.name) {
                options.add(ColorsAssets.getRandomItemByColor(color))
            }
        }
        
        // 3. Amestecăm pozițiile în grilă
        return options.shuffled()
    }

    fun onOptionSelected(selectedItem: ColorItem, startPos: Offset, targetPos: Offset) {
        if (_uiState.value.gameState != GameState.WAITING_INPUT) return
        if (targetPos == Offset.Zero) return

        val isCorrect = selectedItem.id == _uiState.value.currentTarget.id

        if (isCorrect) {
            viewModelScope.launch {
                delay(POP_DURATION)
                
                _uiState.value = _uiState.value.copy(
                    gameState = GameState.PROJECTILE_FLYING,
                    projectileStart = startPos,
                    projectileEnd = targetPos,
                    projectileColor = selectedItem.colorValue,
                    isAnswerCorrect = true,
                    wrongSelectionId = null
                )

                delay(PROJECTILE_DURATION)
                
                _uiState.value = _uiState.value.copy(
                    gameState = GameState.IMPACT,
                    score = _uiState.value.score + 10
                )
                
                delay(2000)
                nextQuestion()
            }
        } else {
            _uiState.value = _uiState.value.copy(wrongSelectionId = selectedItem.id)
            viewModelScope.launch {
                delay(500)
                _uiState.value = _uiState.value.copy(wrongSelectionId = null)
            }
        }
    }

    private fun nextQuestion() {
        val nextTarget = getNextTargetFromQueue()
        
        _uiState.value = _uiState.value.copy(
            currentTarget = nextTarget,
            options = generateOptionsForTarget(nextTarget),
            gameState = GameState.WAITING_INPUT,
            isAnswerCorrect = null,
            wrongSelectionId = null
        )
    }
}