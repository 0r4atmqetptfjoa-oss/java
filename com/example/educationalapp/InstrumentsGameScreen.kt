package com.example.educationalapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.navigation.NavController

/**
 * The Instruments game serves as a wrapper around the instrument guessing mini‑game.
 * This keeps the route structure consistent while allowing the actual game logic
 * to live in [InstrumentGuessGameScreen].  Any additional instrument related
 * games can be routed through this screen in the future.
 */
@Composable
fun InstrumentsGameScreen(navController: NavController, starState: MutableState<Int>) {
    // Delegate to the existing instrument guessing game.  Passing the star state
    // through allows earned stars to propagate to the global counter.
    InstrumentGuessGameScreen(navController = navController, starState = starState)
}