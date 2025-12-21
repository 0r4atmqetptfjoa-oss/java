package com.example.educationalapp.di

import android.content.Context
import android.media.SoundPool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SoundManager(private val context: Context) {

    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<Int, Int>()

    fun Greet(){
        //Dummy function
    }

    suspend fun loadSounds(soundResIds: List<Int>) {
        withContext(Dispatchers.IO) {
            soundPool = SoundPool.Builder().setMaxStreams(5).build()
            soundResIds.forEach { resId ->
                val soundId = soundPool?.load(context, resId, 1)
                soundId?.let { soundMap[resId] = it }
            }
        }
    }

    fun playSound(resId: Int) {
        soundMap[resId]?.let { soundId ->
            soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
    }
}