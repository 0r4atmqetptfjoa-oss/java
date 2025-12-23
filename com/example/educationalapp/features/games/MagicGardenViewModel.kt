package com.example.educationalapp.features.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.educationalapp.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

enum class GardenStage {
    GRASS,      // need digging
    DUG,        // need seeds
    SEEDED,     // need watering
    WATERED,    // need sun (move cloud)
    GROWN       // harvest / next
}

enum class ToolType { SHOVEL, SEEDS, WATER }

enum class PlantType(val imageRes: Int) {
    STRAWBERRY(R.drawable.char_strawberry_happy),
    SUN(R.drawable.char_sun_happy),
    CACTUS(R.drawable.char_cactus_happy),
    CARROT(R.drawable.char_carrot_happy),
    APPLE(R.drawable.food_apple)
}

data class MagicGardenUiState(
    val stage: GardenStage = GardenStage.GRASS,
    val currentPlant: PlantType = PlantType.STRAWBERRY,
    val actionProgress: Float = 0f,          // 0..1 for current stage action (dig/seed/water)
    val isCloudMoved: Boolean = false,       // sun revealed
    val harvestCount: Int = 0,               // session counter
    val isCelebrating: Boolean = false       // short celebration window
)

@HiltViewModel
class MagicGardenViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(MagicGardenUiState())
    val uiState: StateFlow<MagicGardenUiState> = _uiState

    init {
        startNewRound(resetCounter = true)
    }

    private fun randomPlant(): PlantType = PlantType.values().random()

    private fun startNewRound(resetCounter: Boolean = false) {
        val count = if (resetCounter) 0 else _uiState.value.harvestCount
        _uiState.value = MagicGardenUiState(
            stage = GardenStage.GRASS,
            currentPlant = randomPlant(),
            actionProgress = 0f,
            isCloudMoved = false,
            harvestCount = count,
            isCelebrating = false
        )
    }

    fun resetGame() {
        startNewRound(resetCounter = true)
    }

    fun currentTool(): ToolType? = when (_uiState.value.stage) {
        GardenStage.GRASS -> ToolType.SHOVEL
        GardenStage.DUG -> ToolType.SEEDS
        GardenStage.SEEDED -> ToolType.WATER
        else -> null
    }

    /**
     * Adds progress for the current stage action.
     * Call this continuously while the correct tool is "working" over the garden patch.
     */
    fun addActionProgress(delta: Float) {
        val s = _uiState.value
        val stage = s.stage
        if (stage != GardenStage.GRASS && stage != GardenStage.DUG && stage != GardenStage.SEEDED) return

        val next = min(1f, max(0f, s.actionProgress + delta))
        if (next >= 1f) {
            // Stage complete -> advance
            val newStage = when (stage) {
                GardenStage.GRASS -> GardenStage.DUG
                GardenStage.DUG -> GardenStage.SEEDED
                GardenStage.SEEDED -> GardenStage.WATERED
                else -> stage
            }
            _uiState.value = s.copy(stage = newStage, actionProgress = 0f)
            if (newStage == GardenStage.WATERED) {
                // small pause before user moves cloud; keeps UX clear
                return
            }
        } else {
            _uiState.value = s.copy(actionProgress = next)
        }
    }

    fun onCloudMovedAway() {
        val s = _uiState.value
        if (s.stage != GardenStage.WATERED || s.isCloudMoved) return
        viewModelScope.launch {
            _uiState.value = s.copy(isCloudMoved = true)
            delay(450) // let rays appear
            _uiState.value = _uiState.value.copy(stage = GardenStage.GROWN, isCelebrating = true)
            delay(900)
            _uiState.value = _uiState.value.copy(isCelebrating = false)
        }
    }

    fun harvestAndNext() {
        val s = _uiState.value
        if (s.stage != GardenStage.GROWN) return
        viewModelScope.launch {
            _uiState.value = s.copy(isCelebrating = true)
            delay(650)
            _uiState.value = MagicGardenUiState(
                stage = GardenStage.GRASS,
                currentPlant = randomPlant(),
                actionProgress = 0f,
                isCloudMoved = false,
                harvestCount = s.harvestCount + 1,
                isCelebrating = false
            )
        }
    }
}
