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
 * Allows the user to select one of several profiles.  The selected profile
 * index is stored in a MutableState provided by the parent.  In a real
 * application you might also persist profile data and stars separately.
 */
@Composable
fun ProfilesMenuScreen(navController: NavController, selectedProfileIndex: MutableState<Int>) {
    val profiles = listOf("Profil 1", "Profil 2", "Profil 3")
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Selectează Profil", modifier = Modifier.padding(bottom = 16.dp))
        profiles.forEachIndexed { index, name ->
            Button(
                onClick = {
                    selectedProfileIndex.value = index
                    navController.navigate(Screen.MainMenu.route)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(text = if (selectedProfileIndex.value == index) "$name ✔" else name)
            }
        }
        Button(onClick = { navController.navigate(Screen.MainMenu.route) }, modifier = Modifier.padding(top = 16.dp)) {
            Text(text = "Înapoi la Meniu Principal")
        }
    }
}