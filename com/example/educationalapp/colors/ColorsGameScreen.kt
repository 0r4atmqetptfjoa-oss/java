package com.example.educationalapp.colors

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun ColorsGameScreen(
    viewModel: ColorsGameViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var targetPosition by remember { mutableStateOf(Offset.Zero) }

    // Logică Hint (Resetăm timer-ul la fiecare schimbare de rundă SAU greșeală)
    var showHint by remember { mutableStateOf(false) }
    
    // Key-ul include și wrongSelectionId ca să reseteze hintul dacă greșește
    LaunchedEffect(uiState.currentTarget.id, uiState.wrongSelectionId) {
        showHint = false
        delay(5000)
        if (uiState.gameState == GameState.WAITING_INPUT) showHint = true
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. FUNDAL
        Image(
            painter = painterResource(id = ColorsUi.Backgrounds.carnival),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Row(modifier = Modifier.fillMaxSize()) {
            // ZONA STÂNGA (70%)
            Box(modifier = Modifier.weight(0.7f).fillMaxHeight()) {
                // Home Btn
                Box(
                    modifier = Modifier.align(Alignment.TopStart).offset(20.dp, 20.dp).size(80.dp)
                ) {
                    Image(
                        painter = painterResource(id = ColorsUi.Icons.home),
                        contentDescription = "Home",
                        modifier = Modifier.fillMaxSize().clickable { onBack() }
                    )
                }

                // Score
                Box(
                    modifier = Modifier.align(Alignment.TopStart).offset(10.dp, -40.dp).size(300.dp, 200.dp)
                ) {
                    ScoreBadge(score = uiState.score)
                }

                // Ținta
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(70.dp, -20.dp)
                        .size(360.dp)
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInRoot()
                            val size = coords.size
                            targetPosition = Offset(pos.x + size.width / 2f, pos.y + size.height / 2f)
                        }
                ) {
                    TargetObject(uiState = uiState)
                }

                // Mascota
                val isHappy = uiState.gameState == GameState.CELEBRATE || uiState.gameState == GameState.IMPACT
                Box(
                    modifier = Modifier.align(Alignment.BottomStart).offset(-25.dp, 50.dp).size(260.dp)
                        .scale(if (isHappy) 1.25f else 1f)
                ) {
                    Image(
                        painter = painterResource(id = if (isHappy) ColorsUi.Mascot.happy else ColorsUi.Mascot.waiting),
                        contentDescription = "Mascot",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // ZONA DREAPTA (30%) - GRID BALOANE
            Box(
                modifier = Modifier.weight(0.3f).fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.offset((-20).dp, 0.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(15.dp),
                        verticalArrangement = Arrangement.spacedBy(15.dp),
                        contentPadding = PaddingValues(10.dp),
                        modifier = Modifier.wrapContentSize()
                    ) {
                        items(uiState.options) { item ->
                            // Identificăm dacă trebuie să arătăm hint pe acest item
                            val isHintTarget = showHint && item.name == uiState.currentTarget.name
                            val isCorrectTarget = item.name == uiState.currentTarget.name
                            val isWrong = uiState.wrongSelectionId == item.id
                            
                            // Cheie de resetare pentru balon: Când se schimbă ținta (Runda), balonul reapare
                            val resetKey = uiState.currentTarget.id 

                            AnimatedColorButton(
                                item = item,
                                resetKey = resetKey, 
                                isEnabled = uiState.gameState == GameState.WAITING_INPUT,
                                isHintActive = isHintTarget,
                                isCorrectTarget = isCorrectTarget,
                                isWrongAnimation = isWrong,
                                buttonSize = 110.dp,
                                onSelected = { startPos ->
                                    showHint = false
                                    viewModel.onOptionSelected(item, startPos, targetPosition)
                                }
                            )
                        }
                    }
                }
            }
        }

        // VFX
        if (uiState.gameState == GameState.PROJECTILE_FLYING) {
            ComposeProjectile(
                start = uiState.projectileStart,
                end = uiState.projectileEnd,
                color = uiState.projectileColor
            )
        }

        if (uiState.gameState == GameState.IMPACT) {
            ComposeExplosion(center = uiState.projectileEnd, color = uiState.projectileColor)
        }
    }
}

// --- COMPONENTE COMPLETE ---

@Composable
fun ScoreBadge(score: Int) {
    Box(
        contentAlignment = Alignment.Center, 
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = ColorsUi.Icons.scoreBg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.offset(x = -10.dp, y = 2.dp) 
        ) {
            Image(
                painter = painterResource(id = ColorsUi.Icons.star),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$score",
                style = TextStyle(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFE65100),
                    shadow = Shadow(color = Color.White, offset = Offset(2f, 2f), blurRadius = 4f)
                )
            )
        }
    }
}

