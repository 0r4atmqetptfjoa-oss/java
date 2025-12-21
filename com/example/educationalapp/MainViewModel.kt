package com.example.educationalapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class MainViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

    private val _starCount = MutableStateFlow(0)
    val starCount: StateFlow<Int> get() = _starCount.asStateFlow()

    private val _hasFullVersion = MutableStateFlow(false)
    val hasFullVersion: StateFlow<Boolean> get() = _hasFullVersion.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> get() = _soundEnabled.asStateFlow()

    private val _musicEnabled = MutableStateFlow(true)
    val musicEnabled: StateFlow<Boolean> get() = _musicEnabled.asStateFlow()

    private val _hardModeEnabled = MutableStateFlow(false)
    val hardModeEnabled: StateFlow<Boolean> get() = _hardModeEnabled.asStateFlow()

    // Example functions to mutate state
    fun setStarCount(count: Int) {
        _starCount.value = count
    }

    fun addStars(delta: Int) {
        _starCount.value = (_starCount.value + delta).coerceAtLeast(0)
    }


    fun toggleFullVersion() {
        _hasFullVersion.value = !_hasFullVersion.value
    }

    fun toggleSound() {
        _soundEnabled.value = !_soundEnabled.value
    }

    fun toggleMusic() {
        _musicEnabled.value = !_musicEnabled.value
    }

    fun toggleHardMode() {
        _hardModeEnabled.value = !_hardModeEnabled.value
    }
}
