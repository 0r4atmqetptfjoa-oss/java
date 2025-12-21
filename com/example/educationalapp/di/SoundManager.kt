package com.example.educationalapp.di

import android.content.Context
import android.media.SoundPool
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SoundManager(private val context: Context) {

    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<Int, Int>()

    // Inițializare lazy pentru a economisi resurse
    private fun ensureSoundPool() {
        if (soundPool == null) {
            soundPool = SoundPool.Builder().setMaxStreams(5).build()
        }
    }

    suspend fun loadSounds(soundResIds: List<Int>) {
        withContext(Dispatchers.IO) {
            ensureSoundPool()
            soundResIds.forEach { resId ->
                try {
                    // Verificăm dacă nu e deja încărcat
                    if (!soundMap.containsKey(resId)) {
                        val soundId = soundPool?.load(context, resId, 1)
                        soundId?.let { soundMap[resId] = it }
                    }
                } catch (e: Exception) {
                    Log.e("SoundManager", "Error loading sound resource: $resId", e)
                }
            }
        }
    }

    fun playSound(resId: Int) {
        try {
            ensureSoundPool()
            soundMap[resId]?.let { soundId ->
                soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
            } ?: run {
                Log.w("SoundManager", "Sound not loaded: $resId")
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing sound: $resId", e)
        }
    }

    fun release() {
        try {
            soundPool?.release()
            soundPool = null
            soundMap.clear()
        } catch (e: Exception) {
            Log.e("SoundManager", "Error releasing SoundPool", e)
        }
    }
}