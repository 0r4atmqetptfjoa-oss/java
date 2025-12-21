package com.example.educationalapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.educationalapp.di.BgMusicManager
import com.example.educationalapp.ui.theme.EducationalAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var bgMusicManager: BgMusicManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Setăm Edge-to-Edge imediat, dar în siguranță
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // 2. Ascundem UI-ul
        hideSystemUI()

        setContent {
            val viewModel: MainViewModel = hiltViewModel()

            EducationalAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bgMusicManager.play()
        // Re-ascundem UI-ul dacă a reapărut (ex: după ce utilizatorul a tras bara de notificări)
        hideSystemUI()
    }

    override fun onPause() {
        super.onPause()
        bgMusicManager.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        bgMusicManager.release()
    }

    private fun hideSystemUI() {
        // Folosim WindowCompat.getInsetsController care este NULL-SAFE și compatibil cu toate versiunile
        // 'window.decorView' forțează crearea decorului dacă nu există, prevenind eroarea de null
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        
        // Setăm comportamentul: barele apar doar scurt la swipe și se ascund la loc
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        // Ascundem tot (status bar, navigation bar)
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}