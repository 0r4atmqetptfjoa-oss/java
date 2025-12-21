package com.example.educationalapp.BalloonGame

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.educationalapp.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@HiltViewModel
class BalloonViewModel @Inject constructor() : ViewModel() {

    // --- STATE ---
    var score = mutableIntStateOf(0)
    val balloons = mutableStateListOf<BalloonState>()
    val particles = mutableStateListOf<PopParticle>()
    
    private var nextId = 0L
    
    // Configurare
    private val balloonImages = listOf(
        R.drawable.balloon_blue, R.drawable.balloon_green,
        R.drawable.balloon_orange, R.drawable.balloon_purple,
        R.drawable.balloon_red, R.drawable.balloon_yellow
    )

    // --- GAME LOOP LOGIC ---
    // Această funcție este apelată de UI la fiecare frame (60 ori pe secundă)
    fun updateGame(dt: Float, screenHeight: Float, screenWidth: Float) {
        
        // 1. Spawn (Generare Baloane)
        if (Random.nextInt(100) < 2) { // 2% șansă per frame
            spawnBalloon(screenHeight, screenWidth)
        }

        // 2. Update Baloane (Mișcare)
        val balloonIterator = balloons.iterator()
        while (balloonIterator.hasNext()) {
            val b = balloonIterator.next()
            if (!b.isPopped) {
                b.y -= b.speed * (dt * 60f) // Normalizăm viteza
                // Ștergem dacă iese din ecran sus
                if (b.y < -200f) {
                    balloonIterator.remove()
                }
            } else {
                // Dacă e spart, îl scoatem imediat din listă
                balloonIterator.remove()
            }
        }

        // 3. Update Particule
        val particleIterator = particles.iterator()
        while (particleIterator.hasNext()) {
            val p = particleIterator.next()
            p.alpha -= 2f * dt
            // Mișcare explozivă
            p.currentX += cos(Math.toRadians(p.angle.toDouble())).toFloat() * 5f
            p.currentY += sin(Math.toRadians(p.angle.toDouble())).toFloat() * 5f
            
            if (p.alpha <= 0f) particleIterator.remove()
        }
    }

    private fun spawnBalloon(screenHeight: Float, screenWidth: Float) {
        balloons.add(
            BalloonState(
                id = nextId++,
                imageRes = balloonImages.random(),
                speed = Random.nextFloat() * 3f + 2f,
                startX = Random.nextFloat() * (screenWidth - 150f)
            ).apply { 
                y = screenHeight + 100f // Pornesc de sub ecran
            }
        )
    }

    // --- INTERACTION ---
    fun onBalloonTap(balloon: BalloonState) {
        if (balloon.isPopped) return
        
        // Logică Sparge
        balloon.isPopped = true // Marcăm ca spart pt a nu fi desenat/clickuit dublu
        score.value += 10
        
        // Spawn Particule
        repeat(6) {
            particles.add(
                PopParticle(
                    id = nextId++,
                    x = balloon.startX + 50f, // Aproximativ centrul
                    y = balloon.y + 60f,
                    angle = Random.nextFloat() * 360f
                )
            )
        }
    }
}