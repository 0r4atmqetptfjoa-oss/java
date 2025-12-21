package com.example.educationalapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.educationalapp.designsystem.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    soundEnabled: Boolean,
    musicEnabled: Boolean,
    hardModeEnabled: Boolean,
    onSoundChanged: () -> Unit,
    onMusicChanged: () -> Unit,
    onHardModeChanged: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Fundal ca în restul aplicației
        Image(
            painter = painterResource(id = R.drawable.bg_category_games),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                val baseStyle = MaterialTheme.typography.headlineSmall
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.label_settings),
                            style = baseStyle.copy(
                                fontSize = baseStyle.fontSize * 0.7f // ~30% mai mic
                            ),
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.label_home),
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Switch-uri pentru setări
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.large)
                ) {
                    SettingRow(
                        label = "Sunete activate",
                        checked = soundEnabled,
                        onCheckedChange = onSoundChanged
                    )
                    SettingRow(
                        label = "Muzică activată",
                        checked = musicEnabled,
                        onCheckedChange = onMusicChanged
                    )
                    SettingRow(
                        label = "Mod greu",
                        checked = hardModeEnabled,
                        onCheckedChange = onHardModeChanged
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Butoane de acțiune
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { navController.navigate(Screen.ParentalGate.route) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(text = "Poartă Părinte")
                    }
                    Button(
                        onClick = { navController.navigate(Screen.MainMenu.route) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(text = "Înapoi la Meniu Principal")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    checked: Boolean,
    onCheckedChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange() }
        )
    }
}
