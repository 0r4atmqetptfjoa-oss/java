package com.example.educationalapp.alphabet

import android.content.Context
import android.media.MediaPlayer
import com.example.educationalapp.R

class AlphabetSoundPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    fun playClick() {
        playSound(R.raw.buton_click)
    }

    fun playCorrect() {
        playSound(R.raw.raspuns_corect)
    }

    fun playWrong() {
        playSound(R.raw.raspuns_gresit)
    }

    private fun playSound(resId: Int) {
        // 1. Oprim orice sunet care rula înainte (logica de "tăiere")
        stop()

        // 2. Pornim sunetul nou
        mediaPlayer = MediaPlayer.create(context, resId).apply {
            setOnCompletionListener { mp ->
                mp.release()
                if (mediaPlayer == mp) {
                    mediaPlayer = null
                }
            }
            start()
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
        }
    }
    
    // Apelăm asta când ieșim din ecran ca să nu rămână resurse blocate
    fun release() {
        stop()
    }
}