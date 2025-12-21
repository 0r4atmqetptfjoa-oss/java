package com.example.educationalapp

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Displays the main menu background with a crossfade animation between
 * a "lights on" and "lights off" state.
 *
 * @param isLightsOn Controls whether to show the 'on' or 'off' image.
 */
@Composable
fun MainMenuBackground(isLightsOn: Boolean) {
    Crossfade(targetState = isLightsOn, label = "MainMenuBackgroundCrossfade") { state ->
        Image(
            painter = painterResource(
                if (state) R.drawable.bg_main_menu_on
                else R.drawable.bg_main_menu_off
            ),
            contentDescription = null, // Decorative background
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * A simple screen demonstrating the usage of MainMenuBackground with a toggle switch.
 */
@Composable
fun MainMenuScreenWithToggle() {
    var isLightsOn by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        // The background is drawn first, covering the whole screen
        MainMenuBackground(isLightsOn = isLightsOn)

        // UI elements are drawn on top of the background
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Lumea Alessiei") // Example title
            Spacer(modifier = Modifier.height(16.dp))
            Switch(
                checked = isLightsOn,
                onCheckedChange = { isLightsOn = it }
            )
            Text(text = if (isLightsOn) "Luminile Aprinse" else "Luminile Stinse")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainMenuScreenWithTogglePreview() {
    // This preview will likely fail if the drawable resources are not available
    // in the project context where the preview is rendered.
    MainMenuScreenWithToggle()
}
