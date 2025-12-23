package com.example.educationalapp.features.games

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke // <--- IMPORT ADĂUGAT
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow // <--- IMPORT PENTRU TEXT SHADOW
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.educationalapp.R
import com.example.educationalapp.alphabet.AlphabetSoundPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// ==============================================================================
// ⬇️ RESURSELE TALE AICI (Înlocuiește cu numele reale din proiectul tău!)
// ==============================================================================

// Fundal
private const val BG_KITCHEN_RES: Int = R.drawable.bg_pizzeria_kitchen // Pune fundalul tău aici

// Etape Pizza (Imagini pentru baza pizzei)
private const val PIZZA_DOUGH_RES: Int = R.drawable.pizza_1_dough_ball   // Aluat crud
private const val PIZZA_FLAT_RES: Int = R.drawable.pizza_2_flat    // Aluat întins
private const val PIZZA_SAUCE_RES: Int = R.drawable.pizza_3_sauced   // Cu sos
private const val PIZZA_BAKED_RES: Int = R.drawable.pizza_4_baked   // Coaptă

// Unelte
private const val TOOL_ROLLER_RES: Int = R.drawable.tool_rolling_pin   // Făcăleț
private const val TOOL_LADLE_RES: Int = R.drawable.tool_sauce_ladle    // Polonic
private const val OVEN_RES: Int = R.drawable.prop_oven_open          // Cuptor (dacă ai)

// ==============================================================================

@Composable
fun CookingGameScreen(
    viewModel: CookingGameViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val soundPlayer = remember { AlphabetSoundPlayer(context) }

    // Sistem Particule
    var confettiBurstId by remember { mutableLongStateOf(0L) }
    
    // Sunete la schimbarea etapei
    LaunchedEffect(uiState.stage) {
        if (uiState.stage != CookingStage.ROLLING) {
            soundPlayer.playClick()
        }
    }

    LaunchedEffect(uiState.isPizzaFinished) {
        if (uiState.isPizzaFinished) {
            soundPlayer.playCorrect()
            confettiBurstId = System.currentTimeMillis()
        }
    }

    // Layout Pizza
    var pizzaCenter by remember { mutableStateOf(Offset.Zero) }
    var pizzaRadius by remember { mutableStateOf(0f) }

    ConfettiBox(burstId = confettiBurstId) {
        Box(modifier = Modifier.fillMaxSize()) {

            // 1. FUNDAL + VIGNETTE (Atmosferă)
            Image(
                painter = painterResource(id = BG_KITCHEN_RES),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                            radius = 1200f
                        )
                    )
            )

            // 2. HEADER FLOTANT (Back + Indicator)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassButton(onClick = onBack, size = 60.dp) {
                    Image(
                        painter = painterResource(id = R.drawable.ui_btn_home),
                        contentDescription = "Back",
                        modifier = Modifier.fillMaxSize(0.6f)
                    )
                }

                GlassStageIndicator(currentStage = uiState.stage)
            }

            // 3. ZONA CENTRALĂ (PIZZA / CUPTOR)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.stage == CookingStage.BAKING) {
                    BakingOvenView(progress = uiState.bakeProgress, ovenRes = OVEN_RES)
                } else {
                    PizzaView(
                        stage = uiState.stage,
                        toppings = uiState.placedToppings,
                        isFinished = uiState.isPizzaFinished,
                        biteMarks = uiState.biteMarks,
                        lastDropFxId = uiState.lastDropFxId,
                        lastDropPos = uiState.lastDropFxPosFromCenter,
                        onLayout = { center, radius ->
                            pizzaCenter = center
                            pizzaRadius = radius
                        },
                        onBite = { 
                            soundPlayer.playClick()
                            viewModel.takeBite(pizzaRadius)
                        }
                    )
                }
            }

            // 4. INTERFAȚA DE JOS (FLOATING GLASS DOCK)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.BottomCenter
            ) {
                AnimatedContent(
                    targetState = uiState.stage,
                    transitionSpec = {
                        slideInVertically { it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                    },
                    label = "Controls"
                ) { stage ->
                    when (stage) {
                        CookingStage.ROLLING -> {
                            ControlPanel(
                                title = "Întinde aluatul!",
                                toolRes = TOOL_ROLLER_RES,
                                targetCenter = pizzaCenter,
                                targetRadius = pizzaRadius,
                                progress = uiState.rollProgress,
                                onProgress = { viewModel.addRollingProgress(it) }
                            )
                        }
                        CookingStage.SAUCE -> {
                            ControlPanel(
                                title = "Pune sosul!",
                                toolRes = TOOL_LADLE_RES,
                                targetCenter = pizzaCenter,
                                targetRadius = pizzaRadius,
                                progress = uiState.sauceProgress,
                                onProgress = { viewModel.addSauceProgress(it) }
                            )
                        }
                        CookingStage.TOPPING -> {
                            IngredientsDock(
                                ingredients = viewModel.availableIngredients,
                                isReady = uiState.isRecipeComplete,
                                onDrop = { id, pos -> 
                                    soundPlayer.playClick()
                                    viewModel.addTopping(id, pos) 
                                },
                                onBake = {
                                    soundPlayer.playClick()
                                    viewModel.startBaking()
                                },
                                pizzaCenter = pizzaCenter,
                                pizzaRadius = pizzaRadius
                            )
                        }
                        CookingStage.EATING -> {
                            if (uiState.isPizzaFinished) {
                                GlassButton(
                                    onClick = { 
                                        soundPlayer.playClick()
                                        viewModel.resetGame() 
                                    },
                                    size = 80.dp,
                                    color = Color(0xFF4CAF50)
                                ) {
                                    Text("AGAIN", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(
                                    "Apasă să mănânci!",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    style = TextStyle(
                                        shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
                                    )
                                )
                            }
                        }
                        else -> Spacer(Modifier.height(1.dp))
                    }
                }
            }

            // 5. REȚETA (Card Plutitor Stânga)
            if (uiState.stage == CookingStage.TOPPING) {
                RecipeCard(
                    recipe = uiState.recipe,
                    placedToppings = uiState.placedToppings,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 100.dp, start = 16.dp)
                )
            }
        }
    }
}

