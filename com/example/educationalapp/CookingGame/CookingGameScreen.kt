package com.example.educationalapp.features.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.educationalapp.R
// IMPORTANT: Asigură-te că aceste fișiere există în pachetul 'alphabet'
import com.example.educationalapp.alphabet.AlphabetSoundPlayer
import com.example.educationalapp.alphabet.ConfettiBox
import com.example.educationalapp.alphabet.SquishyButton
import kotlin.math.hypot
import kotlin.math.roundToInt

// ===== Fallback resource mapping =====
private const val BG_PIZZERIA_KITCHEN: Int = R.drawable.bg_game_colors
private const val PIZZA_1_DOUGH: Int = R.drawable.shape_circle_donut
private const val PIZZA_2_FLAT: Int = R.drawable.shape_circle_clock
private const val PIZZA_3_SAUCED: Int = R.drawable.shape_circle_button
private const val PIZZA_4_BAKED: Int = R.drawable.shape_triangle_pizza
private const val PROP_OVEN_OPEN: Int = R.drawable.ui_score_container

// FIX: Folosim ui_btn_home care este consistent cu restul aplicației
private const val TOOL_ROLLING_PIN: Int = R.drawable.ui_btn_home 
private const val TOOL_SAUCE_LADLE: Int = R.drawable.ui_btn_home

