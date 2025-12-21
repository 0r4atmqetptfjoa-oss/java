package com.example.educationalapp.alphabet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
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

    // Păstrăm player-ul sincronizat cu toggle-ul din UI.
    LaunchedEffect(uiState.soundOn) {
        soundPlayer.isEnabled = uiState.soundOn
    }

    // --- CONFETTI ---
    var confettiBurstId by remember { mutableLongStateOf(0L) }

    // --- SHAKE pentru greșit ---
    val shakeX = remember { Animatable(0f) }

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
                        0f at 0
                        -18f at 60
                        18f at 120
                        -14f at 180
                        14f at 240
                        -8f at 300
                        8f at 360
                        0f at 450
                    }
                )
            }
            else -> Unit
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.4f)
                        .alpha(0.6f),
                    contentScale = ContentScale.FillBounds
                )
            }

            // overlay subtil pentru contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.10f)
                            )
                        )
                    )
            )

            // 2. STRUCTURA PRINCIPALĂ
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {

                // =============================================================
                // HEADER: scor + progres + încercări + sunet
                // =============================================================
                HeaderBar(
                    score = uiState.score,
                    stars = uiState.stars,
                    questionIndex = uiState.questionIndex,
                    totalQuestions = uiState.totalQuestions,
                    attemptsLeft = uiState.attemptsLeft,
                    soundOn = uiState.soundOn,
                    onToggleSound = {
                        // Click-ul se aude și când OPRIM, și când PORNIM.
                        if (uiState.soundOn) soundPlayer.playClick()
                        val newSoundOn = !uiState.soundOn
                        soundPlayer.isEnabled = newSoundOn
                        viewModel.toggleSound()
                        if (newSoundOn) soundPlayer.playClick()
                    }
                )

                // =============================================================
                // MIJLOC (CARTONAȘ + NUME)
                // =============================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.58f)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // --- ZONA CARTONAȘULUI (Stânga) ---
                    Box(
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        val isCorrect = uiState.isAnswerCorrect == true
                        val isWrong = uiState.isAnswerCorrect == false

                        val cardScale by animateFloatAsState(
                            targetValue = if (isCorrect) 1.04f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "cardScale"
                        )

                        val borderColor by animateColorAsState(
                            targetValue = when {
                                isCorrect -> Color(0xFF43A047)
                                isWrong -> Color(0xFFE53935)
                                else -> Color(0xFFFFB74D)
                            },
                            label = "cardBorder"
                        )

                        Box(
                            modifier = Modifier
                                .width(300.dp)
                                .height(220.dp)
                                .offset(x = 12.dp, y = (-18).dp)
                                .scale(cardScale)
                                .graphicsLayer { translationX = shakeX.value }
                                .shadow(12.dp, RoundedCornerShape(22.dp))
                                .background(Color.White, RoundedCornerShape(22.dp))
                                .padding(7.dp)
                        ) {
                            val centerBmp = rememberScaledImageBitmap(uiState.currentQuestion.imageRes, maxDim = 900)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color(0xFFE0F7FA), Color(0xFFB2EBF2))
                                        )
                                    )
                                    .then(Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                // border/glow
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.Transparent)
                                        .then(
                                            Modifier
                                                .border(BorderStroke(3.dp, borderColor), RoundedCornerShape(16.dp))
                                        )
                                )
                                if (centerBmp != null) {
                                    Image(
                                        bitmap = centerBmp,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = uiState.currentQuestion.imageRes),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }

                    // --- ZONA NUMELUI (Dreapta) ---
                    Box(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        val formattedWord = if (uiState.isAnswerCorrect == true) {
                            uiState.currentQuestion.word.replaceFirstChar { it.uppercaseChar() }
                        } else {
                            "_" + uiState.currentQuestion.word.drop(1)
                        }

                        AnimatedContent(targetState = formattedWord, label = "word") { word ->
                            Surface(
                                modifier = Modifier.offset(x = (-8).dp, y = 0.dp),
                                color = Color(0xFFFFF9C4),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(3.dp, Color(0xFFFFB74D)),
                                shadowElevation = 4.dp
                            ) {
                                Text(
                                    text = word,
                                    fontSize = 26.sp,
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
                // JOS (MASCOTA & BUTOANE / REZULTAT)
                // =============================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.30f)
                ) {

                    // --- MASCOTA (Stânga Jos) ---
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 10.dp, bottom = 0.dp)
                            .fillMaxHeight()
                            .width(150.dp)
                            .zIndex(1f),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        val mascotRes = when (uiState.mascotMood) {
                            MascotMood.HAPPY, MascotMood.CELEBRATE -> AlphabetUi.Mascot.happy
                            MascotMood.SURPRISED -> AlphabetUi.Mascot.surprised
                            MascotMood.THINKING -> AlphabetUi.Mascot.thinking
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

                    // --- BUTOANE / REZULTAT ---
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 30.dp)
                            .zIndex(5f),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (uiState.isFinished) {
                            FinishPanel(
                                score = uiState.score,
                                stars = uiState.stars,
                                total = uiState.totalQuestions,
                                onReplay = {
                                    soundPlayer.playClick()
                                    viewModel.resetGame()
                                }
                            )
                        } else {
                            Row(
                                modifier = Modifier.padding(start = 120.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                uiState.options.forEachIndexed { index, option ->
                                    val isSelectedCorrect =
                                        uiState.isAnswerCorrect == true &&
                                            AlphabetAssets.normalizeBase(option) == AlphabetAssets.normalizeBase(uiState.currentQuestion.displayLetter)
                                    val isSelectedWrong =
                                        uiState.isAnswerCorrect == false && uiState.selectedOption == option

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
                                        modifier = Modifier.size(80.dp),
                                        shape = CircleShape,
                                        color = containerColor,
                                        elevation = 10.dp
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
                            Image(
                                painter = painterResource(id = AlphabetUi.Icons.home),
                                contentDescription = "Home",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderBar(
    score: Int,
    stars: Int,
    questionIndex: Int,
    totalQuestions: Int,
    attemptsLeft: Int,
    soundOn: Boolean,
    onToggleSound: () -> Unit
) {
    val progress = if (totalQuestions > 0) (questionIndex + 1) / totalQuestions.toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Scor + stele
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.92f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = AlphabetUi.Icons.star),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$score",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFE65100)
                    )

                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "★".repeat(stars.coerceAtMost(10)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFF9800)
                    )
                }
            }

            // Încercări + Sunet
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.92f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Șanse:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF455A64)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val hearts = "❤".repeat(attemptsLeft.coerceAtLeast(0))
                        Text(
                            text = if (hearts.isEmpty()) "—" else hearts,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFE53935)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                SquishyButton(onClick = onToggleSound, size = 42.dp, shape = CircleShape) {
                    Image(
                        painter = painterResource(id = AlphabetUi.Icons.soundOn),
                        contentDescription = "Sound",
                        modifier = Modifier
                            .size(24.dp)
                            .alpha(if (soundOn) 1f else 0.45f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progres
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.75f),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Întrebarea ${questionIndex + 1} / $totalQuestions",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF37474F)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF37474F)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = Color(0xFFFF9800),
                    trackColor = Color(0xFFECEFF1)
                )
            }
        }
    }
}

