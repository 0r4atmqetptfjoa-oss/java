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
    DIRT,       // Pământ plat (trebuie săpat)
    DUG,        // Groapă (trebuie semințe)
    SEEDED,     // Astupat (trebuie apă)
    SPROUT,     // Vlăstar (trebuie soare/nor mutat)
    GROWN       // Plantă matură (trebuie culeasă)
}

enum class ToolType { SHOVEL, SEEDS, WATER }

// Folosim resursele din categoria "Magic Garden / Plants" din CSV [cite: 8]
enum class PlantType(val imageRes: Int) {
    CARROT(R.drawable.plant_carrot),
    PUMPKIN(R.drawable.plant_pumpkin),
    SUNFLOWER(R.drawable.plant_sunflower),
    WATERMELON(R.drawable.plant_watermelon)
}

data class MagicGardenUiState(
    val stage: GardenStage = GardenStage.DIRT,
    val currentPlant: PlantType = PlantType.CARROT,
    val actionProgress: Float = 0f,          // 0..1 progresul acțiunii curente
    val isCloudMoved: Boolean = false,       // soarele a fost descoperit
    val harvestCount: Int = 0,               // contor recoltă
    val isCelebrating: Boolean = false       // animație finală
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
            stage = GardenStage.DIRT,
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
        GardenStage.DIRT -> ToolType.SHOVEL
        GardenStage.DUG -> ToolType.SEEDS
        GardenStage.SEEDED -> ToolType.WATER
        else -> null
    }

    fun addActionProgress(delta: Float) {
        val s = _uiState.value
        val stage = s.stage
        // Doar primele 3 stadii necesită unelte
        if (stage != GardenStage.DIRT && stage != GardenStage.DUG && stage != GardenStage.SEEDED) return

        val next = min(1f, max(0f, s.actionProgress + delta))
        if (next >= 1f) {
            // Avansăm la stadiul următor
            val newStage = when (stage) {
                GardenStage.DIRT -> GardenStage.DUG
                GardenStage.DUG -> GardenStage.SEEDED
                GardenStage.SEEDED -> GardenStage.SPROUT
                else -> stage
            }
            _uiState.value = s.copy(stage = newStage, actionProgress = 0f)
        } else {
            _uiState.value = s.copy(actionProgress = next)
        }
    }

    fun onCloudMovedAway() {
        val s = _uiState.value
        if (s.stage != GardenStage.SPROUT || s.isCloudMoved) return
        viewModelScope.launch {
            _uiState.value = s.copy(isCloudMoved = true)
            delay(450) // așteptăm să apară razele
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
                stage = GardenStage.DIRT,
                currentPlant = randomPlant(),
                actionProgress = 0f,
                isCloudMoved = false,
                harvestCount = s.harvestCount + 1,
                isCelebrating = false
            )
        }
    }
}