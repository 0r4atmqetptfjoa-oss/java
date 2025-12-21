package com.example.educationalapp.di

import android.content.Context
import android.media.MediaPlayer
import com.example.educationalapp.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BgMusicManager @Inject constructor(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    fun play() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, R.raw.main_menu_music)
            mediaPlayer?.isLooping = true
        }
        mediaPlayer?.start()
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}