// ==============================================================================
// 🎨 UI COMPONENTS (GLASS & ANIMATIONS)
// ==============================================================================

@Composable
fun PizzaView(
    stage: CookingStage,
    toppings: List<PlacedTopping>,
    isFinished: Boolean,
    biteMarks: List<BiteMark>,
    lastDropFxId: Long,
    lastDropPos: Offset,
    onLayout: (Offset, Float) -> Unit,
    onBite: () -> Unit
) {
    // Animație "Breath" (Pulsare)
    val infiniteTransition = rememberInfiniteTransition(label = "breath")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val baseScale = if (stage == CookingStage.EATING) 1.1f else 1f
    val finalScale = baseScale * breathScale

    Box(
        modifier = Modifier
            .size(340.dp)
            .scale(finalScale)
            .onGloballyPositioned {
                val pos = it.positionInRoot()
                onLayout(
                    Offset(pos.x + it.size.width / 2f, pos.y + it.size.height / 2f),
                    it.size.width / 2f
                )
            }
            .pointerInput(stage) {
                if (stage == CookingStage.EATING) {
                    detectTapGestures { onBite() }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (!isFinished) {
            // Umbra sub pizza
            Canvas(modifier = Modifier.fillMaxSize().zIndex(-1f)) {
                drawCircle(
                    color = Color.Black.copy(alpha = 0.3f),
                    radius = size.minDimension / 2f,
                    center = center.copy(y = center.y + 10f)
                )
            }

            // Stratul de bază (Aluat)
            val baseImg = when(stage) {
                CookingStage.ROLLING -> PIZZA_DOUGH_RES
                CookingStage.SAUCE -> PIZZA_FLAT_RES
                else -> PIZZA_SAUCE_RES // Sau baked dacă ai imaginea coaptă separat
            }
            
            // Dacă imaginea e placeholder (buton home), desenăm un cerc ca fallback
            if (baseImg == R.drawable.ui_btn_home) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = Color(0xFFFFCC80))
                    drawCircle(color = Color(0xFFE65100), style = Stroke(width = 20f)) // Crustă
                }
            } else {
                Image(painter = painterResource(id = baseImg), contentDescription = null, modifier = Modifier.fillMaxSize())
            }

            // Ingrediente
            if (stage == CookingStage.TOPPING || stage == CookingStage.EATING || stage == CookingStage.BAKING) {
                toppings.forEach { t ->
                    Image(
                        painter = painterResource(id = t.imageRes),
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp)
                            .offset { IntOffset(t.positionFromCenter.x.roundToInt(), t.positionFromCenter.y.roundToInt()) }
                            .rotate(t.rotation)
                            .scale(t.scale)
                    )
                }
                // Efect vizual la adăugare
                DropSparkleFx(lastDropFxId, 170.dp.value, lastDropPos)
            }

            // Mușcături
            if (stage == CookingStage.EATING && biteMarks.isNotEmpty()) {
                BiteMarksOverlay(biteMarks)
            }
        } else {
            // FIX: Shadow folosit corect
            Text(
                "DELICIOS!",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF4CAF50),
                modifier = Modifier.rotate(-10f),
                style = TextStyle(
                    shadow = Shadow(Color.Black, Offset(2f, 2f), 10f)
                )
            )
        }
    }
}

