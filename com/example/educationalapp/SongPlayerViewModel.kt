package com.example.educationalapp

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.educationalapp.di.BgMusicManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class SongPlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bgMusicManager: BgMusicManager
) : ViewModel() {

    val songTitles = listOf(
        "Cântec de leagăn",
        "La mulți ani",
        "Bate toba",
        "Happy Birthday"
    )
    private val resIds = listOf(
        R.raw.song_0,
        R.raw.song_1,
        R.raw.song_2,
        R.raw.song_3
    )

    var isPlaying by mutableStateOf(false)
        private set

    private var mediaPlayer: MediaPlayer? = null

    fun onPlayPauseClick(songId: Int, onSongCompleted: () -> Unit) {
        if (isPlaying) {
            stopPlayback()
        } else {
            startPlayback(songId, onSongCompleted)
        }
    }

    private fun startPlayback(songId: Int, onSongCompleted: () -> Unit) {
        val resId = resIds.getOrElse(songId) { return }
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(context, resId).apply {
            setOnCompletionListener {
                this@SongPlayerViewModel.isPlaying = false
                onSongCompleted()
                release()
            }
            start()
        }
        isPlaying = true
    }

    private fun stopPlayback() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        isPlaying = false
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
    }
}