package com.example.educationalapp.alphabet

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random
import kotlin.jvm.JvmSynthetic

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AlphabetGameScreen(
    viewModel: AlphabetGameViewModel = hiltViewModel(),
    onBackToMenu: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val soundPlayer = remember { AlphabetSoundPlayer(context) }
    DisposableEffect(Unit) { onDispose { soundPlayer.release() } }
    LaunchedEffect(uiState.soundOn) { soundPlayer.isEnabled = uiState.soundOn }

    // --- STĂRI ANIMAȚII ---
    var confettiBurstId by remember { mutableLongStateOf(0L) }
    val shakeX = remember { Animatable(0f) }

    // Stare pentru litera zburătoare
    var flyingLetter by remember { mutableStateOf<String?>(null) }
    val flyingAnimatable = remember { Animatable(0f) }

    // Reacții
    LaunchedEffect(uiState.isAnswerCorrect) {
        when (uiState.isAnswerCorrect) {
            true -> {
                soundPlayer.playCorrect()
                confettiBurstId = System.currentTimeMillis()
            }
            false -> {
                soundPlayer.playWrong()
                confettiBurstId = 0L
                shakeX.snapTo(0f)
                shakeX.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 450
                        0f at 0; -18f at 60; 18f at 120; -14f at 180; 14f at 240; -8f at 300; 8f at 360; 0f at 450
                    }
                )
            }
            else -> Unit
        }
    }

    ConfettiBox(burstId = confettiBurstId) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(painter = painterResource(id = AlphabetUi.Backgrounds.sky), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Image(painter = painterResource(id = AlphabetUi.Backgrounds.city), contentDescription = null, modifier = Modifier.fillMaxWidth().fillMaxHeight(0.4f).alpha(0.6f), contentScale = ContentScale.FillBounds)
            }
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.10f)))))

            Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                HeaderBar(
                    score = uiState.score, stars = uiState.stars, questionIndex = uiState.questionIndex, totalQuestions = uiState.totalQuestions, attemptsLeft = uiState.attemptsLeft, soundOn = uiState.soundOn,
                    onToggleSound = { if (uiState.soundOn) soundPlayer.playClick(); viewModel.toggleSound(); if (!uiState.soundOn) soundPlayer.playClick() },
                    onHome = { soundPlayer.playClick(); onBackToMenu() }
                )

                Spacer(modifier = Modifier.height(8.dp))

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val mainMaxWidth = this.maxWidth
                    val mainMaxHeight = this.maxHeight
                    val isLandscape = mainMaxWidth > mainMaxHeight
                    val gap = if (isLandscape) 10.dp else 12.dp

                    val cardWidth = (mainMaxWidth * if (isLandscape) 0.60f else 0.72f).coerceIn(300.dp, if (isLandscape) 760.dp else 420.dp)
                    val cardHeight = (mainMaxHeight * if (isLandscape) 0.65f else 0.48f).coerceIn(200.dp, if (isLandscape) 440.dp else 280.dp)
                    val mascotBaseSize = if (isLandscape) 150.dp else 135.dp
                    val optionBaseSize = if (isLandscape) 90.dp else 94.dp

                    val isCorrect = uiState.isAnswerCorrect == true
                    val isWrong = uiState.isAnswerCorrect == false
                    val borderColor by animateColorAsState(targetValue = when { isCorrect -> Color(0xFF43A047); isWrong -> Color(0xFFE53935); else -> Color(0xFFFF9800) }, label = "cardBorder")
                    val cardScale by animateFloatAsState(targetValue = if (isCorrect) 1.03f else if (isWrong) 0.99f else 1.0f, label = "cardScale")
                    
                    val mascotRes = when (uiState.mascotMood) { MascotMood.HAPPY, MascotMood.CELEBRATE -> AlphabetUi.Mascot.happy; MascotMood.SURPRISED -> AlphabetUi.Mascot.surprised; MascotMood.THINKING -> AlphabetUi.Mascot.thinking; else -> AlphabetUi.Mascot.normal }

                    if (isLandscape) {
                        // =========================================================================
                        // LANDSCAPE MODE
                        // =========================================================================
                        
                        // >>>>>> ZONA DE CONFIGURARE MANUALĂ (AICI MODIFICI POZIȚIILE) <<<<<<
                        
                        // 1. POZIȚIE CARD (IMAGINE)
                        val cardOffsetX = 0.dp        // Stânga (-) / Dreapta (+)
                        val cardOffsetY = (-10).dp    // Sus (-) / Jos (+)

                        // 2. POZIȚIE LITERE (BUTOANE) - Sunt sub card
                        val lettersOffsetX = 0.dp     // Stânga (-) / Dreapta (+)
                        val lettersOffsetY = 10.dp    // Sus (-) / Jos (+)

                        // 3. POZIȚIE MASCOTĂ (VULPEA)
                        val mascotOffsetX = (-10).dp  // Stânga (-) / Dreapta (+)
                        val mascotOffsetY = 20.dp     // Sus (-) / Jos (+)

                        // 4. POZIȚIE CUVÂNT (TEXT SUS)
                        val wordOffsetX = 0.dp        // Stânga (-) / Dreapta (+)
                        val wordOffsetY = 30.dp       // Sus (-) / Jos (+)
                        
                        // >>>>>> FINAL ZONA DE CONFIGURARE <<<<<<


                        // Logică afișare cuvânt (Magic Sync)
                        var displayedWord by remember { mutableStateOf("") }
                        var isWordPopping by remember { mutableStateOf(false) }
                        val wordScale by animateFloatAsState(if (isWordPopping) 1.2f else 1f, spring(dampingRatio = 0.5f))

                        LaunchedEffect(uiState.currentQuestion, uiState.isAnswerCorrect) {
                            val w = uiState.currentQuestion.word
                            val masked = if (w.isNotEmpty()) "_" + w.drop(1) else "_"
                            val full = w.replaceFirstChar { it.uppercaseChar() }

                            if (uiState.isAnswerCorrect == true) {
                                delay(800) // Așteptăm zborul literei
                                displayedWord = full
                                isWordPopping = true
                                delay(150)
                                isWordPopping = false
                            } else {
                                displayedWord = masked
                            }
                        }

                        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                            // --- STÂNGA: CARD + BUTOANE ---
                            Column(modifier = Modifier.weight(0.65f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                // CARD
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.offset(x = cardOffsetX, y = cardOffsetY) // <--- Folosește variabila ta
                                ) {
                                    val w = minOf(cardWidth, mainMaxWidth * 0.70f)
                                    val h = minOf(cardHeight, mainMaxHeight * 0.85f)
                                    Box(modifier = Modifier.width(w).height(h).scale(cardScale).graphicsLayer { translationX = shakeX.value }.shadow(12.dp, RoundedCornerShape(22.dp)).background(Color.White, RoundedCornerShape(22.dp)).border(BorderStroke(4.dp, borderColor), RoundedCornerShape(22.dp)).padding(7.dp)) {
                                        val centerBmp = rememberScaledImageBitmap(uiState.currentQuestion.imageRes, maxDim = 1100)
                                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(Brush.verticalGradient(listOf(Color(0xFFE3F2FD), Color(0xFFF1F8E9)))), contentAlignment = Alignment.Center) {
                                            if (centerBmp != null) Image(bitmap = centerBmp, contentDescription = null, modifier = Modifier.fillMaxSize(0.97f), contentScale = ContentScale.Fit)
                                            else Image(painter = painterResource(id = uiState.currentQuestion.imageRes), contentDescription = null, modifier = Modifier.fillMaxSize(0.97f), contentScale = ContentScale.Fit)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                // BUTOANE LITERE
                                if (!uiState.isFinished) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                                        modifier = Modifier.offset(x = lettersOffsetX, y = lettersOffsetY) // <--- Folosește variabila ta
                                    ) {
                                        uiState.options.forEach { option ->
                                            val isCorrectOption = AlphabetAssets.normalizeBase(option) == AlphabetAssets.normalizeBase(uiState.currentQuestion.displayLetter)
                                            val isSelectedCorrect = uiState.isAnswerCorrect == true && isCorrectOption
                                            val isSelectedWrong = uiState.isAnswerCorrect == false && uiState.selectedOption == option
                                            val containerColor = when { isSelectedCorrect -> Color(0xFF4CAF50); isSelectedWrong -> Color(0xFFEF5350); else -> Color(0xFFFF9800) }

                                            SquishyButton(
                                                onClick = {
                                                    if (!uiState.isInputLocked) {
                                                        soundPlayer.playClick()
                                                        if (isCorrectOption) {
                                                            flyingLetter = option
                                                            scope.launch {
                                                                flyingAnimatable.snapTo(0f)
                                                                flyingAnimatable.animateTo(1f, animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing))
                                                                flyingLetter = null
                                                            }
                                                        }
                                                        viewModel.selectAnswer(option)
                                                    }
                                                },
                                                size = optionBaseSize, shape = CircleShape, color = containerColor, elevation = 10.dp
                                            ) {
                                                Text(text = option, fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }

                            // --- DREAPTA: CUVÂNT + MASCOTĂ ---
                            Box(modifier = Modifier.weight(0.35f).fillMaxHeight()) {
                                // CUVÂNT
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(x = wordOffsetX, y = wordOffsetY) // <--- Folosește variabila ta
                                ) {
                                    Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFFFFF3E0).copy(alpha = 0.95f), shadowElevation = 6.dp) {
                                        Text(
                                            text = displayedWord,
                                            fontSize = 44.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFE65100),
                                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp).scale(wordScale),
                                            letterSpacing = 1.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (uiState.isFinished) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        FinishPanel(score = uiState.score, stars = uiState.stars, total = uiState.totalQuestions, onRestart = { soundPlayer.playClick(); viewModel.resetGame() }, onBackToMenu = { soundPlayer.playClick(); onBackToMenu() })
                                    }
                                } else {
                                    // MASCOTA
                                    Image(
                                        painter = painterResource(id = mascotRes),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(mascotBaseSize * 1.2f)
                                            .offset(x = mascotOffsetX, y = mascotOffsetY), // <--- Folosește variabila ta
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    } else {
                        // --- PORTRAIT MODE ---
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(0.6f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                    Box(modifier = Modifier.width(cardWidth).height(cardHeight).scale(cardScale).graphicsLayer { translationX = shakeX.value }.shadow(12.dp, RoundedCornerShape(22.dp)).background(Color.White, RoundedCornerShape(22.dp)).border(BorderStroke(4.dp, borderColor), RoundedCornerShape(22.dp)).padding(7.dp)) {
                                        val centerBmp = rememberScaledImageBitmap(uiState.currentQuestion.imageRes, maxDim = 900)
                                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(Brush.verticalGradient(listOf(Color(0xFFE3F2FD), Color(0xFFF1F8E9)))), contentAlignment = Alignment.Center) {
                                            if (centerBmp != null) Image(bitmap = centerBmp, contentDescription = null, modifier = Modifier.fillMaxSize(0.92f), contentScale = ContentScale.Fit)
                                            else Image(painter = painterResource(id = uiState.currentQuestion.imageRes), contentDescription = null, modifier = Modifier.fillMaxSize(0.92f), contentScale = ContentScale.Fit)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Box(modifier = Modifier.weight(0.4f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                                    val formattedWord = if (uiState.isAnswerCorrect == true) uiState.currentQuestion.word.replaceFirstChar { it.uppercaseChar() } else "_" + uiState.currentQuestion.word.drop(1)
                                    AnimatedContent(targetState = formattedWord, label = "word") { w ->
                                        Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(16.dp), shadowElevation = 6.dp, modifier = Modifier.padding(end = 8.dp)) {
                                            Text(text = w, fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color(0xFFE65100), modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), letterSpacing = 1.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(gap))
                            Row(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 14.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Image(painter = painterResource(id = mascotRes), contentDescription = "Mascot", modifier = Modifier.size(mascotBaseSize), contentScale = ContentScale.Fit, alignment = Alignment.BottomStart)
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
                                    if (uiState.isFinished) {
                                        FinishPanel(score = uiState.score, stars = uiState.stars, total = uiState.totalQuestions, onRestart = { soundPlayer.playClick(); viewModel.resetGame() }, onBackToMenu = { soundPlayer.playClick(); onBackToMenu() })
                                    } else {
                                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            uiState.options.forEach { option ->
                                                val isSelectedCorrect = uiState.isAnswerCorrect == true && AlphabetAssets.normalizeBase(option) == AlphabetAssets.normalizeBase(uiState.currentQuestion.displayLetter)
                                                val isSelectedWrong = uiState.isAnswerCorrect == false && uiState.selectedOption == option
                                                val containerColor = when { isSelectedCorrect -> Color(0xFF4CAF50); isSelectedWrong -> Color(0xFFEF5350); else -> Color(0xFFFF9800) }
                                                SquishyButton(onClick = { if (!uiState.isInputLocked) { soundPlayer.playClick(); viewModel.selectAnswer(option) } }, size = optionBaseSize, shape = CircleShape, color = containerColor, elevation = 10.dp) {
                                                    Text(text = option, fontSize = 44.sp, fontWeight = FontWeight.Black, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- LITERA MAGICĂ (FLYING LETTER) ---
                    if (flyingLetter != null && isLandscape) {
                        val progress = flyingAnimatable.value
                        
                        // Poți ajusta manual punctele de start și finish și aici
                        val startPos = Offset(with(LocalDensity.current) { mainMaxWidth.toPx() * 0.25f }, with(LocalDensity.current) { mainMaxHeight.toPx() * 0.8f })
                        val endPos = Offset(with(LocalDensity.current) { mainMaxWidth.toPx() * 0.80f }, with(LocalDensity.current) { mainMaxHeight.toPx() * 0.15f })
                        
                        val currentPos = startPos + (endPos - startPos) * progress
                        val scale = 1f + (0.6f - 1f) * progress
                        val rotation = 0f + (360f - 0f) * progress
                        val alpha = 1f - (progress * 1.3f).coerceAtMost(1f)

                        Box(modifier = Modifier.fillMaxSize().zIndex(100f)) {
                            Text(
                                text = flyingLetter!!,
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFE65100),
                                modifier = Modifier.graphicsLayer {
                                    translationX = currentPos.x
                                    translationY = currentPos.y
                                    scaleX = scale
                                    scaleY = scale
                                    rotationZ = rotation
                                    this.alpha = alpha
                                }
                            )
                        }
                    }

                } // End BoxWithConstraints
            } // End Column
        } // End Box
    } // End Confetti
}

// RESTUL COMPONENTELOR (HeaderBar, FinishPanel, SquishyButton, ConfettiBox) RĂMÂN NESCHIMBATE
@Composable
private fun HeaderBar(score: Int, stars: Int, questionIndex: Int, totalQuestions: Int, attemptsLeft: Int, soundOn: Boolean, onToggleSound: () -> Unit, onHome: () -> Unit) {
    val progress = if (totalQuestions > 0) (questionIndex + 1) / totalQuestions.toFloat() else 0f
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(999.dp), color = Color.White.copy(alpha = 0.92f), shadowElevation = 4.dp) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painter = painterResource(id = AlphabetUi.Icons.star), contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = score.toString(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF263238))
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "★".repeat(stars.coerceAtMost(10)), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF9800))
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f).height(if (isLandscape) 42.dp else 46.dp).clip(RoundedCornerShape(999.dp)).background(Color.White.copy(alpha = 0.90f))) {
            LinearProgressIndicator(progress = progress.coerceIn(0f, 1f), modifier = Modifier.fillMaxSize(), color = Color(0xFFFF9800), trackColor = Color(0xFFECEFF1))
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Întrebarea ${questionIndex + 1} / $totalQuestions", fontSize = if (isLandscape) 14.sp else 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF37474F), maxLines = 1, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "${(progress * 100f).toInt()}%", fontSize = if (isLandscape) 13.sp else 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF37474F))
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Surface(shape = RoundedCornerShape(999.dp), color = Color.White.copy(alpha = 0.92f), shadowElevation = 4.dp) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Șanse:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF455A64))
                Spacer(modifier = Modifier.width(8.dp))
                val hearts = "❤".repeat(attemptsLeft.coerceAtLeast(0))
                Text(text = if (hearts.isEmpty()) "—" else hearts, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFE53935))
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        SquishyButton(onClick = onToggleSound, size = 42.dp, shape = CircleShape) {
            Image(painter = painterResource(id = AlphabetUi.Icons.soundOn), contentDescription = "Sound", modifier = Modifier.size(24.dp).alpha(if (soundOn) 1f else 0.45f))
        }
        Spacer(modifier = Modifier.width(8.dp))
        SquishyButton(onClick = onHome, size = 42.dp, shape = CircleShape) {
            Image(painter = painterResource(id = AlphabetUi.Icons.home), contentDescription = "Home", modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun FinishPanel(score: Int, stars: Int, total: Int, onRestart: () -> Unit, onBackToMenu: () -> Unit) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Surface(shape = RoundedCornerShape(22.dp), color = Color.White.copy(alpha = 0.92f), shadowElevation = 10.dp, modifier = Modifier.padding(horizontal = 18.dp).fillMaxWidth(0.85f)) {
        Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Felicitări!", fontSize = if (isLandscape) 32.sp else 36.sp, fontWeight = FontWeight.Black, color = Color(0xFFE65100))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Scor: $score", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF37474F))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Stele: $stars / $total", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF37474F))
            Spacer(modifier = Modifier.height(14.dp))
            SquishyButton(onClick = onRestart, modifier = Modifier.height(56.dp).fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color(0xFFFF9800), elevation = 8.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
                    Image(painter = painterResource(id = AlphabetUi.Icons.replay), contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "JOACĂ DIN NOU", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            SquishyButton(onClick = onBackToMenu, modifier = Modifier.height(52.dp).fillMaxWidth().border(2.dp, Color(0xFFF39C12), RoundedCornerShape(18.dp)), shape = RoundedCornerShape(18.dp), color = Color.White, elevation = 0.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
                    Image(painter = painterResource(id = AlphabetUi.Icons.home), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "MENIU", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color(0xFFF39C12))
                }
            }
        }
    }
}

