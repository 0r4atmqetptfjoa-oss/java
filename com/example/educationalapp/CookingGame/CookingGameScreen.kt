package com.example.educationalapp.features.games

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.educationalapp.R
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.math.sin

// FIX: Folosim 'val' simplu, nu 'const val', deoarece R.drawable.* nu sunt constante de compilare pure în Kotlin
private val BG_KITCHEN_RES: Int = R.drawable.bg_pizzeria_kitchen
private val PIZZA_DOUGH_RES: Int = R.drawable.pizza_1_dough_ball
private val PIZZA_FLAT_RES: Int = R.drawable.pizza_2_flat
private val PIZZA_SAUCE_RES: Int = R.drawable.pizza_3_sauced
private val TOOL_ROLLER_RES: Int = R.drawable.tool_rolling_pin
private val TOOL_LADLE_RES: Int = R.drawable.tool_sauce_ladle
private val OVEN_RES: Int = R.drawable.prop_oven_open

@Composable
fun CookingGameScreen(
    viewModel: CookingGameViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var showConfetti by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isPizzaFinished) {
        if (uiState.isPizzaFinished) {
            showConfetti = true
        } else {
            showConfetti = false
        }
    }

    var pizzaGlobalCenter by remember { mutableStateOf(Offset.Zero) }
    var pizzaRadiusPx by remember { mutableStateOf(0f) }

    // Particle System
    val particles = remember { mutableStateListOf<VisualParticle>() }

    LaunchedEffect(Unit) {
        while (true) {
            val dt = 16f 
            val iterator = particles.listIterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                p.life -= dt
                p.x += p.vx
                p.y += p.vy
                p.scale *= 0.95f
                if (p.life <= 0) iterator.remove()
            }
            delay(16)
        }
    }

    fun spawnParticles(type: ParticleType, pos: Offset) {
        repeat(if (type == ParticleType.FLOUR) 3 else 1) {
            particles.add(
                VisualParticle(
                    x = pos.x + Random.nextInt(-20, 20).toFloat(),
                    y = pos.y + Random.nextInt(-20, 20).toFloat(),
                    color = if (type == ParticleType.FLOUR) Color.White.copy(alpha = 0.6f) else Color(0xFFD32F2F),
                    scale = if (type == ParticleType.FLOUR) Random.nextFloat() * 1.5f else Random.nextFloat() * 0.8f + 0.5f,
                    life = if (type == ParticleType.FLOUR) 600f else 800f,
                    vx = Random.nextFloat() * 4 - 2,
                    vy = Random.nextFloat() * 4 - 2
                )
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = BG_KITCHEN_RES),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(0.5f))))
        )

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
                    rollProgress = uiState.rollProgress,
                    sauceProgress = uiState.sauceProgress,
                    onLayout = { center, radius ->
                        pizzaGlobalCenter = center
                        pizzaRadiusPx = radius
                    },
                    onBite = { pos ->
                        viewModel.takeBite(pizzaRadiusPx, pos)
                    }
                )
            }
        }
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                drawCircle(
                    color = p.color,
                    radius = 15f * p.scale,
                    center = Offset(p.x, p.y)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedContent(targetState = uiState.stage, label = "Controls") { stage ->
                when (stage) {
                    CookingStage.ROLLING -> {
                        ControlPanel(
                            title = "Întinde aluatul!",
                            toolRes = TOOL_ROLLER_RES,
                            pizzaCenter = pizzaGlobalCenter,
                            pizzaRadius = pizzaRadiusPx,
                            progress = uiState.rollProgress,
                            onAction = { pos, delta ->
                                viewModel.addRollingProgress(delta)
                                spawnParticles(ParticleType.FLOUR, pos)
                            }
                        )
                    }
                    CookingStage.SAUCE -> {
                        ControlPanel(
                            title = "Pune sosul!",
                            toolRes = TOOL_LADLE_RES,
                            pizzaCenter = pizzaGlobalCenter,
                            pizzaRadius = pizzaRadiusPx,
                            progress = uiState.sauceProgress,
                            onAction = { pos, delta ->
                                viewModel.addSauceProgress(delta)
                                spawnParticles(ParticleType.SAUCE, pos)
                            }
                        )
                    }
                    CookingStage.TOPPING -> {
                        IngredientsDock(
                            ingredients = viewModel.availableIngredients,
                            isReady = true,
                            pizzaCenter = pizzaGlobalCenter,
                            pizzaRadius = pizzaRadiusPx,
                            onDrop = { id, pos ->
                                viewModel.addTopping(id, pos)
                            },
                            onBake = { viewModel.startBaking() }
                        )
                    }
                    CookingStage.EATING -> {
                        if (uiState.isPizzaFinished) {
                            GlassButton(onClick = { viewModel.resetGame() }, size = 80.dp, color = Color(0xFF4CAF50)) {
                                Text("DIN NOU", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                "Mănâncă Pizza!",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                style = TextStyle(shadow = Shadow(Color.Black, Offset(2f, 2f), 4f))
                            )
                        }
                    }
                    else -> Spacer(Modifier.height(1.dp))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GlassButton(onClick = onBack, size = 56.dp) {
                Image(painter = painterResource(R.drawable.ui_btn_home), contentDescription = "Back", modifier = Modifier.padding(12.dp))
            }
            if (uiState.stage == CookingStage.TOPPING) {
                RecipeCard(uiState.recipe, uiState.placedToppings)
            }
        }

        if (showConfetti) {
            ConfettiOverlay()
        }
    }
}

// ... Restul componentelor PizzaView, ControlPanel, etc. rămân neschimbate ...
// Asigură-te că păstrezi tot codul de mai jos din fișierul original, l-am lăsat neatins aici pentru a nu aglomera.
// Dă copy-paste doar la partea de sus dacă ai deja restul, sau copiază tot fișierul din răspunsul anterior dacă vrei siguranță.
// Pentru siguranță, voi re-include helper-ele esențiale mai jos:

@Composable
fun PizzaView(
    stage: CookingStage,
    toppings: List<PlacedTopping>,
    isFinished: Boolean,
    biteMarks: List<BiteMark>,
    rollProgress: Float,
    sauceProgress: Float,
    onLayout: (Offset, Float) -> Unit,
    onBite: (Offset) -> Unit
) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f, targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "scale"
    )

    val doughScale = 0.6f + (rollProgress * 0.4f)
    val actualScale = if (stage == CookingStage.ROLLING) doughScale else 1f
    
    Box(
        modifier = Modifier
            .size(360.dp)
            .scale(pulse * actualScale)
            .onGloballyPositioned {
                val pos = it.positionInRoot()
                val center = Offset(pos.x + it.size.width / 2f, pos.y + it.size.height / 2f)
                onLayout(center, it.size.width / 2f)
            }
            .pointerInput(stage) {
                if (stage == CookingStage.EATING) {
                    detectTapGestures { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        onBite(offset - center)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (!isFinished) {
            Canvas(Modifier.fillMaxSize().zIndex(-1f)) {
                drawCircle(Color.Black.copy(0.3f), radius = size.minDimension / 2f, center = center.copy(y = center.y + 10f))
            }

            Image(painter = painterResource(PIZZA_FLAT_RES), contentDescription = null, modifier = Modifier.fillMaxSize())
            
            if (stage == CookingStage.SAUCE || stage.ordinal > CookingStage.SAUCE.ordinal) {
                val alpha = if (stage == CookingStage.SAUCE) sauceProgress else 1f
                Image(
                    painter = painterResource(PIZZA_SAUCE_RES),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().alpha(alpha)
                )
            }
            
            if (stage == CookingStage.ROLLING && rollProgress < 1f) {
                val ballAlpha = 1f - rollProgress
                 Image(
                    painter = painterResource(PIZZA_DOUGH_RES),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().scale(1.2f - rollProgress * 0.2f).alpha(ballAlpha)
                )
            }

            toppings.forEach { t ->
                ToppingItem(t)
            }

            if (biteMarks.isNotEmpty()) {
                Canvas(Modifier.fillMaxSize()) {
                    biteMarks.forEach { mark ->
                        drawCircle(
                            color = Color(0xFFFFF3E0),
                            radius = mark.radiusPx,
                            center = center + mark.offsetFromCenter
                        )
                    }
                }
            }
        } else {
            Text("DELICIOS!", fontSize = 50.sp, color = Color(0xFF43A047), fontWeight = FontWeight.Black, modifier = Modifier.rotate(-15f))
        }
    }
}

@Composable
fun ToppingItem(topping: PlacedTopping) {
    val animatedScale = remember { Animatable(0f) }
    LaunchedEffect(topping.id) {
        animatedScale.animateTo(
            targetValue = topping.scale,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)
        )
    }

    Image(
        painter = painterResource(id = topping.imageRes),
        contentDescription = null,
        modifier = Modifier
            .size(50.dp)
            .offset { IntOffset(topping.positionFromCenter.x.roundToInt(), topping.positionFromCenter.y.roundToInt()) }
            .rotate(topping.rotation)
            .scale(animatedScale.value)
    )
}

@Composable
fun ControlPanel(
    title: String,
    toolRes: Int,
    pizzaCenter: Offset,
    pizzaRadius: Float,
    progress: Float,
    onAction: (Offset, Float) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(color = Color.Black.copy(0.6f), shape = RoundedCornerShape(20.dp)) {
            Text(title, color = Color.White, modifier = Modifier.padding(12.dp, 6.dp), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        
        var toolOffset by remember { mutableStateOf(Offset.Zero) }
        var isDragging by remember { mutableStateOf(false) }
        
        val animatedOffset by animateOffsetAsState(if (isDragging) toolOffset else Offset.Zero)
        val rotation by animateFloatAsState(if (isDragging) -15f else 0f)

        Box(
            modifier = Modifier
                .size(120.dp)
                .offset { IntOffset(animatedOffset.x.roundToInt(), animatedOffset.y.roundToInt()) }
                .rotate(rotation)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false; toolOffset = Offset.Zero },
                        onDragCancel = { isDragging = false; toolOffset = Offset.Zero }
                    ) { change, dragAmount ->
                        change.consume()
                        toolOffset += dragAmount
                        
                        if (toolOffset.y < -150f) { 
                             onAction(
                                 Offset(change.position.x, change.position.y), 
                                 0.02f 
                             )
                        }
                    }
                }
        ) {
            Image(painter = painterResource(toolRes), contentDescription = null, modifier = Modifier.fillMaxSize())
            if (progress > 0 && progress < 1f) {
                LinearProgressIndicator(progress = progress, modifier = Modifier.align(Alignment.BottomCenter).width(60.dp).padding(bottom = 10.dp), color = Color.Green)
            }
        }
    }
}