@Composable
fun ControlPanel(
    title: String,
    toolRes: Int,
    targetCenter: Offset,
    targetRadius: Float,
    progress: Float,
    onProgress: (Float) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Titlu plutitor
        Surface(
            color = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(50),
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }

        // Unealta
        DraggableToolItem(
            imageRes = toolRes,
            targetCenter = targetCenter,
            targetRadius = targetRadius,
            progress = progress,
            onDragProgress = onProgress
        )
    }
}

@Composable
fun IngredientsDock(
    ingredients: List<IngredientOption>,
    isReady: Boolean,
    onDrop: (Int, Offset) -> Unit,
    onBake: () -> Unit,
    pizzaCenter: Offset,
    pizzaRadius: Float
) {
    // Dock din sticlă (Glassmorphism)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.15f)) // Translucid
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(ingredients) { ing ->
                DraggableIngredientItem(
                    ingredient = ing,
                    pizzaCenter = pizzaCenter,
                    pizzaRadius = pizzaRadius,
                    onDrop = { pos -> onDrop(ing.imageRes, pos) }
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Buton COACE
        val btnColor by animateColorAsState(if (isReady) Color(0xFFFF5722) else Color.White.copy(alpha = 0.1f))
        val scale by animateFloatAsState(if (isReady) 1.05f else 1f)

        Box(
            modifier = Modifier
                .width(90.dp)
                .fillMaxHeight()
                .scale(scale)
                .clip(RoundedCornerShape(18.dp))
                .background(btnColor)
                .clickable(enabled = isReady) { onBake() }
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Icon cuptor (sau text)
                Text(
                    if (isReady) "COACE!" else "REȚETA",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun RecipeCard(
    recipe: List<RecipeRequirement>,
    placedToppings: List<PlacedTopping>,
    modifier: Modifier = Modifier
) {
    // Card mic transparent
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)) // FIX: BorderStroke importat
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("MISIUNE", color = Color.White.copy(0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            recipe.forEach { req ->
                val current = placedToppings.count { it.imageRes == req.imageRes }
                val isDone = current >= req.requiredCount
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isDone) "✔ " else "○ ",
                        color = if (isDone) Color.Green else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Image(
                        painter = painterResource(id = req.imageRes),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$current / ${req.requiredCount}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun BakingOvenView(progress: Float, ovenRes: Int) {
    Box(contentAlignment = Alignment.Center) {
        // Glow Effect
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(
                    Brush.radialGradient(listOf(Color(0xFFFF5722).copy(alpha = 0.6f), Color.Transparent)),
                    CircleShape
                )
        )
        
        // Imaginea cuptorului sau placeholder
        if (ovenRes == R.drawable.ui_btn_home) {
             Box(modifier = Modifier.size(280.dp).background(Color(0xFF3E2723), RoundedCornerShape(32.dp)).border(4.dp, Color.Gray, RoundedCornerShape(32.dp)))
        } else {
            Image(painter = painterResource(id = ovenRes), contentDescription = null, modifier = Modifier.size(320.dp))
        }

        Text("SE COACE...", 
            color = Color.White, 
            fontSize = 28.sp, 
            fontWeight = FontWeight.Black, 
            style = TextStyle(shadow = Shadow(Color.Black, Offset(0f, 0f), 8f)) // FIX: Shadow
        )

        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .width(200.dp)
                .height(12.dp)
                .align(Alignment.BottomCenter)
                .offset(y = (-40).dp)
                .clip(CircleShape),
            color = Color(0xFFFFCC80),
            trackColor = Color.Black.copy(0.5f)
        )
    }
}

// 📦 HELPERE UI (Butoane, Indicatoare)

@Composable
fun GlassButton(onClick: () -> Unit, size: Dp, color: Color = Color.White.copy(alpha = 0.2f), content: @Composable BoxScope.() -> Unit) {
    SquishyButton(
        onClick = onClick,
        size = size,
        color = color,
        shape = CircleShape,
        elevation = 0.dp
    ) {
        content()
    }
}

@Composable
fun GlassStageIndicator(currentStage: CookingStage) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.3f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CookingStage.values().forEach { stage ->
            val isActive = stage == currentStage
            val isPassed = stage.ordinal < currentStage.ordinal
            val color = if (isActive) Color(0xFFFF9800) else if (isPassed) Color(0xFF4CAF50) else Color.White.copy(0.2f)
            val size by animateDpAsState(if (isActive) 12.dp else 8.dp)
            Box(Modifier.size(size).clip(CircleShape).background(color))
        }
    }
}

// 📦 LOCAL COMPONENTS (DRAG LOGIC, VFX) - INDEPENDENTE

@Composable
fun DraggableToolItem(imageRes: Int, targetCenter: Offset, targetRadius: Float, progress: Float, onDragProgress: (Float) -> Unit) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var initialPos by remember { mutableStateOf(Offset.Zero) }
    val scale by animateFloatAsState(if (isDragging) 1.2f else 1f)
    val rotation by animateFloatAsState(if (isDragging) -15f else 0f)

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(100.dp)
            .scale(scale)
            .rotate(rotation)
            .onGloballyPositioned {
                if (initialPos == Offset.Zero) initialPos = it.positionInRoot().let { p -> Offset(p.x + it.size.width/2, p.y + it.size.height/2) }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false; offsetX = 0f; offsetY = 0f },
                    onDragCancel = { isDragging = false; offsetX = 0f; offsetY = 0f }
                ) { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                    if (targetRadius > 0f && initialPos != Offset.Zero) {
                        val current = initialPos + Offset(offsetX, offsetY)
                        if ((current - targetCenter).getDistance() < targetRadius) {
                            onDragProgress((hypot(dragAmount.x, dragAmount.y) / 500f).coerceIn(0f, 0.05f))
                        }
                    }
                }
            }
    ) {
        Image(painter = painterResource(id = imageRes), contentDescription = null, modifier = Modifier.fillMaxSize())
        if (progress > 0f && progress < 1f) {
             androidx.compose.material3.CircularProgressIndicator(progress = progress, modifier = Modifier.align(Alignment.Center).size(40.dp), color = Color.Green, strokeWidth = 4.dp)
        }
    }
}

