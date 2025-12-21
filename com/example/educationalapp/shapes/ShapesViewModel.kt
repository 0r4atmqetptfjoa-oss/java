package com.example.educationalapp.shapes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Definim stările exacte pentru reacția robotului
enum class GameState { 
    WAITING_INPUT,    // Robot normal (așteaptă)
    CORRECT_FEEDBACK, // Robot Happy (tăblița sus)
    WRONG_FEEDBACK    // Robot Sad (tăblița jos)
}

data class ShapesUiState(
    val targetShape: ShapeType,
    val options: List<ShapeItem>,
    val score: Int = 0,
    val gameState: GameState = GameState.WAITING_INPUT,
    val wrongSelectionId: String? = null
)

@HiltViewModel
class ShapesViewModel @Inject constructor() : ViewModel() {

    private val shapeQueue = ArrayDeque<ShapeType>()
    
    private val _uiState = MutableStateFlow(
        ShapesUiState(ShapeType.CIRCLE, emptyList())
    )
    val uiState: StateFlow<ShapesUiState> = _uiState

    init {
        refillQueue()
        nextRound()
    }

    private fun refillQueue() {
        val allShapes = ShapeType.values().toList()
        shapeQueue.addAll((allShapes + allShapes).shuffled())
    }

    fun onOptionSelected(selectedItem: ShapeItem) {
        if (_uiState.value.gameState != GameState.WAITING_INPUT) return

        val isCorrect = selectedItem.type == _uiState.value.targetShape

        if (isCorrect) {
            _uiState.value = _uiState.value.copy(
                gameState = GameState.CORRECT_FEEDBACK,
                score = _uiState.value.score + 10,
                wrongSelectionId = null
            )
            
            viewModelScope.launch {
                delay(2000)
                nextRound()
            }
        } else {
            _uiState.value = _uiState.value.copy(
                gameState = GameState.WRONG_FEEDBACK,
                wrongSelectionId = selectedItem.id
            )
            
            viewModelScope.launch {
                delay(2000)
                _uiState.value = _uiState.value.copy(
                    gameState = GameState.WAITING_INPUT,
                    wrongSelectionId = null
                )
            }
        }
    }

    private fun nextRound() {
        if (shapeQueue.isEmpty()) {
            refillQueue()
        }
        val nextShape = shapeQueue.removeFirst()

        _uiState.value = _uiState.value.copy(
            targetShape = nextShape,
            options = generateOptions(nextShape),
            gameState = GameState.WAITING_INPUT,
            wrongSelectionId = null
        )
    }

    private fun generateOptions(target: ShapeType): List<ShapeItem> {
        val opts = mutableListOf<ShapeItem>()
        opts.add(ShapesAssets.getRandomItemForShape(target))
        
        while (opts.size < 4) {
            val distractor = ShapesAssets.getRandomDistractor(target)
            if (opts.none { it.id == distractor.id }) {
                opts.add(distractor)
            }
        }
        return opts.shuffled()
    }
}