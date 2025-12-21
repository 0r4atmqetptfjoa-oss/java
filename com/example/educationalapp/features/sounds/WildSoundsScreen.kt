package com.example.educationalapp.features.sounds

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.educationalapp.ui.components.rememberAssetSheet
import com.example.educationalapp.ui.media.rememberSoundPlayer
import com.example.educationalapp.ui.theme.KidFontFamily
import com.example.educationalapp.utils.toSafeFileName
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random

// Simplified data class for static animal assets
data class SoundAnimal(val name: String, val assetPath: String)

val wildAnimals = listOf(
    SoundAnimal("Elefant", "menus/sounds/salbatice/elefant.webp"),
    SoundAnimal("Girafă", "menus/sounds/salbatice/girafa.webp"),
    SoundAnimal("Gorilă", "menus/sounds/salbatice/gorila.webp"),
    SoundAnimal("Hienă", "menus/sounds/salbatice/hiena.webp"),
    SoundAnimal("Koala", "menus/sounds/salbatice/koala.webp"),
    SoundAnimal("Leopard", "menus/sounds/salbatice/leopard.webp"),
    SoundAnimal("Leu", "menus/sounds/salbatice/leu.webp"),
    SoundAnimal("Porc Mistreț", "menus/sounds/salbatice/porc mistret.webp"),
    SoundAnimal("Rinocer", "menus/sounds/salbatice/rinocer.webp"),
    SoundAnimal("Tigru", "menus/sounds/salbatice/tigru.webp"),
    SoundAnimal("Urs", "menus/sounds/salbatice/urs.webp"),
    SoundAnimal("Vulpe", "menus/sounds/salbatice/vulpe.webp"),
    SoundAnimal("Zebră", "menus/sounds/salbatice/zebra.webp")
)

@Composable
fun WildSoundsScreen() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(wildAnimals) { animal ->
            AnimalSoundCard(animal = animal)
        }
    }
}

@Composable
private fun AnimalSoundCard(animal: SoundAnimal) {
    val interactionSource = remember { MutableInteractionSource() }
    val context = LocalContext.current

    // --- Sound Player ---
    val soundFileName = animal.name.toSafeFileName()
    val soundUri = remember(soundFileName) {
        Uri.parse("asset:///sounds/salbatice/$soundFileName.mp3")
    }
    val soundPlayer = rememberSoundPlayer(soundUri = soundUri)


    // --- Animation Values ---
    val scale = remember { Animatable(1f) }
    val translationY = remember { Animatable(0f) }
    val shadowElevation = remember { Animatable(8f) }
    val sparkles = remember { List(5) { Sparkle() } }

    // --- Idle Animation ---
    val infiniteTransition = rememberInfiniteTransition(label = "idle_bounce")
    val idleBounce by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000 + (animal.name.length % 500), easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_bounce_anim"
    )

    // --- Tap Gesture ---
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            coroutineScope {
                when (interaction) {
                    is PressInteraction.Press -> {
                        soundPlayer.seekTo(0)
                        soundPlayer.play()
                        launch { scale.animateTo(0.9f, spring(stiffness = 400f)) }
                        launch { translationY.animateTo(-30f, spring(stiffness = 400f)) }
                        launch { shadowElevation.animateTo(25f, spring(stiffness = 400f)) }
                        sparkles.forEach { it.explode() }
                    }
                    is PressInteraction.Release -> {
                        launch { scale.animateTo(1f, spring(stiffness = 200f)) }
                        launch { translationY.animateTo(0f, spring(stiffness = 200f)) }
                        launch { shadowElevation.animateTo(8f, spring(stiffness = 200f)) }
                    }
                    is PressInteraction.Cancel -> {
                         launch { scale.animateTo(1f) }
                         launch { translationY.animateTo(0f) }
                         launch { shadowElevation.animateTo(8f) }
                    }
                }
            }
        }
    }

    // --- Card Layout ---
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = shadowElevation.value.dp),
        modifier = Modifier.clickable(interactionSource, null, onClick = {}) // Click is handled by LaunchedEffect
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF81C784), Color(0xFF4CAF50))
                    )
                )
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            val image = rememberAssetSheet(path = animal.assetPath)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = animal.name,
                        modifier = Modifier
                            .fillMaxSize(0.9f)
                            .graphicsLayer {
                                this.scaleX = scale.value
                                this.scaleY = scale.value
                                this.translationY = translationY.value + idleBounce
                            }
                    )
                }
                SparklesView(sparkles)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = animal.name,
                style = TextStyle(
                    fontFamily = KidFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    shadow = Shadow(Color.Black.copy(alpha = 0.25f), Offset(2f, 4f), 4f)
                )
            )
        }
    }
}

// --- Sparkles ---
data class Sparkle(val id: Long = Random.nextLong()) {
    val scale = Animatable(0f)
    val alpha = Animatable(0f)
    val xOffset = (Random.nextFloat() * 200f) - 100f
    val yOffset = (Random.nextFloat() * 200f) - 100f

    suspend fun explode() {
        coroutineScope {
            launch {
                scale.snapTo(0f)
                alpha.snapTo(1f)
                scale.animateTo(1f, tween(300, easing = FastOutLinearInEasing))
                alpha.animateTo(0f, tween(200, delayMillis = 100))
            }
        }
    }
}

@Composable
fun SparklesView(sparkles: List<Sparkle>) {
    Canvas(modifier = Modifier.size(150.dp)) {
        sparkles.forEach { sparkle ->
            if (sparkle.alpha.value > 0) {
                drawCircle(
                    color = Color.White.copy(alpha = sparkle.alpha.value),
                    radius = 8f * sparkle.scale.value,
                    center = center + Offset(
                        sparkle.xOffset * sparkle.scale.value,
                        sparkle.yOffset * sparkle.scale.value
                    ),
                    blendMode = BlendMode.Screen
                )
            }
        }
    }
}
