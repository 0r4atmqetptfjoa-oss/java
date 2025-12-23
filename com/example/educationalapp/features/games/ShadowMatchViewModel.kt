package com.example.educationalapp.features.games

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.random.Random

enum class ShadowKind(val label: String) {
    HEART("Inimă"),
    STAR("Stea"),
    INFO("Info"),
    HOME("Casă"),
    FACE("Față"),
    BUILD("Unelte"),
}

data class ShadowMatchItem(
    val id: Int,
    val kind: ShadowKind,
    val isMatched: Boolean = false,
)

data class ShadowMatchUiState(
    val level: Int = 1,
    val targets: List<ShadowMatchItem> = emptyList(),
    val tray: List<ShadowMatchItem> = emptyList(),
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
        val next = (_uiState.value.level + 1).coerceAtMost(99)
        newLevel(next)
    }

    fun onMatched(id: Int) {
        _uiState.update { s ->
            val newTargets = s.targets.map { if (it.id == id) it.copy(isMatched = true) else it }
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
        val allKinds = ShadowKind.values().toList()
        val count = when {
            level <= 1 -> 3
            level == 2 -> 4
            else -> 6
        }
        val picked = allKinds.shuffled(Random(level)).take(count)
        val targets = picked.mapIndexed { index, kind ->
            // id stabil per item in nivel
            ShadowMatchItem(id = index + 1, kind = kind)
        }
        val tray = targets.shuffled(Random(level * 31))

        _uiState.value = ShadowMatchUiState(
            level = level,
            targets = targets,
            tray = tray,
            isLevelComplete = false,
            lastMatchedId = null
        )
    }
}