@Composable
fun IngredientsDock(
    ingredients: List<IngredientOption>,
    isReady: Boolean,
    pizzaCenter: Offset,
    pizzaRadius: Float,
    onDrop: (Int, Offset) -> Unit,
    onBake: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(0.2f))
            .border(1.dp, Color.White.copy(0.4f), RoundedCornerShape(24.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(ingredients) { ing ->
                DraggableIngredient(
                    ingredient = ing,
                    pizzaCenter = pizzaCenter,
                    pizzaRadius = pizzaRadius,
                    onDrop = onDrop
                )
            }
        }
        
        Box(
            modifier = Modifier
                .padding(8.dp)
                .width(100.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFFF5722))
                .clickable { onBake() },
            contentAlignment = Alignment.Center
        ) {
            Text("COACE!", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }
}

@Composable
fun DraggableIngredient(
    ingredient: IngredientOption,
    pizzaCenter: Offset,
    pizzaRadius: Float,
    onDrop: (Int, Offset) -> Unit
) {
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var startPosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .size(70.dp)
            .onGloballyPositioned { 
                val pos = it.positionInRoot()
                startPosition = Offset(pos.x + it.size.width/2, pos.y + it.size.height/2)
            }
            .zIndex(if (isDragging) 10f else 1f)
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        val finalAbsolutePos = startPosition + offset
                        val dist = (finalAbsolutePos - pizzaCenter).getDistance()
                        
                        if (dist < pizzaRadius) {
                            val relativePos = finalAbsolutePos - pizzaCenter
                            onDrop(ingredient.imageRes, relativePos)
                        }
                        offset = Offset.Zero
                    },
                    onDragCancel = { isDragging = false; offset = Offset.Zero }
                ) { change, dragAmount ->
                    change.consume()
                    offset += dragAmount
                }
            }
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = if (isDragging) 8.dp else 2.dp,
            modifier = Modifier.fillMaxSize().scale(if (isDragging) 1.2f else 1f)
        ) {
            Image(
                painter = painterResource(ingredient.imageRes),
                contentDescription = null,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

@Composable
fun BakingOvenView(progress: Float, ovenRes: Int) {
    Box(contentAlignment = Alignment.Center) {
        val alpha by rememberInfiniteTransition().animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(500), RepeatMode.Reverse))
        Box(Modifier.size(320.dp).background(Brush.radialGradient(listOf(Color.Red.copy(alpha), Color.Transparent))))
        
        Image(painter = painterResource(ovenRes), contentDescription = null, modifier = Modifier.size(300.dp))
        
        Column(Modifier.align(Alignment.BottomCenter).offset(y = (-20).dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Se coace...", 
                color = Color.White, 
                fontWeight = FontWeight.Bold, 
                style = TextStyle(shadow = Shadow(Color.Black, Offset(2f, 2f), 4f))
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = progress, modifier = Modifier.width(150.dp).height(10.dp).clip(CircleShape), color = Color(0xFFFF9800), trackColor = Color.Black.copy(0.5f))
        }
    }
}