@Composable
fun DraggableIngredientItem(ingredient: IngredientOption, pizzaCenter: Offset, pizzaRadius: Float, onDrop: (Offset) -> Unit) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var initialPos by remember { mutableStateOf(Offset.Zero) }
    val scale by animateFloatAsState(if (isDragging) 1.3f else 1f)

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(70.dp)
            .scale(scale)
            .zIndex(if (isDragging) 100f else 1f)
            .onGloballyPositioned {
                if (initialPos == Offset.Zero) initialPos = it.positionInRoot().let { p -> Offset(p.x + it.size.width/2, p.y + it.size.height/2) }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        if (pizzaRadius > 0f) {
                            val current = initialPos + Offset(offsetX, offsetY)
                            if ((current - pizzaCenter).getDistance() < pizzaRadius) {
                                onDrop(current - pizzaCenter)
                            }
                        }
                        offsetX = 0f; offsetY = 0f
                    },
                    onDragCancel = { isDragging = false; offsetX = 0f; offsetY = 0f }
                ) { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
    ) {
        Surface(shape = CircleShape, color = Color.White, shadowElevation = if (isDragging) 8.dp else 2.dp) {
            Image(painter = painterResource(id = ingredient.imageRes), contentDescription = null, modifier = Modifier.padding(10.dp).fillMaxSize())
        }
    }
}