@Composable
private fun FinishPanel(
    score: Int,
    stars: Int,
    total: Int,
    onReplay: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.92f),
        shadowElevation = 10.dp,
        modifier = Modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth(0.85f)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Felicitări!",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFE65100)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Scor: $score",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF37474F)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Stele: $stars / $total",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF37474F)
            )
            Spacer(modifier = Modifier.height(14.dp))

            SquishyButton(
                onClick = onReplay,
                modifier = Modifier
                    .height(56.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFFF9800),
                elevation = 8.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(id = AlphabetUi.Icons.replay),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "JOACĂ DIN NOU",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// --- CLASE AUXILIARE ---

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
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "btnScale"
    )

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
    var vx: Float,
    var vy: Float
)

@Composable
fun ConfettiBox(burstId: Long, content: @Composable () -> Unit) {
    val colors = listOf(
        Color(0xFFFFC107),
        Color(0xFF4CAF50),
        Color(0xFF2196F3),
        Color(0xFFE91E63),
        Color(0xFFFF5722)
    )
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
                    particles.add(
                        ConfettiParticle(
                            id = id,
                            x = startX,
                            y = startY,
                            color = colors.random(),
                            scale = Random.nextFloat() * 0.4f + 0.6f,
                            rotationSpeed = (Random.nextFloat() - 0.5f) * 260f,
                            currentRotation = Random.nextFloat() * 360f,
                            vx = (Random.nextFloat() - 0.5f) * 220f,
                            vy = 720f + (Random.nextFloat() * 320f)
                        )
                    )
                }

                var lastTime = withFrameNanos { it }
                while (isActive && particles.isNotEmpty()) {
                    withFrameNanos { now ->
                        val dt = (now - lastTime) / 1_000_000_000f
                        lastTime = now
                        val t = now / 1_000_000_000f

                        val newParticles = particles.map { p ->
                            val sway = (sin((t * 4.8f + p.id).toDouble()) * 28.0).toFloat()
                            p.apply {
                                x += (vx + sway) * dt
                                y += vy * dt
                                currentRotation += rotationSpeed * dt
                            }
                        }.filter { it.y < heightPx + with(density) { 120.dp.toPx() } }

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
                        withTransform({
                            translate(p.x, p.y)
                            rotate(p.currentRotation)
                            scale(p.scale, p.scale)
                        }) {
                            drawRect(
                                color = p.color,
                                topLeft = Offset(-12f, -8f),
                                size = androidx.compose.ui.geometry.Size(24f, 16f)
                            )
                        }
                    }
                }
            }
        }
    }
}