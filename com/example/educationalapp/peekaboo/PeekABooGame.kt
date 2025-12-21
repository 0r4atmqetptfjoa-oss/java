package com.example.educationalapp.peekaboo

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.educationalapp.R

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

private enum class PeekABooPhase { Searching, Revealing, Celebrating }

@Composable
fun PeekABooGame(
    modifier: Modifier = Modifier,
    onHome: () -> Unit = {},
    numberOfBushes: Int = 3,
    enableSparkles: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // --- RESURSE ---
    var bunnyFrames by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var bushImage by remember { mutableStateOf<ImageBitmap?>(null) }

    // --- GAME STATE ---
    var phase by rememberSaveable { mutableStateOf(PeekABooPhase.Searching) }
    var hidingSpotIndex by rememberSaveable { mutableIntStateOf(Random.nextInt(numberOfBushes)) }
    var currentFrameIndex by rememberSaveable { mutableIntStateOf(0) }

    var showPeekHint by rememberSaveable { mutableStateOf(false) }

    // Confetti
    var confettiVisible by remember { mutableStateOf(false) }
    var confettiKey by remember { mutableIntStateOf(1) }

    // Anim per bush
    val bushRot = remember(numberOfBushes) { List(numberOfBushes) { Animatable(0f) } }
    val bushScale = remember(numberOfBushes) { List(numberOfBushes) { Animatable(1f) } }

    suspend fun shakeBush(index: Int, amplitude: Float = 6f) {
        if (index !in 0 until numberOfBushes) return
        val anim = bushRot[index]
        anim.snapTo(0f)
        anim.animateTo(amplitude, tween(80, easing = FastOutSlowInEasing))
        anim.animateTo(-amplitude, tween(80, easing = FastOutSlowInEasing))
        anim.animateTo(0f, tween(80, easing = FastOutSlowInEasing))
    }

    fun pulseBush(index: Int, big: Boolean = false) {
        if (index !in 0 until numberOfBushes) return
        scope.launch {
            val anim = bushScale[index]
            val target = if (big) 1.06f else 1.03f
            anim.animateTo(target, tween(90, easing = FastOutSlowInEasing))
            anim.animateTo(1f, tween(140, easing = FastOutSlowInEasing))
        }
    }

    // 1) ÎNCĂRCARE RESURSE
    LaunchedEffect(Unit) {
        val bunnySheet = BitmapFactory.decodeResource(context.resources, R.drawable.bunny_sheet)
        val frames = SpriteTools.processSpriteSheet(
            fullBitmap = bunnySheet,
            rows = 4,
            cols = 6,
            applySafetyCrop = true,
            removeBackgroundColor = null
        )
        bunnyFrames = frames.map { it.asImageBitmap() }

        val rawBush = BitmapFactory.decodeResource(context.resources, R.drawable.prop_bush)
        bushImage = rawBush.asImageBitmap()
    }

    // 2) HINT LOOP
    LaunchedEffect(hidingSpotIndex, phase) {
        if (phase != PeekABooPhase.Searching) return@LaunchedEffect
        showPeekHint = false

        var elapsedMs = 0L
        while (isActive && phase == PeekABooPhase.Searching) {
            delay(3000)
            elapsedMs += 3000
            scope.launch { shakeBush(hidingSpotIndex, amplitude = 4.5f) }
            if (elapsedMs >= 9000) showPeekHint = true
        }
    }

    // 3) LOGICA ANIMATIEI - FRAME BY FRAME
    LaunchedEffect(phase) {
        when (phase) {
            PeekABooPhase.Searching -> {
                confettiVisible = false
                currentFrameIndex = 0
            }
            PeekABooPhase.Revealing -> {
                // Iepurasul incepe animatia de la frame 12 (pregatire) pana la 23 (aterizare)
                currentFrameIndex = 12
                while (currentFrameIndex < 23 && isActive) {
                    delay(80) // Viteza animatiei
                    currentFrameIndex++
                }
                confettiKey++
                confettiVisible = true
                runCatching {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                }
                phase = PeekABooPhase.Celebrating
            }
            PeekABooPhase.Celebrating -> {
                delay(850)
                var newSpot = Random.nextInt(numberOfBushes)
                while (newSpot == hidingSpotIndex) newSpot = Random.nextInt(numberOfBushes)
                hidingSpotIndex = newSpot

                showPeekHint = false
                confettiVisible = false
                currentFrameIndex = 0
                phase = PeekABooPhase.Searching
            }
        }
    }

    fun onBushTap(index: Int) {
        if (phase != PeekABooPhase.Searching) return
        pulseBush(index)
        if (index == hidingSpotIndex) {
            pulseBush(index, big = true)
            phase = PeekABooPhase.Revealing
        } else {
            scope.launch { shakeBush(index, amplitude = 7.5f) }
            runCatching {
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            }
        }
    }

    // --- UI ---
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.bg_sunny_meadow),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        SparkleOverlay(enabled = enableSparkles, modifier = Modifier.fillMaxSize())
        ConfettiOverlay(visible = confettiVisible, burstKey = confettiKey, modifier = Modifier.fillMaxSize())

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            val maxW = maxWidth
            val bushSlotSize: Dp = (maxW / numberOfBushes.toFloat()) * 0.92f
            val bushSize: Dp = bushSlotSize.coerceIn(160.dp, 280.dp)
            val bunnySize: Dp = bushSize * 0.86f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 46.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                repeat(numberOfBushes) { index ->
                    val rotation = bushRot[index].value
                    val scale = bushScale[index].value

                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier
                            .size(bushSize)
                            .scale(scale)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onBushTap(index) }
                    ) {
                        // 1) IEPURAȘ (Layer 1 - Spate)
                        if (index == hidingSpotIndex && bunnyFrames.isNotEmpty()) {
                            when (phase) {
                                PeekABooPhase.Searching -> {
                                    if (showPeekHint) {
                                        Image(
                                            bitmap = bunnyFrames[0],
                                            contentDescription = "Hint",
                                            modifier = Modifier
                                                .size(bunnySize)
                                                .offset(y = (-bushSize * 0.25f)) // Un mic peek
                                                .alpha(0.9f)
                                        )
                                    }
                                }
                                PeekABooPhase.Revealing,
                                PeekABooPhase.Celebrating -> {
                                    val idxSafe = currentFrameIndex.coerceIn(0, bunnyFrames.lastIndex)

                                    // >>> LOGICA NOUĂ DE SĂRITURĂ <<<
                                    // Calculăm înălțimea săriturii în funcție de frame-ul curent
                                    // Frame 15,16,17 sunt vârful săriturii -> offset mare negativ
                                    val jumpOffset = when (idxSafe) {
                                        13 -> (-20).dp  // Început
                                        14 -> (-70).dp  // Urcare
                                        15 -> (-130).dp // Vârf 1
                                        16 -> (-140).dp // Vârf Maxim (Deasupra tufișului)
                                        17 -> (-130).dp // Început coborâre
                                        18 -> (-70).dp  // Coborâre
                                        19 -> (-20).dp  // Aterizare
                                        else -> 0.dp
                                    }

                                    Image(
                                        bitmap = bunnyFrames[idxSafe],
                                        contentDescription = "Bunny",
                                        modifier = Modifier
                                            .size(bunnySize)
                                            .offset(y = jumpOffset) // Aici aplicăm săritura
                                    )
                                }
                            }
                        }

                        // 2) TUFIȘ (Layer 2 - Față)
                        bushImage?.let { img ->
                            Image(
                                bitmap = img,
                                contentDescription = "Bush",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .rotate(rotation)
                            )
                        }
                    }
                }
            }
            
            // Text Intrebare
            if (phase == PeekABooPhase.Searching) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 22.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text("Unde s-a ascuns iepurașul?", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Bravo!
            if (phase == PeekABooPhase.Celebrating) {
                 Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 22.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.90f))
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text("Bravo!", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20))
                }
            }

            // Buton Home
            Image(
                // Folosește icon_game_hiddenobjects temporar dacă nu ai ui_button_home, altfel pune ui_button_home
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.icon_game_hiddenobjects),
                contentDescription = "Home",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
                    .size(64.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onHome() }
            )
        }
    }
}