@Composable
fun CookingGameScreen(
    viewModel: CookingGameViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val soundPlayer = remember { AlphabetSoundPlayer(context) }

    // Confetti final
    var confettiBurstId by remember { mutableStateOf(0L) }

    // Stage change SFX (lightweight)
    var lastStage by remember { mutableStateOf(uiState.stage) }
    LaunchedEffect(uiState.stage) {
        if (uiState.stage != lastStage) {
            when (uiState.stage) {
                CookingStage.SAUCE,
                CookingStage.TOPPING,
                CookingStage.EATING -> soundPlayer.playClick()
                else -> {}
            }
            lastStage = uiState.stage
        }
    }

    LaunchedEffect(uiState.isPizzaFinished) {
        if (uiState.isPizzaFinished) {
            soundPlayer.playCorrect()
            confettiBurstId = System.currentTimeMillis()
        }
    }

    // Coordonate zonă pizza (în px)
    var pizzaCenter by remember { mutableStateOf(Offset.Zero) }
    var pizzaRadius by remember { mutableStateOf(0f) }

    val density = LocalDensity.current
    val toppingSizePx = with(density) { 46.dp.toPx() }

    ConfettiBox(burstId = confettiBurstId) {
        Box(modifier = Modifier.fillMaxSize()) {

            // 1) BACKGROUND
            Image(
                painter = painterResource(id = BG_PIZZERIA_KITCHEN),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Back button
            Image(
                // FIX: Aici era "ui_button_home"
                painter = painterResource(id = R.drawable.ui_btn_home),
                contentDescription = "Back",
                modifier = Modifier
                    .padding(16.dp)
                    .size(64.dp)
                    .align(Alignment.TopStart)
                    .clickable { onBack() }
            )

            // Stage HUD
            StageStepper(
                stage = uiState.stage,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )

            // 2) MAIN AREA (Pizza)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.stage == CookingStage.BAKING) {
                    BakingPanel(progress = uiState.bakeProgress)
                } else {
                    val baseRes = when (uiState.stage) {
                        CookingStage.ROLLING -> PIZZA_1_DOUGH
                        CookingStage.SAUCE -> PIZZA_2_FLAT
                        CookingStage.TOPPING, CookingStage.BAKING -> PIZZA_3_SAUCED
                        CookingStage.EATING -> PIZZA_4_BAKED
                    }

                    val scale by animateFloatAsState(
                        targetValue = if (uiState.stage == CookingStage.EATING) 1.06f else 1f,
                        animationSpec = spring()
                    )

                    // Pizza container
                    Box(
                        modifier = Modifier
                            .size(340.dp)
                            .scale(scale)
                            .onGloballyPositioned {
                                val pos = it.positionInRoot()
                                val size = it.size
                                pizzaCenter = Offset(pos.x + size.width / 2f, pos.y + size.height / 2f)
                                pizzaRadius = size.width / 2f
                            }
                            .pointerInput(uiState.stage, pizzaRadius) {
                                if (uiState.stage == CookingStage.EATING && pizzaRadius > 0f) {
                                    detectTapGestures {
                                        soundPlayer.playClick()
                                        viewModel.takeBite(pizzaRadiusPx = pizzaRadius)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!uiState.isPizzaFinished) {
                            // Base
                            Image(
                                painter = painterResource(id = baseRes),
                                contentDescription = "Pizza",
                                modifier = Modifier.fillMaxSize()
                            )

                            // Toppings
                            if (uiState.stage == CookingStage.TOPPING || uiState.stage == CookingStage.EATING) {
                                uiState.placedToppings.forEach { topping ->
                                    val x = (pizzaRadius + topping.positionFromCenter.x - toppingSizePx / 2f).roundToInt()
                                    val y = (pizzaRadius + topping.positionFromCenter.y - toppingSizePx / 2f).roundToInt()

                                    Image(
                                        painter = painterResource(id = topping.imageRes),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(46.dp)
                                            .offset { IntOffset(x, y) }
                                            .rotate(topping.rotation)
                                            .scale(topping.scale)
                                    )
                                }

                                // Drop sparkle FX
                                DropSparkleFx(
                                    fxId = uiState.lastDropFxId,
                                    pizzaRadiusPx = pizzaRadius,
                                    posFromCenter = uiState.lastDropFxPosFromCenter
                                )
                            }

                            // Bite marks
                            if (uiState.stage == CookingStage.EATING && uiState.biteMarks.isNotEmpty()) {
                                BiteMarksOverlay(biteMarks = uiState.biteMarks)
                            }
                        } else {
                            Text(
                                "Delicios!",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }

            // 3) BOTTOM PANEL
            BottomPanel(
                uiState = uiState,
                viewModel = viewModel,
                pizzaCenter = pizzaCenter,
                pizzaRadius = pizzaRadius,
                soundPlayer = soundPlayer
            )
        }
    }
}

// ... Restul funcțiilor (StageStepper, BakingPanel, etc.) rămân la fel ca în fișierul tău anterior ...
// Asigură-te că păstrezi tot codul de sub "fun CookingGameScreen" din versiunea anterioară!
// (Le-am omis aici pentru a economisi spațiu, dar ele sunt necesare).
@Composable
private fun StageStepper(stage: CookingStage, modifier: Modifier = Modifier) {
    val items = listOf(
        CookingStage.ROLLING to "Aluat",
        CookingStage.SAUCE to "Sos",
        CookingStage.TOPPING to "Ingrediente",
        CookingStage.BAKING to "Cuptor",
        CookingStage.EATING to "Mănâncă"
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x66000000))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (s, label) ->
            val active = s == stage
            Text(
                text = label,
                color = if (active) Color.White else Color(0xAAFFFFFF),
                fontSize = 14.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun BakingPanel(progress: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = PROP_OVEN_OPEN),
            contentDescription = "Oven",
            modifier = Modifier.size(360.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Se coace...", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .width(260.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp)),
            color = Color(0xFFFF9800),
            trackColor = Color.White
        )
    }
}

@Composable
private fun BottomPanel(
    uiState: CookingUiState,
    viewModel: CookingGameViewModel,
    pizzaCenter: Offset,
    pizzaRadius: Float,
    soundPlayer: AlphabetSoundPlayer
) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(170.dp),
        contentAlignment = Alignment.Center
    ) {
        // Panel background
        Image(
            painter = painterResource(id = R.drawable.ui_score_container),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.92f),
            contentScale = ContentScale.FillBounds
        )

        when (uiState.stage) {
            CookingStage.ROLLING -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Trage făcălețul peste pizza!", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    DraggableToolItem(
                        imageRes = TOOL_ROLLING_PIN,
                        targetCenter = pizzaCenter,
                        targetRadius = pizzaRadius,
                        progress = uiState.rollProgress,
                        onDragProgress = { delta ->
                            viewModel.addRollingProgress(delta)
                        }
                    )
                }
            }
            CookingStage.SAUCE -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Întinde sosul cu polonicul!", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    DraggableToolItem(
                        imageRes = TOOL_SAUCE_LADLE,
                        targetCenter = pizzaCenter,
                        targetRadius = pizzaRadius,
                        progress = uiState.sauceProgress,
                        onDragProgress = { delta ->
                            viewModel.addSauceProgress(delta)
                        }
                    )
                }
            }
            CookingStage.TOPPING -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .width(140.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Rețetă:", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        uiState.recipe.forEach { req ->
                            val placed = uiState.placedToppings.count { it.imageRes == req.imageRes }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = req.imageRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${placed}/${req.requiredCount}",
                                    color = if (placed >= req.requiredCount) Color(0xFFB9FFB9) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        val bakeEnabled = uiState.isRecipeComplete
                        SquishyButton(
                            onClick = {
                                if (bakeEnabled) {
                                    soundPlayer.playClick()
                                    viewModel.startBaking()
                                }
                            },
                            modifier = Modifier.padding(top = 6.dp),
                            size = 78.dp,
                            color = if (bakeEnabled) Color(0xFFFF5722) else Color(0xFF777777)
                        ) {
                            Text("COACE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        }
                        if (!bakeEnabled) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Completează rețeta", color = Color(0xCCFFFFFF), fontSize = 12.sp)
                        }
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        items(viewModel.availableIngredients) { ingredient ->
                            DraggableIngredientItem(
                                ingredient = ingredient,
                                pizzaCenter = pizzaCenter,
                                pizzaRadius = pizzaRadius,
                                onDrop = { posFromCenter ->
                                    soundPlayer.playClick()
                                    viewModel.addTopping(ingredient.imageRes, posFromCenter)
                                }
                            )
                        }
                    }
                }
            }
            CookingStage.EATING -> {
                if (uiState.isPizzaFinished) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SquishyButton(
                            onClick = {
                                soundPlayer.playClick()
                                viewModel.resetGame()
                            },
                            size = 84.dp,
                            color = Color(0xFF4CAF50)
                        ) {
                            Text("Alta!", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Text("Pizza nouă", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("Apasă pe pizza să muști!", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            else -> {}
        }
    }
}

@Composable
fun DraggableToolItem(
    imageRes: Int,
    targetCenter: Offset,
    targetRadius: Float,
    progress: Float,
    onDragProgress: (Float) -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var initialPosition by remember { mutableStateOf(Offset.Zero) }

    val animatedOffsetX by animateFloatAsState(if (isDragging) offsetX else 0f)
    val animatedOffsetY by animateFloatAsState(if (isDragging) offsetY else 0f)
    val scale by animateFloatAsState(if (isDragging) 1.18f else 1f)
    val rotation by animateFloatAsState(if (isDragging) -18f else 0f)

    Box(
        modifier = Modifier
            .offset { IntOffset(animatedOffsetX.roundToInt(), animatedOffsetY.roundToInt()) }
            .size(110.dp)
            .scale(scale)
            .rotate(rotation)
            .zIndex(if (isDragging) 10f else 1f)
            .onGloballyPositioned {
                if (initialPosition == Offset.Zero) {
                    val pos = it.positionInRoot()
                    val size = it.size
                    initialPosition = Offset(pos.x + size.width / 2f, pos.y + size.height / 2f)
                }
            }
            .pointerInput(targetCenter, targetRadius) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        offsetX = 0f
                        offsetY = 0f
                    },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y

                        if (targetRadius > 0f && initialPosition != Offset.Zero) {
                            val currentPos = initialPosition + Offset(offsetX, offsetY)
                            val dist = (currentPos - targetCenter).getDistance()
                            if (dist < targetRadius) {
                                val len = hypot(dragAmount.x, dragAmount.y)
                                // Tune: progress gained per drag distance while "on pizza"
                                val delta = (len / (targetRadius * 14f)).coerceIn(0f, 0.08f)
                                onDragProgress(delta)
                            }
                        }
                    }
                )
            }
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )

        // Progress ring
        if (progress in 0f..0.99f) {
            ToolProgressRing(
                progress = progress,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(30.dp)
            )
        }
    }
}