@Composable
fun SquishyButton(onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp? = null, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp), color: Color = Color.White, elevation: Dp = 4.dp, content: @Composable BoxScope.() -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.9f else 1f, spring(stiffness = Spring.StiffnessMedium))
    Surface(onClick = onClick, modifier = modifier.scale(scale).let { if (size != null) it.size(size) else it }, shape = shape, color = color, shadowElevation = elevation, interactionSource = interactionSource) { Box(contentAlignment = Alignment.Center, content = content) }
}

@Composable
fun ConfettiBox(burstId: Long, content: @Composable () -> Unit) {
    val particles = remember { mutableStateListOf<ConfettiParticle>() }
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        LaunchedEffect(burstId) {
            if (burstId > 0) {
                repeat(60) {
                    particles.add(ConfettiParticle(it, Random.nextFloat() * w, -50f, listOf(Color.Red, Color.Green, Color.Yellow, Color.Cyan).random(), Random.nextFloat() + 0.5f, 0f, 0f, 0f, Random.nextFloat() * 500 + 400))
                }
                while(isActive && particles.isNotEmpty()) {
                    val dt = 0.016f
                    val iter = particles.listIterator()
                    while(iter.hasNext()) {
                        val p = iter.next()
                        p.y += p.vy * dt
                        p.x += sin(p.y / 100) * 5 
                        if (p.y > h) iter.remove()
                    }
                    delay(16)
                }
            }
        }
        Box(Modifier.fillMaxSize()) {
            content()
            particles.forEach { p -> Box(Modifier.offset { IntOffset(p.x.toInt(), p.y.toInt()) }.size(8.dp).background(p.color, CircleShape)) }
        }
    }
}

data class ConfettiParticle(val id: Int, var x: Float, var y: Float, val color: Color, val scale: Float, val rotationSpeed: Float, var currentRotation: Float, var vx: Float, var vy: Float)

@Composable
fun DropSparkleFx(fxId: Long, radius: Float, pos: Offset) {
    if (fxId == 0L) return
    var visible by remember(fxId) { mutableStateOf(true) }
    val scale by animateFloatAsState(if (visible) 0.5f else 1.8f, tween(400))
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(400))
    LaunchedEffect(fxId) { visible = true; delay(400); visible = false }
    if (alpha > 0f) {
        Box(Modifier.offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }.scale(scale).alpha(alpha)) {
            Canvas(Modifier.size(60.dp)) {
                drawCircle(color = Color.White, style = Stroke(width = 6f))
                drawCircle(color = Color.Yellow.copy(alpha=0.5f), radius = size.minDimension/3)
            }
        }
    }
}

@Composable
fun BiteMarksOverlay(biteMarks: List<BiteMark>) {
    Canvas(Modifier.fillMaxSize()) {
        biteMarks.forEach { m -> drawCircle(color = Color(0xFFFFF9C4), radius = m.radiusPx, center = center + m.offsetFromCenter) }
    }
}