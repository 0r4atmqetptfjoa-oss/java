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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import kotlin.jvm.JvmSynthetic

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
                    },
                    onHome = { soundPlayer.playClick(); onBackToMenu() }
                )

                
                Spacer(modifier = Modifier.height(8.dp))

                // Layout adaptiv: evităm suprapuneri și folosim mai bine spațiul (mai ales în landscape).
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val isLandscape = maxWidth > maxHeight
                    val gap = if (isLandscape) 10.dp else 12.dp

                    // Card mai mare + imagine mai mare (cerință user)
                    val cardWidth = (maxWidth * if (isLandscape) 0.60f else 0.72f)
                        .coerceIn(300.dp, if (isLandscape) 760.dp else 420.dp)
                    val cardHeight = (maxHeight * if (isLandscape) 0.72f else 0.48f)
                        .coerceIn(200.dp, if (isLandscape) 440.dp else 280.dp)

                    val mascotSize = if (isLandscape) 150.dp else 135.dp
                    val optionSize = if (isLandscape) 100.dp else 94.dp

                    if (isLandscape) {
                        val isCorrect = uiState.isAnswerCorrect == true
                        val isWrong = uiState.isAnswerCorrect == false

                        val borderColor by animateColorAsState(
                            targetValue = when {
                                isCorrect -> Color(0xFF43A047)
                                isWrong -> Color(0xFFE53935)
                                else -> Color(0xFFFF9800)
                            },
                            label = "cardBorder"
                        )

                        val cardScale by animateFloatAsState(
                            targetValue = when {
                                isCorrect -> 1.03f
                                isWrong -> 0.99f
                                else -> 1.0f
                            },
                            label = "cardScale"
                        )

                        val word = remember(uiState.currentQuestion.word) {
                            val w = uiState.currentQuestion.word
                            if (w.isNotEmpty()) "_" + w.drop(1) else "_"
                        }

                        val mascotRes = when (uiState.mascotMood) {
                            MascotMood.HAPPY, MascotMood.CELEBRATE -> AlphabetUi.Mascot.happy
                            MascotMood.SURPRISED -> AlphabetUi.Mascot.surprised
                            MascotMood.THINKING -> AlphabetUi.Mascot.thinking
                            else -> AlphabetUi.Mascot.normal
                        }

                                                // =============================================================
                        // LANDSCAPE: layout fix (totul vizibil pe ecran)
                        // - STÂNGA: card mare cu animal/obiect (prioritar)
                        // - DREAPTA SUS: cuvântul cu literă lipsă
                        // - DREAPTA JOS: litere mari + mascotă lângă litere, lipită de "pământ"
                        // =============================================================
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // --- CARD (principal) ---
                            Box(
                                modifier = Modifier
                                    .weight(0.68f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                // lăsăm card-ul să fie mare, dar controlat (fără să iasă din ecran)
                                val w = minOf(cardWidth, maxWidth * 0.70f)
                                val h = minOf(cardHeight, maxHeight * 0.85f)

                                Box(
                                    modifier = Modifier
                                        .width(w)
                                        .height(h)
                                        .scale(cardScale)
                                        .graphicsLayer { translationX = shakeX.value }
                                        .shadow(12.dp, RoundedCornerShape(22.dp))
                                        .background(Color.White, RoundedCornerShape(22.dp))
                                        .border(BorderStroke(4.dp, borderColor), RoundedCornerShape(22.dp))
                                        .padding(7.dp)
                                ) {
                                    val centerBmp = rememberScaledImageBitmap(uiState.currentQuestion.imageRes, maxDim = 1100)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color(0xFFE3F2FD),
                                                        Color(0xFFF1F8E9)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (centerBmp != null) {
                                            Image(
                                                bitmap = centerBmp,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(0.97f),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Image(
                                                painter = painterResource(id = uiState.currentQuestion.imageRes),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(0.97f),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    }
                                }
                            }

                            // --- DREAPTA (cuvânt sus, litere + mascotă jos) ---
                            BoxWithConstraints(
                                modifier = Modifier
                                    .weight(0.32f)
                                    .fillMaxHeight()
                            ) {
                                val rightW = maxWidth
                                val rightH = maxHeight

                                // Mascota (lipită de jos). Păstrăm un size sănătos și rezervăm lățime pentru ea.
                                val mascotSize = (rightH * 0.44f).coerceIn(120.dp, 230.dp)
                                val reservedForMascot = (mascotSize * 0.82f).coerceIn(110.dp, 200.dp)

                                val optionsCount = uiState.options.size.coerceAtLeast(1)
                                val spacing = 16.dp

                                // Dimensiune butoane din lățimea rămasă (după ce rezervăm spațiu pentru mascotă).
                                val buttonsAreaWidth = (rightW - reservedForMascot - 10.dp).coerceAtLeast(160.dp)
                                val btnByWidth =
                                    ((buttonsAreaWidth - spacing * (optionsCount - 1)) / optionsCount.toFloat())
                                        .coerceAtLeast(70.dp)
                                val btnByHeight = (rightH * 0.28f).coerceIn(78.dp, 150.dp)
                                val optionSize = minOf(btnByWidth, btnByHeight).coerceIn(78.dp, 150.dp)

                                Box(modifier = Modifier.fillMaxSize()) {

                                    // (verde) cuvântul sus
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = Color(0xFFFFF3E0).copy(alpha = 0.95f),
                                        shadowElevation = 6.dp,
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = 8.dp)
                                    ) {
                                        Text(
                                            text = word,
                                            fontSize = 44.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFE65100),
                                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                                            letterSpacing = 1.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (uiState.isFinished) {
                                        // La final: panel în partea de jos-stânga a zonei drepte; mascota jos-dreapta.
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(end = reservedForMascot),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            FinishPanel(
                                                score = uiState.score,
                                                stars = uiState.stars,
                                                total = uiState.totalQuestions,
                                                onRestart = { soundPlayer.playClick(); viewModel.resetGame() },
                                                onBackToMenu = { soundPlayer.playClick(); onBackToMenu() }
                                            )
                                        }

                                        Image(
                                            painter = painterResource(id = mascotRes),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(end = 4.dp, bottom = 0.dp)
                                                .size(mascotSize),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        // (maro) litere jos + (negru) mascotă jos, lângă litere, lipită de jos
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .padding(bottom = 0.dp),
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(start = 2.dp, end = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(spacing),
                                                verticalAlignment = Alignment.Bottom
                                            ) {
                                                uiState.options.forEach { option ->
                                                    val isSelectedCorrect =
                                                        uiState.isAnswerCorrect == true &&
                                                            AlphabetAssets.normalizeBase(option) ==
                                                            AlphabetAssets.normalizeBase(uiState.currentQuestion.displayLetter)
                                                    val isSelectedWrong =
                                                        uiState.isAnswerCorrect == false && uiState.selectedOption == option

                                                    val containerColor = when {
                                                        isSelectedCorrect -> Color(0xFF4CAF50)
                                                        isSelectedWrong -> Color(0xFFEF5350)
                                                        else -> Color(0xFFFF9800)
                                                    }

                                                    SquishyButton(
                                                        onClick = {
                                                            if (!uiState.isInputLocked) {
                                                                soundPlayer.playClick()
                                                                viewModel.selectAnswer(option)
                                                            }
                                                        },
                                                        size = optionSize,
                                                        shape = CircleShape,
                                                        color = containerColor,
                                                        elevation = 10.dp
                                                    ) {
                                                        Text(
                                                            text = option,
                                                            fontSize = 48.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = Color.White
                                                        )
                                                    }
                                                }
                                            }

                                            // Mascota lipită de jos, lângă litere
                                            Image(
                                                painter = painterResource(id = mascotRes),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .padding(end = 4.dp, bottom = 0.dp)
                                                    .size(mascotSize),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {

                        Column(modifier = Modifier.fillMaxSize()) {

                                                // =============================================================
                                                // MIJLOC (CARTONAȘ + NUME)
                                                // =============================================================
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .weight(1f)
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

                                                        val borderColor by animateColorAsState(
                                                            targetValue = when {
                                                                isCorrect -> Color(0xFF43A047)
                                                                isWrong -> Color(0xFFE53935)
                                                                else -> Color(0xFFFFB74D)
                                                            },
                                                            label = "cardBorder"
                                                        )

                                                        val cardScale by animateFloatAsState(
                                                            targetValue = if (isCorrect) 1.03f else 1f,
                                                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                                            label = "cardScale"
                                                        )

                                                        Box(
                                                            modifier = Modifier
                                                                .width(cardWidth)
                                                                .height(cardHeight)
                                                                .scale(cardScale)
                                                                .graphicsLayer { translationX = shakeX.value }
                                                                .shadow(12.dp, RoundedCornerShape(22.dp))
                                                                .background(Color.White, RoundedCornerShape(22.dp))
                                                                .border(BorderStroke(4.dp, borderColor), RoundedCornerShape(22.dp))
                                                                .padding(7.dp)
                                                        ) {
                                                            val centerBmp = rememberScaledImageBitmap(uiState.currentQuestion.imageRes, maxDim = 900)
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .clip(RoundedCornerShape(16.dp))
                                                                    .background(
                                                                        Brush.verticalGradient(
                                                                            listOf(
                                                                                Color(0xFFE3F2FD),
                                                                                Color(0xFFF1F8E9)
                                                                            )
                                                                        )
                                                                    ),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                if (centerBmp != null) {
                                                                    Image(
                                                                        bitmap = centerBmp,
                                                                        contentDescription = null,
                                                                        modifier = Modifier.fillMaxSize(0.92f),
                                                                        contentScale = ContentScale.Fit
                                                                    )
                                                                } else {
                                                                    Image(
                                                                        painter = painterResource(id = uiState.currentQuestion.imageRes),
                                                                        contentDescription = null,
                                                                        modifier = Modifier.fillMaxSize(0.92f),
                                                                        contentScale = ContentScale.Fit
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.width(if (isLandscape) 10.dp else 14.dp))

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
                                                                color = Color(0xFFFFF3E0),
                                                                shape = RoundedCornerShape(16.dp),
                                                                shadowElevation = 6.dp,
                                                                modifier = Modifier.padding(end = 8.dp)
                                                            ) {
                                                                Text(
                                                                    text = word,
                                                                    fontSize = if (isLandscape) 32.sp else 36.sp,
                                                                    fontWeight = FontWeight.Black,
                                                                    color = Color(0xFFE65100),
                                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                                    letterSpacing = 1.sp
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(gap))

                                                // =============================================================
                                                // JOS (MASCOTA & BUTOANE / REZULTAT) - fără suprapuneri
                                                // =============================================================
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(
                                                            start = 12.dp,
                                                            end = 12.dp,
                                                            bottom = if (isLandscape) 10.dp else 14.dp
                                                        ),
                                                    verticalAlignment = Alignment.Bottom,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                                                        modifier = Modifier.size(mascotSize),
                                                        contentScale = ContentScale.Fit,
                                                        alignment = Alignment.BottomStart
                                                    )

                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f),
                                                        contentAlignment = Alignment.BottomCenter
                                                    ) {
                                                        if (uiState.isFinished) {
                                                            FinishPanel(
                                                                score = uiState.score,
                                                                stars = uiState.stars,
                                                                total = uiState.totalQuestions,
                                                                onRestart = {
                                                                    soundPlayer.playClick()
                                                                    viewModel.resetGame()
                                                                },
                                                                onBackToMenu = {
                                                                    soundPlayer.playClick()
                                                                    onBackToMenu()
                                                                }
                                                            )
                                                        } else {
                                                            Row(
                                                                horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 10.dp else 16.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                uiState.options.forEach { option ->
                                                                    val isSelectedCorrect =
                                                                        uiState.isAnswerCorrect == true &&
                                                                            AlphabetAssets.normalizeBase(option) ==
                                                                            AlphabetAssets.normalizeBase(uiState.currentQuestion.displayLetter)
                                                                    val isSelectedWrong =
                                                                        uiState.isAnswerCorrect == false && uiState.selectedOption == option

                                                                    val containerColor = when {
                                                                        isSelectedCorrect -> Color(0xFF4CAF50)
                                                                        isSelectedWrong -> Color(0xFFEF5350)
                                                                        else -> Color(0xFFFF9800)
                                                                    }

                                                                    SquishyButton(
                                                                        onClick = {
                                                                            if (!uiState.isInputLocked) {
                                                                                soundPlayer.playClick()
                                                                                viewModel.selectAnswer(option)
                                                                            }
                                                                        },
                                                                        size = optionSize,
                                                                        shape = CircleShape,
                                                                        color = containerColor,
                                                                        elevation = 10.dp
                                                                    ) {
                                                                        Text(
                                                                            text = option,
                                                                            fontSize = if (isLandscape) 40.sp else 44.sp,
                                                                            fontWeight = FontWeight.Black,
                                                                            color = Color.White
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
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
    onToggleSound: () -> Unit,
    onHome: () -> Unit
) {
    val progress = if (totalQuestions > 0) (questionIndex + 1) / totalQuestions.toFloat() else 0f
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val questionText = "Întrebarea ${questionIndex + 1} / $totalQuestions"
    val percentText = "${(progress * 100f).toInt()}%"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- SCOR + STELE (stânga) ---
        Surface(
            shape = RoundedCornerShape(999.dp),
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
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = score.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF263238)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "★".repeat(stars.coerceAtMost(10)),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF9800)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // --- PROGRES + ÎNTREBAREA (mijloc) ---
        Box(
            modifier = Modifier
                .weight(1f)
                .height(if (isLandscape) 42.dp else 46.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.90f))
        ) {
            LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(999.dp)),
                color = Color(0xFFFF9800),
                trackColor = Color(0xFFECEFF1)
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = questionText,
                    fontSize = if (isLandscape) 14.sp else 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF37474F),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = percentText,
                    fontSize = if (isLandscape) 13.sp else 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF37474F)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // --- ȘANSE (dreapta) ---
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.White.copy(alpha = 0.92f),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
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

        Spacer(modifier = Modifier.width(8.dp))

        // --- TOGGLE SUNET ---
        SquishyButton(onClick = onToggleSound, size = 42.dp, shape = CircleShape) {
            Image(
                painter = painterResource(id = AlphabetUi.Icons.soundOn),
                contentDescription = "Sound",
                modifier = Modifier
                    .size(24.dp)
                    .alpha(if (soundOn) 1f else 0.45f)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // --- HOME ---
        SquishyButton(onClick = onHome, size = 42.dp, shape = CircleShape) {
            Image(
                painter = painterResource(id = AlphabetUi.Icons.home),
                contentDescription = "Home",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun FinishPanel(
    score: Int,
    stars: Int,
    total: Int,
    onRestart: () -> Unit,
    onBackToMenu: () -> Unit
) {
    // Local responsive flag for this composable (it is not in scope from the parent).
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

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
                                            fontSize = if (isLandscape) 32.sp else 36.sp,
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
                onClick = onRestart,
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
            Spacer(modifier = Modifier.height(10.dp))

            SquishyButton(
                onClick = onBackToMenu,
                modifier = Modifier
                    .height(52.dp)
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFFF39C12), RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                elevation = 0.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(id = AlphabetUi.Icons.home),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "MENIU",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF39C12)
                    )
                }
            }

        }
    }
}

// --- CLASE AUXILIARE ---

@JvmSynthetic
@Composable
private fun SquishyButton(
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

@JvmSynthetic
@Composable
private fun ConfettiBox(burstId: Long, content: @Composable () -> Unit) {
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