@Composable
fun AnimatedColorButton(
    item: ColorItem,
    resetKey: String, // Cheia care forțează re-apariția balonului (ID-ul rundei curente)
    isEnabled: Boolean,
    isHintActive: Boolean,
    isCorrectTarget: Boolean,
    isWrongAnimation: Boolean,
    buttonSize: Dp,
    onSelected: (Offset) -> Unit
) {
    var myPos by remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Stare POP
    var isPopped by remember { mutableStateOf(false) }
    val popAnim = remember { Animatable(0f) }

    // BUG FIX: Resetăm starea când se schimbă RUNDA (resetKey), nu item-ul
    LaunchedEffect(resetKey) {
        isPopped = false
        popAnim.snapTo(0f)
    }

    // --- ANIMATII ---
    val infiniteTransition = rememberInfiniteTransition(label = "balloonAnim")
    val randomDelay = remember(item.id) { Random.nextInt(0, 1500) }
    
    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(randomDelay)
        ), label = "float"
    )

    val hintScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "hint"
    )

    // SHAKE ANIMATION (Pt greșeală)
    val shakeAnim = remember { Animatable(0f) }
    LaunchedEffect(isWrongAnimation) {
        if (isWrongAnimation) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Vibrație eroare
            // Secvență shake: stânga-dreapta rapid
            shakeAnim.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    (-10f) at 50
                    10f at 100
                    (-10f) at 150
                    10f at 200
                    0f at 400
                }
            )
        }
    }

    val popScale = if (popAnim.value > 0f) 1f + (popAnim.value * 0.5f) else 1f
    val currentScale = (if (isHintActive) hintScale else 1f) * popScale
    
    // Dacă pocnește, devine transparent
    val alpha = 1f - popAnim.value 

    Box(
        modifier = Modifier
            .size(buttonSize)
            .scale(currentScale)
            .offset(x = shakeAnim.value.dp, y = floatingOffset.dp) // Adăugat Shake pe X
            .alpha(alpha)
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                val size = coords.size
                myPos = Offset(pos.x + size.width / 2f, pos.y + size.height / 2f)
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                enabled = isEnabled && !isPopped
            ) { 
                if (isCorrectTarget) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) // Vibrație succes
                    scope.launch {
                        isPopped = true
                        popAnim.animateTo(1f, tween(200, easing = FastOutLinearInEasing)) // POP rapid
                        onSelected(myPos)
                    }
                } else {
                    onSelected(myPos) // Trimitem click-ul ca să declanșăm Shake-ul din ViewModel
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Explozie vizuală POP
        if (popAnim.value > 0.1f && popAnim.value < 0.9f) {
            Canvas(modifier = Modifier.fillMaxSize().scale(1.4f)) {
                 drawCircle(
                    color = Color.White.copy(alpha = 0.8f * (1f - popAnim.value)),
                    radius = size.minDimension / 1.8f,
                    style = Stroke(width = 8f)
                )
            }
        }

        Image(
            painter = painterResource(id = item.balloonImageRes),
            contentDescription = item.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        
        // Gloss (dispare la pop)
        if (popAnim.value == 0f) { 
            Canvas(modifier = Modifier.fillMaxSize()) {
                 drawCircle(
                    color = Color.White.copy(alpha = 0.25f),
                    radius = size.minDimension / 4f,
                    center = center.copy(x = center.x - 25f, y = center.y - 25f)
                )
            }
        }

        if (isHintActive && popAnim.value == 0f) {
            Canvas(modifier = Modifier.fillMaxSize().scale(1.2f)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Yellow.copy(alpha = 0.6f), Color.Transparent),
                        center = center,
                        radius = size.minDimension / 1.5f
                    )
                )
            }
        }
    }
}

