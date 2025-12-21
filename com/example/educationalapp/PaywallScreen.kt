package com.example.educationalapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/**
 * Presents information about the premium upgrade.  Only half of the games are
 * available for free; purchasing the full version unlocks all content for a
 * one time fee.  For simplicity this screen immediately grants access when
 * the purchase button is pressed (no real payment processing).  In a real
 * application you would integrate Google Play Billing or another IAP
 * provider.
 */
@Composable
fun PaywallScreen(navController: NavController, hasFullVersion: MutableState<Boolean>) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Upgrade la Versiunea Completă", modifier = Modifier.padding(bottom = 16.dp))
        Text(text = "Doar 50% din conținut este disponibil gratuit. Pentru a debloca toate jocurile și activitățile, achiziționează versiunea completă.", modifier = Modifier.padding(bottom = 16.dp))
        Text(text = "Preț: 19,99 lei", modifier = Modifier.padding(bottom = 24.dp))
        Button(onClick = {
            hasFullVersion.value = true
            navController.navigate(Screen.GamesMenu.route)
        }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(text = "Cumpără și Deblochează Tot")
        }
        Button(onClick = { navController.navigate(Screen.GamesMenu.route) }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(text = "Renunță")
        }
    }
}