@JvmSynthetic
@Composable
private fun SquishyButton(onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp? = null, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp), color: Color = Color.White, elevation: Dp = 4.dp, content: @Composable BoxScope.() -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(targetValue = if (isPressed) 0.86f else 1f, animationSpec = spring(stiffness = Spring.StiffnessMedium), label = "btnScale")
    Surface(onClick = onClick, modifier = modifier.scale(buttonScale).let { if (size != null) it.size(size) else it }, shape = shape, color = color, shadowElevation = elevation, interactionSource = interactionSource) { Box(contentAlignment = Alignment.Center, content = content) }
}

data class ConfettiParticle(val id: Int, var x: Float, var y: Float, val color: Color, val scale: Float, val rotationSpeed: Float, var currentRotation: Float, var vx: Float, var vy: Float)

@JvmSynthetic
@Composable
private fun ConfettiBox(burstId: Long, content: @Composable () -> Unit) {
    val colors = listOf(Color(0xFFFFC107), Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFE91E63), Color(0xFFFF5722))
    val particles = remember { mutableStateListOf<ConfettiParticle>() }
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        LaunchedEffect(burstId, widthPx, heightPx) {
            particles.clear()
            if (burstId > 0L) {
                repeat(80) { id ->
                    val startX = Random.nextFloat() * widthPx
                    val startY = -with(density) { 40.dp.toPx() }
                    particles.add(ConfettiParticle(id = id, x = startX, y = startY, color = colors.random(), scale = Random.nextFloat() * 0.4f + 0.6f, rotationSpeed = (Random.nextFloat() - 0.5f) * 260f, currentRotation = Random.nextFloat() * 360f, vx = (Random.nextFloat() - 0.5f) * 220f, vy = 720f + (Random.nextFloat() * 320f)))
                }
                var lastTime = withFrameNanos { it }
                while (isActive && particles.isNotEmpty()) {
                    withFrameNanos { now ->
                        val dt = (now - lastTime) / 1_000_000_000f; lastTime = now; val t = now / 1_000_000_000f
                        val newParticles = particles.map { p -> val sway = (sin((t * 4.8f + p.id).toDouble()) * 28.0).toFloat(); p.apply { x += (vx + sway) * dt; y += vy * dt; currentRotation += rotationSpeed * dt } }.filter { it.y < heightPx + with(density) { 120.dp.toPx() } }
                        particles.clear(); particles.addAll(newParticles)
                    }
                }
            }
        }
        Box(modifier = Modifier.fillMaxSize()) { content(); if (particles.isNotEmpty()) { Canvas(modifier = Modifier.fillMaxSize().zIndex(999f)) { particles.forEach { p -> withTransform({ translate(p.x, p.y); rotate(p.currentRotation); scale(p.scale, p.scale) }) { drawRect(color = p.color, topLeft = Offset(-12f, -8f), size = androidx.compose.ui.geometry.Size(24f, 16f)) } } } } }
    }
}