@Composable
fun TargetObject(uiState: ColorsUiState) {
    val impactScale by animateFloatAsState(
        targetValue = if (uiState.gameState == GameState.IMPACT || uiState.gameState == GameState.CELEBRATE) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow), 
        label = "targetBounce"
    )

    Box(modifier = Modifier.fillMaxSize().scale(impactScale)) {
        val imageRes = if (uiState.gameState == GameState.IMPACT || uiState.gameState == GameState.CELEBRATE)
            uiState.currentTarget.happyImageRes else uiState.currentTarget.sadImageRes

        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "Target",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

// --- VFX ---

@Composable
fun ComposeProjectile(start: Offset, end: Offset, color: Color) {
    val anim = remember { Animatable(0f) }
    
    // FIX TIMING: Folosim constanta (asigură-te că valoarea e aceeași ca în ViewModel, ex: 600)
    LaunchedEffect(Unit) {
        anim.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = Modifier.fillMaxSize().zIndex(99f)) {
        val t = anim.value
        val controlPoint = Offset((start.x + end.x) / 2, minOf(start.y, end.y) - 500f)
        val x = (1 - t).pow(2) * start.x + 2 * (1 - t) * t * controlPoint.x + t.pow(2) * end.x
        val y = (1 - t).pow(2) * start.y + 2 * (1 - t) * t * controlPoint.y + t.pow(2) * end.y
        val currentPos = Offset(x, y)

        drawCircle(
            brush = Brush.radialGradient(colors = listOf(Color.White, color), center = currentPos, radius = 35f),
            center = currentPos,
            radius = 35f
        )
        
        // Coada proiectilului
        for (i in 1..8) {
             val tailT = (t - i * 0.02f).coerceAtLeast(0f)
             if (tailT > 0) {
                 // Recalculăm poziția pentru coadă
                 val tx = (1 - tailT).pow(2) * start.x + 2 * (1 - tailT) * tailT * controlPoint.x + tailT.pow(2) * end.x
                 val ty = (1 - tailT).pow(2) * start.y + 2 * (1 - tailT) * tailT * controlPoint.y + tailT.pow(2) * end.y
                 
                 drawCircle(
                     color = color.copy(alpha = 0.5f * (1f - i / 8f)),
                     center = Offset(tx, ty),
                     radius = 25f * (1f - i / 8f)
                 )
             }
        }
    }
}

@Composable
fun ComposeExplosion(center: Offset, color: Color) {
    val anim = remember { Animatable(0f) }
    
    // FIX FLICKER: Calculăm culorile O SINGURĂ DATĂ în remember
    data class Particle(val angle: Float, val speed: Float, val size: Float, val pColor: Color)
    
    val particles = remember {
        List(25) { 
            Particle(
                angle = Random.nextFloat() * 360f,
                speed = Random.nextFloat() * 0.6f + 0.4f,
                size = Random.nextFloat() * 30f + 10f,
                pColor = if (Random.nextBoolean()) color else Color.White // Culoarea e decisă la creare
            )
        }
    }

    LaunchedEffect(Unit) {
        anim.animateTo(1f, animationSpec = tween(800, easing = LinearOutSlowInEasing))
    }

    Canvas(modifier = Modifier.fillMaxSize().zIndex(100f)) {
        val t = anim.value
        val maxRadius = 500f

        // Shockwave
        drawCircle(
            color = color.copy(alpha = (1f - t).coerceIn(0f, 1f)),
            radius = t * maxRadius * 0.6f,
            center = center,
            style = Stroke(width = 30f * (1 - t))
        )

        // Particule stabile
        particles.forEach { p ->
            val rad = Math.toRadians(p.angle.toDouble())
            val distance = t * maxRadius * p.speed
            val px = center.x + (distance * cos(rad)).toFloat()
            val py = center.y + (distance * sin(rad)).toFloat() + (t * t * 150f)

            drawCircle(
                color = p.pColor, // Folosim culoarea pre-calculată
                alpha = (1f - t).coerceIn(0f, 1f),
                radius = p.size * (1f - t),
                center = Offset(px, py)
            )
        }
    }
}