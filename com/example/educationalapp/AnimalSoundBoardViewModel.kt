package com.example.educationalapp

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.educationalapp.di.SoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue


@HiltViewModel
class AnimalSoundBoardViewModel @Inject constructor(
    private val soundManager: SoundManager
) : ViewModel() {

    val sounds = listOf(
        SoundItem("Câine", "🐶", R.raw.sound_dog),
        SoundItem("Pisică", "🐱", R.raw.sound_cat),
        SoundItem("Vacă", "🐮", R.raw.sound_cow),
        SoundItem("Cal", "🐴", R.raw.sound_horse),
        SoundItem("Broască", "🐸", R.raw.sound_frog),
        SoundItem("Leu", "🦁", R.raw.sound_lion),
        SoundItem("Oaie", "🐑", R.raw.sound_sheep),
        SoundItem("Elefant", "🐘", R.raw.sound_elephant)
    )

    private var plays by mutableIntStateOf(0)
    val feedback = mutableStateOf("")

    init {
        viewModelScope.launch {
            soundManager.loadSounds(sounds.map { it.soundResId })
        }
    }

    fun onSoundItemClick(item: SoundItem, starState: MutableState<Int>) {
        soundManager.playSound(item.soundResId)
        plays++
        if (plays >= 5) {
            starState.value += 1
            feedback.value = "Bravo! Ai explorat 5 sunete și ai câștigat o stea."
            plays = 0
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}