@Composable
fun DraggableIngredientItem(
    ingredient: IngredientOption,
    pizzaCenter: Offset,
    pizzaRadius: Float,
    onDrop: (Offset) -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var initialPosition by remember { mutableStateOf(Offset.Zero) }

    val animatedOffsetX by animateFloatAsState(if (isDragging) offsetX else 0f)
    val animatedOffsetY by animateFloatAsState(if (isDragging) offsetY else 0f)
    val scale by animateFloatAsState(if (isDragging) 1.25f else 1f)

    Box(
        modifier = Modifier
            .offset { IntOffset(animatedOffsetX.roundToInt(), animatedOffsetY.roundToInt()) }
            .size(78.dp)
            .scale(scale)
            .zIndex(if (isDragging) 10f else 1f)
            .onGloballyPositioned {
                if (initialPosition == Offset.Zero) {
                    val pos = it.positionInRoot()
                    val size = it.size
                    initialPosition = Offset(pos.x + size.width / 2f, pos.y + size.height / 2f)
                }
            }
            .pointerInput(pizzaCenter, pizzaRadius) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        if (pizzaRadius > 0f && initialPosition != Offset.Zero) {
                            val currentPos = initialPosition + Offset(offsetX, offsetY)
                            val dist = (currentPos - pizzaCenter).getDistance()
                            if (dist < pizzaRadius) {
                                val rel = currentPos - pizzaCenter
                                onDrop(rel)
                            }
                        }
                        offsetX = 0f
                        offsetY = 0f
                    },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                )
            }
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            Image(
                painter = painterResource(id = ingredient.imageRes),
                contentDescription = null,
                modifier = Modifier.padding(10.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}


@Composable
private fun ToolProgressRing(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shadow(4.dp, CircleShape)
            .background(Color.White, CircleShape)
            .padding(4.dp)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = size.minDimension * 0.16f
            val sweep = 360f * progress.coerceIn(0f, 1f)
            drawArc(
                color = Color(0x33000000),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokePx)
            )
            drawArc(
                color = Color(0xFFFF9800),
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokePx)
            )
        }
    }
}

