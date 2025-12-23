package com.example.educationalapp.features.games

import androidx.lifecycle.ViewModel
import com.example.educationalapp.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.random.Random

// Definim perechile Imagine - Umbră folosind resursele din CSV
enum class ShadowKind(val label: String, val imageRes: Int, val shadowRes: Int) {
    ELEPHANT("Elefant", R.drawable.img_elephant, R.drawable.shadow_elephant),
    GIRAFFE("Girafă", R.drawable.img_giraffe, R.drawable.shadow_giraffe),
    HIPPO("Hipopotam", R.drawable.img_hippo, R.drawable.shadow_hippo),
    LION("Leu", R.drawable.img_lion, R.drawable.shadow_lion),
    MONKEY("Maimuță", R.drawable.img_monkey, R.drawable.shadow_monkey),
    TIGER("Tigru", R.drawable.img_tiger, R.drawable.shadow_tiger),
    ZEBRA("Zebră", R.drawable.img_zebra, R.drawable.shadow_zebra)
}

data class ShadowMatchItem(
    val id: Int,
    val kind: ShadowKind,
    val isMatched: Boolean = false,
)

data class ShadowMatchUiState(
    val level: Int = 1,
    val targets: List<ShadowMatchItem> = emptyList(), // Umbrele de pe tablă
    val tray: List<ShadowMatchItem> = emptyList(),    // Piesele colorate de jos
    val isLevelComplete: Boolean = false,
    val lastMatchedId: Int? = null,
)

@HiltViewModel
class ShadowMatchViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ShadowMatchUiState())
    val uiState: StateFlow<ShadowMatchUiState> = _uiState

    init {
        newLevel(1)
    }

    fun resetGame() {
        newLevel(1)
    }

    fun nextLevel() {
        val next = (_uiState.value.level + 1)
        newLevel(next)
    }

    fun onMatched(id: Int) {
        _uiState.update { s ->
            // Marcăm ținta ca fiind completată
            val newTargets = s.targets.map { if (it.id == id) it.copy(isMatched = true) else it }
            // Scoatem piesa din tavă
            val newTray = s.tray.filterNot { it.id == id }
            
            val complete = newTargets.all { it.isMatched }
            s.copy(
                targets = newTargets,
                tray = newTray,
                isLevelComplete = complete,
                lastMatchedId = id
            )
        }
    }

    fun clearLastMatched() {
        _uiState.update { it.copy(lastMatchedId = null) }
    }

    private fun newLevel(level: Int) {
        val allKinds = ShadowKind.values().toList().shuffled()
        
        // Creștem dificultatea: 2 piese la niv 1, 3 la niv 2, 4 la niv 3+
        val count = when {
            level == 1 -> 2
            level == 2 -> 3
            else -> 4
        }.coerceAtMost(allKinds.size)

        val picked = allKinds.take(count)
        
        // Creăm itemele
        val items = picked.mapIndexed { index, kind ->
            ShadowMatchItem(id = index + 1, kind = kind)
        }

        _uiState.value = ShadowMatchUiState(
            level = level,
            targets = items, // Ordinea originală pe tablă
            tray = items.shuffled(), // Ordine amestecată în tavă
            isLevelComplete = false,
            lastMatchedId = null
        )
    }
}