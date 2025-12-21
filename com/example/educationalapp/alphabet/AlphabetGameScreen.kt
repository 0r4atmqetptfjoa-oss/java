package com.example.educationalapp.alphabet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.isActive
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AlphabetGameScreen(
    viewModel: AlphabetGameViewModel = hiltViewModel(),
    onBackToMenu: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    val soundPlayer = remember { AlphabetSoundPlayer(context) }
    DisposableEffect(Unit) {
        onDispose { soundPlayer.release() }
    }

    // --- CONFETTI ---
    var confettiBurstId by remember { mutableLongStateOf(0L) }

    LaunchedEffect(uiState.isAnswerCorrect) {
        if (uiState.isAnswerCorrect == true) {
            soundPlayer.playCorrect()
            confettiBurstId = System.currentTimeMillis()
        } else if (uiState.isAnswerCorrect == false) {
            soundPlayer.playWrong()
            confettiBurstId = 0L
        }
    }

    ConfettiBox(burstId = confettiBurstId) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // 1. FUNDAL
            Image(
                painter = painterResource(id = AlphabetUi.Backgrounds.sky),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Decor oraș jos
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Image(
                    painter = painterResource(id = AlphabetUi.Backgrounds.city),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.4f).alpha(0.6f),
                    contentScale = ContentScale.FillBounds
                )
            }

            // 2. STRUCTURA PRINCIPALĂ
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding() 
            ) {

                // =============================================================
                // ZONA 1: HEADER (Sus - Scor și Sunet)
                // =============================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.15f) // Ocupă 15% din ecran sus
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Scor
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.9f),
                        shadowElevation = 4.dp
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(painter = painterResource(id = AlphabetUi.Icons.star), contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${uiState.score}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFFE65100))
                        }
                    }
                    // Sunet
                    SquishyButton(onClick = { soundPlayer.playClick(); viewModel.toggleSound() }, size = 42.dp) {
                        Image(
                            painter = painterResource(id = AlphabetUi.Icons.soundOn),
                            contentDescription = "Sound",
                            modifier = Modifier.size(24.dp).alpha(if (uiState.soundOn) 1f else 0.5f)
                        )
                    }
                }

                // =============================================================
                // ZONA 2: MIJLOC (CARTONAȘ + NUME)
                // Aici faci modificările manuale!
                // =============================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.55f) // Ocupă 55% din ecran (Mijloc)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    
                    // --- ZONA CARTONAȘULUI (Stânga) ---
                    Box(
                        modifier = Modifier
                            .weight(0.6f) // Zona cartonașului e mai lată
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        // >>>>> AICI MODIFICI DIMENSIUNEA ȘI POZIȚIA CARTONAȘULUI <<<<<
                        Box(
                            modifier = Modifier
                                // 1. LĂȚIME ȘI ÎNĂLȚIME (MANUAL)
                                .width(300.dp)   // <--- MODIFICĂ AICI LĂȚIMEA (ex: 200.dp e mai mic, 250.dp e mai mare)
                                .height(220.dp)  // <--- MODIFICĂ AICI ÎNĂLȚIMEA (pune la fel ca la width pentru pătrat)

                                // 2. POZIȚIA (MANUAL)
                                // x: pozitiv = dreapta, negativ = stânga
                                // y: pozitiv = jos, negativ = sus
                                .offset(x = 15.dp, y = -20.dp) // <--- MODIFICĂ AICI DACĂ VREI SĂ ÎL MUȚI

                                .shadow(10.dp, RoundedCornerShape(20.dp))
                                .background(Color.White, RoundedCornerShape(20.dp))
                                .padding(6.dp)
                        ) {
                            val centerBmp = rememberScaledImageBitmap(uiState.currentQuestion.imageRes, maxDim = 800)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Brush.verticalGradient(colors = listOf(Color(0xFFE0F7FA), Color(0xFFB2EBF2)))),
                                    contentAlignment = Alignment.Center
                            ) {
                                if (centerBmp != null) {
                                    Image(bitmap = centerBmp, contentDescription = null, modifier = Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit)
                                } else {
                                    Image(painter = painterResource(id = uiState.currentQuestion.imageRes), contentDescription = null, modifier = Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit)
                                }
                            }
                        }
                    }

                    // --- ZONA NUMELUI (Dreapta) ---
                    Box(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.CenterStart // Textul stă în stânga zonei lui (lângă cartonaș)
                    ) {
                        val formattedWord = if (uiState.isAnswerCorrect == true) uiState.currentQuestion.word.replaceFirstChar { it.uppercaseChar() } else "_" + uiState.currentQuestion.word.drop(1)
                        
                        AnimatedContent(targetState = formattedWord, label = "word") { word ->
                            Surface(
                                // >>>>> AICI MODIFICI POZIȚIA TEXTULUI <<<<<
                                modifier = Modifier.offset(x = (-10).dp, y = 0.dp), // <--- MODIFICĂ AICI (x negativ îl trage spre cartonaș)
                                
                                color = Color(0xFFFFF9C4), 
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFFFFB74D)),
                                shadowElevation = 3.dp
                            ) {
                                Text(
                                    text = word,
                                    fontSize = 26.sp, // Mărime text
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFE65100),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }

                // =============================================================
                // ZONA 3: JOS (MASCOTA & BUTOANE)
                // =============================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.3f) // Ocupă 30% din ecran (Jos)
                ) {
                    
                    // --- MASCOTA (Stânga Jos) ---
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 10.dp, bottom = 0.dp)
                            .fillMaxHeight()
                            .width(150.dp) // <--- LĂȚIME MASCOTĂ
                            .zIndex(1f),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        val mascotRes = when (uiState.mascotMood) {
                            MascotMood.HAPPY -> AlphabetUi.Mascot.happy
                            MascotMood.SURPRISED -> AlphabetUi.Mascot.surprised
                            else -> AlphabetUi.Mascot.normal
                        }
                        Image(
                            painter = painterResource(id = mascotRes),
                            contentDescription = "Mascot",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.BottomStart
                        )
                    }

                    // --- BUTOANELE (Dreapta / Centru) ---
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 30.dp)
                            .zIndex(5f),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (uiState.isFinished) {
                            Button(
                                onClick = { 
                                    soundPlayer.playClick()
                                    viewModel.resetGame() 
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                modifier = Modifier.height(60.dp)
                            ) {
                                Text("JOACĂ DIN NOU", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(
                                modifier = Modifier.padding(start = 120.dp), // Le dăm la dreapta să nu intre peste mascotă
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                uiState.options.forEachIndexed { index, option ->
                                    val isSelectedCorrect = uiState.isAnswerCorrect == true && AlphabetAssets.normalizeBase(option) == AlphabetAssets.normalizeBase(uiState.currentQuestion.displayLetter)
                                    val isSelectedWrong = uiState.isAnswerCorrect == false && uiState.selectedOption == option
                                    
                                    val containerColor = when {
                                        isSelectedCorrect -> Color(0xFF4CAF50)
                                        isSelectedWrong -> Color(0xFFEF5350)
                                        else -> Color(0xFFFF9800)
                                    }

                                    if (index > 0) Spacer(modifier = Modifier.width(16.dp))

                                    SquishyButton(
                                        onClick = { 
                                            if (!uiState.isInputLocked) {
                                                soundPlayer.playClick()
                                                viewModel.selectAnswer(option)
                                            }
                                        },
                                        modifier = Modifier.size(80.dp), // <--- DIMENSIUNE BUTOANE
                                        shape = CircleShape,
                                        color = containerColor,
                                        elevation = 8.dp
                                    ) {
                                        Text(
                                            text = option,
                                            fontSize = 38.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Buton Home
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        SquishyButton(
                            onClick = { soundPlayer.playClick(); onBackToMenu() },
                            size = 50.dp,
                            shape = CircleShape
                        ) {
                            Image(painter = painterResource(id = AlphabetUi.Icons.home), contentDescription = "Home", modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
    }
}

// --- CLASE AUXILIARE ---

fun Modifier.alpha(alpha: Float) = this.then(Modifier.graphicsLayer(alpha = alpha))

@Composable
fun SquishyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    color: Color = Color.White,
    elevation: Dp = 4.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(targetValue = if (isPressed) 0.85f else 1f, animationSpec = spring(stiffness = Spring.StiffnessMedium), label = "btnScale")

    Surface(
        onClick = onClick,
        modifier = modifier.scale(buttonScale).let { if (size != null) it.size(size) else it },
        shape = shape,
        color = color,
        shadowElevation = elevation,
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center, content = content)
    }
}

data class ConfettiParticle(
    val id: Int, 
    var x: Float, 
    var y: Float, 
    val color: Color, 
    val scale: Float, 
    val rotationSpeed: Float,
    var currentRotation: Float,
    val initialSpeedX: Float
)

@Composable
fun ConfettiBox(burstId: Long, content: @Composable () -> Unit) {
    val colors = listOf(Color(0xFFFFC107), Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFE91E63), Color(0xFFFF5722))
    val particles = remember { mutableStateListOf<ConfettiParticle>() }
    val density = LocalDensity.current

    LaunchedEffect(burstId) {
        particles.clear()
        if (burstId > 0) { 
            repeat(80) { id -> 
                val startX = with(density) { Random.nextFloat() * 400.dp.toPx() } 
                val startY = with(density) { -50.dp.toPx() } 
                particles.add(
                    ConfettiParticle(
                        id = id,
                        x = startX,
                        y = startY,
                        color = colors.random(),
                        scale = Random.nextFloat() * 0.4f + 0.6f, 
                        rotationSpeed = (Random.nextFloat() - 0.5f) * 10f, 
                        currentRotation = Random.nextFloat() * 360f,
                        initialSpeedX = (Random.nextFloat() - 0.5f) * 5f 
                    )
                )
            }
            val startTime = withFrameNanos { it }
            while (isActive && particles.isNotEmpty()) {
                withFrameNanos { time ->
                    val elapsed = (time - startTime) / 1_000_000_000f 
                    val newParticles = particles.map { p ->
                        val fallSpeed = 600f + (Random.nextFloat() * 200f) 
                        val sway = sin(elapsed * 5f + p.id) * 2f 
                        p.apply {
                            y += fallSpeed * 0.016f 
                            x += initialSpeedX + sway
                            currentRotation += rotationSpeed
                        }
                    }.filter { it.y < with(density) { 1000.dp.toPx() } } 
                    particles.clear()
                    particles.addAll(newParticles)
                }
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        if (particles.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize().zIndex(999f)) {
                particles.forEach { p ->
                    withTransform({ translate(p.x, p.y); rotate(p.currentRotation); scale(p.scale, p.scale) }) {
                        drawRect(color = p.color, topLeft = Offset(-12f, -8f), size = androidx.compose.ui.geometry.Size(24f, 16f))
                    }
                }
            }
        }
    }
}