@Composable
fun RecipeCard(recipe: List<RecipeRequirement>, placedToppings: List<PlacedTopping>) {
    Surface(
        color = Color.Black.copy(0.7f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.3f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("MISIUNE", color = Color(0xFFFFCC80), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            recipe.forEach { req ->
                val current = placedToppings.count { it.imageRes == req.imageRes }
                val isDone = current >= req.requiredCount
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isDone) "✔" else "○", color = if (isDone) Color.Green else Color.White)
                    Spacer(Modifier.width(8.dp))
                    Image(painter = painterResource(req.imageRes), contentDescription = null, modifier = Modifier.size(24.dp))
                    Text(" $current/${req.requiredCount}", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GlassButton(onClick: () -> Unit, size: Dp, color: Color = Color.White.copy(0.2f), content: @Composable BoxScope.() -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = color,
        border = BorderStroke(1.dp, Color.White.copy(0.3f)),
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center, content = content)
    }
}

@Composable
fun ConfettiOverlay() {
    val particles = remember { List(50) { ConfettiP(it) } }
    var time by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        val startTime = System.nanoTime()
        while(true) {
            time = (System.nanoTime() - startTime) / 1_000_000_000f
            withFrameNanos { }
        }
    }
    
    Canvas(Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val y = (p.speed * time + p.offsetY) % size.height
            val wobbleVal = sin((time * p.wobble).toDouble()).toFloat() * 50f
            val x = (p.offsetX + wobbleVal) % size.width
            drawCircle(p.color, radius = 8f, center = Offset(x, y))
        }
    }
}

data class ConfettiP(val id: Int) {
    val color = listOf(Color.Red, Color.Green, Color.Yellow, Color.Cyan, Color.Magenta).random()
    val offsetX = Random.nextFloat() * 1000
    val offsetY = Random.nextFloat() * -1000
    val speed = Random.nextFloat() * 300 + 200
    val wobble = Random.nextFloat() * 5
}

data class VisualParticle(
    var x: Float, var y: Float,
    val color: Color,
    var scale: Float,
    var life: Float,
    val vx: Float, val vy: Float
)
enum class ParticleType { FLOUR, SAUCE }