package com.example.educationalapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PastelPink,
    secondary = PastelBlue,
    tertiary = PastelYellow
)

// Use a light pastel palette to create a soft, child‑friendly appearance.
private val LightColorScheme = lightColorScheme(
    primary = PastelPink,
    onPrimary = Color.Black,
    secondary = PastelBlue,
    onSecondary = Color.Black,
    tertiary = PastelYellow,
    onTertiary = Color.Black,
    background = Color(0xFFFFFBFE),
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black
)

/**
 * A Material3 theme that optionally supports dynamic color on Android 12+.
 * If [useDynamicColors] is true and the device supports dynamic color, the theme
 * will use the system's dynamic light/dark color scheme. Otherwise it falls back
 * to the pastel palette defined above.
 */
@Composable
fun EducationalAppTheme(
    useDynamicColors: Boolean = true,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) DarkColorScheme else LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