@Composable
private fun DropSparkleFx(
    fxId: Long,
    pizzaRadiusPx: Float,
    posFromCenter: Offset
) {
    if (fxId == 0L || pizzaRadiusPx <= 0f) return

    var visible by remember(fxId) { mutableStateOf(true) }
    val alpha by animateFloatAsState(if (visible) 1f else 0f)
    val scale by animateFloatAsState(if (visible) 1.1f else 1.7f)

    LaunchedEffect(fxId) {
        visible = true
        kotlinx.coroutines.delay(420)
        visible = false
    }

    AnimatedVisibility(
        visible = alpha > 0.02f,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (pizzaRadiusPx + posFromCenter.x).roundToInt(),
                        (pizzaRadiusPx + posFromCenter.y).roundToInt()
                    )
                }
                .size(1.dp) // anchor
        ) {
            Sparkle(scale = scale, alpha = alpha)
        }
    }
}

@Composable
private fun Sparkle(scale: Float, alpha: Float) {
    // Simple "sparkle": 4 dots
    val density = LocalDensity.current
    val halfPx = with(density) { 40.dp.toPx().roundToInt() }

    Box(
        modifier = Modifier
            .size(80.dp)
            .offset { IntOffset(-halfPx, -halfPx) }
            .scale(scale)
            .alpha(alpha)
    ) {
        SparkDot(Alignment.TopCenter)
        SparkDot(Alignment.BottomCenter)
        SparkDot(Alignment.CenterStart)
        SparkDot(Alignment.CenterEnd)
    }
}
@Composable
private fun SparkDot(alignment: Alignment) {
    Box(
        modifier = Modifier
            .align(alignment)
            .size(10.dp)
            .background(Color(0xFFFFF176), CircleShape)
            .shadow(4.dp, CircleShape)
    )
}

@Composable
private fun BiteMarksOverlay(biteMarks: List<BiteMark>) {
    // Draw bite marks as light circles (visual hint). Safe across Compose versions.
    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val c = center
            biteMarks.forEach { mark ->
                drawCircle(
                    color = Color(0xFFF7F2E8).copy(alpha = 0.92f),
                    radius = mark.radiusPx,
                    center = c + mark.offsetFromCenter
                )
                drawCircle(
                    color = Color(0x22000000),
                    radius = mark.radiusPx * 1.03f,
                    center = c + mark.offsetFromCenter,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = mark.radiusPx * 0.08f)
                )
            }
        }
    }
}