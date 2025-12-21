package com.example.educationalapp.EggGame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EggGameViewModel @Inject constructor() : ViewModel() {

    var eggState by mutableStateOf(EggState.INTACT)
        private set

    var dragonAnim by mutableStateOf(DragonAnim.IDLE)
        private set

    /** crește ca "event counter" pentru UI (LaunchedEffect) */
    var crackEvent by mutableIntStateOf(0)
        private set

    /** 1=crack1, 2=crack2, 3=broken/burst */
    var crackLevel by mutableIntStateOf(0)
        private set

    fun onEggTap() {
        when (eggState) {
            EggState.INTACT -> {
                eggState = EggState.CRACK1
                crackLevel = 1
                crackEvent++
            }
            EggState.CRACK1 -> {
                eggState = EggState.CRACK2
                crackLevel = 2
                crackEvent++
            }
            EggState.CRACK2 -> {
                eggState = EggState.BROKEN
                crackLevel = 3
                crackEvent++
            }
            EggState.BROKEN -> {
                eggState = EggState.DRAGON
                dragonAnim = DragonAnim.IDLE
            }
            EggState.DRAGON -> {
                // daca apasa oul cand e deja dragon, trecem pe hop
                dragonAnim = DragonAnim.HOP
            }
        }
    }

    fun onDragonTap() {
        if (eggState == EggState.DRAGON) {
            dragonAnim = DragonAnim.HOP
        }
    }

    fun onHopFinished() {
        if (eggState == EggState.DRAGON && dragonAnim == DragonAnim.HOP) {
            dragonAnim = DragonAnim.IDLE
        }
    }
}
