package com.example.educationalapp.features.games

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat // Import adăugat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * 2026-style upgrade:
 * - interactive progress (dig/seed/water) while moving the tool over the patch
 * - premium feedback (rings, sparkles, confetti)
 * - cloud drag reveals sun (growth)
 * - tap the grown plant to harvest and auto-start the next round
 */
@Composable
fun MagicGardenGameScreen(
    viewModel: MagicGardenViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val soundPlayer = remember { AlphabetSoundPlayer(context) }
    val scope = rememberCoroutineScope()

    // Confetti burst id
    var confettiBurstId by remember { mutableLongStateOf(0L) }

    // Patch center for hit testing
    var gardenCenter by remember { mutableStateOf(Offset.Zero) }

    // local sparkles (simple, build-safe)
    val sparkles = remember { mutableStateListOf<Sparkle>() }

    fun spawnSparkle(at: Offset) {
        val s = Sparkle(id = System.currentTimeMillis() + sparkles.size, pos = at)
        sparkles.add(s)
        scope.launch {
            delay(420)
            sparkles.remove(s)
        }
    }

    LaunchedEffect(uiState.stage) {
        if (uiState.stage == GardenStage.GROWN) {
            soundPlayer.playCorrect()
            confettiBurstId = System.currentTimeMillis()
        }
    }

    ConfettiBox(burstId = confettiBurstId) {
        Box(modifier = Modifier.fillMaxSize()) {

            // Subtle background "breath"
            val bgBreath by rememberInfiniteTransition(label = "bg")
                .animateFloat(
                    initialValue = 1.02f,
                    targetValue = 1.06f,
                    animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing)),
                    label = "bgScale"
                )

            Image(
                painter = painterResource(id = R.drawable.bg_sunny_meadow),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(bgBreath),
                contentScale = ContentScale.Crop
            )

            // Top bar: Back + counter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ui_button_home),
                    contentDescription = "Back",
                    modifier = Modifier
                        .size(62.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { onBack() }
                        }
                )

                Spacer(Modifier.weight(1f))

                Text(
                    text = "Harvest: ${uiState.harvestCount}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Sun (top-right)
            val sunAlpha by animateFloatAsState(
                targetValue = if (uiState.isCloudMoved || uiState.stage == GardenStage.GROWN) 1f else 0.25f,
                animationSpec = tween(450),
                label = "sunAlpha"
            )
            val sunPulse by rememberInfiniteTransition(label = "sun")
                .animateFloat(
                    initialValue = 1f,
                    targetValue = 1.06f,
                    animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
                    label = "sunPulse"
                )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 64.dp, end = 40.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.char_sun_happy),
                    contentDescription = "Sun",
                    modifier = Modifier
                        .size(140.dp)
                        .alpha(sunAlpha)
                        .scale(sunPulse)
                )
                if (uiState.isCloudMoved || uiState.stage == GardenStage.GROWN) {
                    SunRays(modifier = Modifier.matchParentSize().alpha(0.55f))
                }
            }

            // Cloud (draggable) – only when WATERED
            DraggableCloud(
                isEnabled = (uiState.stage == GardenStage.WATERED),
                isMovedAway = uiState.isCloudMoved,
                onMovedAway = {
                    soundPlayer.playClick()
                    viewModel.onCloudMovedAway()
                }
            )

            // Main garden patch (bigger + premium overlays)
            GardenPatch(
                stage = uiState.stage,
                plantRes = uiState.currentPlant.imageRes,
                progress = uiState.actionProgress,
                isCelebrating = uiState.isCelebrating,
                sparkles = sparkles,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 128.dp)
                    .size(340.dp)
                    .onGloballyPositioned {
                        val pos = it.positionInRoot()
                        val size = it.size
                        gardenCenter = Offset(pos.x + size.width / 2f, pos.y + size.height / 2f)
                    }
                    .pointerInput(uiState.stage) {
                        if (uiState.stage == GardenStage.GROWN) {
                            detectTapGestures {
                                soundPlayer.playClick()
                                viewModel.harvestAndNext()
                            }
                        }
                    }
            )

            // Tool shelf (bottom) – shows next tool and provides a draggable tool instance
            ToolShelf(
                currentTool = viewModel.currentTool(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )

            // Draggable tool instance (only when needed)
            viewModel.currentTool()?.let { tool ->
                val toolRes = when (tool) {
                    ToolType.SHOVEL -> R.drawable.ui_button_home
                    ToolType.SEEDS -> R.drawable.food_cookie
                    ToolType.WATER -> R.drawable.char_drop_happy
                }

                DraggableTool2026(
                    toolType = tool,
                    imageRes = toolRes,
                    patchCenter = gardenCenter,
                    stage = uiState.stage,
                    onWorkTick = { intensity ->
                        // Intensity is derived from drag speed; clamp to a safe range.
                        val d = (0.004f * intensity).coerceIn(0.001f, 0.03f)
                        viewModel.addActionProgress(d)
                    },
                    onMilestone = { at ->
                        spawnSparkle(at)
                        soundPlayer.playClick()
                    }
                )
            }

            // Big CTA when grown: tap plant to harvest
            if (uiState.stage == GardenStage.GROWN) {
                val pop by animateFloatAsState(1f, spring(dampingRatio = 0.6f), label = "pop")
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 36.dp)
                        .scale(pop)
                ) {
                    Text(
                        text = "Tap plant to harvest",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            // Reset button (top-right small) – optional, always available
            SquishyButton(
                onClick = {
                    soundPlayer.playClick()
                    viewModel.resetGame()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 18.dp, end = 16.dp),
                size = 64.dp,
                color = Color.White
            ) {
                Text("↻", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
            }
        }
    }
}

private data class Sparkle(val id: Long, val pos: Offset)

@Composable
private fun GardenPatch(
    stage: GardenStage,
    plantRes: Int,
    progress: Float,
    isCelebrating: Boolean,
    sparkles: List<Sparkle>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Dirt / hole base (use existing prop as safe)
        val dirtRes = when (stage) {
            GardenStage.GRASS -> R.drawable.prop_bush
            else -> R.drawable.prop_bush
        }
        Image(
            painter = painterResource(id = dirtRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Stage visuals
        when (stage) {
            GardenStage.DUG -> {
                Text("🕳️", fontSize = 54.sp, modifier = Modifier.offset(y = 12.dp))
            }
            GardenStage.SEEDED -> {
                Text("🌱", fontSize = 56.sp, modifier = Modifier.offset(y = 0.dp))
            }
            GardenStage.WATERED -> {
                Image(
                    painter = painterResource(id = R.drawable.char_cactus_happy),
                    contentDescription = "Sprout",
                    modifier = Modifier.size(120.dp).offset(y = (-6).dp)
                )
            }
            GardenStage.GROWN -> {
                val scale by animateFloatAsState(
                    targetValue = if (isCelebrating) 1.35f else 1.25f,
                    animationSpec = spring(dampingRatio = 0.55f),
                    label = "plantScale"
                )
                Image(
                    painter = painterResource(id = plantRes),
                    contentDescription = "Plant",
                    modifier = Modifier
                        .size(240.dp)
                        .scale(scale)
                        .offset(y = (-18).dp)
                )
            }
            else -> Unit
        }

        // Progress ring for actions (dig/seed/water)
        if (stage == GardenStage.GRASS || stage == GardenStage.DUG || stage == GardenStage.SEEDED) {
            ProgressRing(
                progress = progress,
                label = when (stage) {
                    GardenStage.GRASS -> "DIG"
                    GardenStage.DUG -> "SEED"
                    GardenStage.SEEDED -> "WATER"
                    else -> ""
                }
            )
        }

        // Sparkles overlay (simple)
        SparkleLayer(sparkles = sparkles)
    }
}

@Composable
private fun ProgressRing(progress: Float, label: String) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp)
    ) {
        val stroke = Stroke(width = 10f)
        drawArc(
            color = Color.White.copy(alpha = 0.18f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = stroke
        )
        drawArc(
            color = Color.White.copy(alpha = 0.95f),
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            style = stroke
        )

        // simple tick marks
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = (size.minDimension / 2f) - 6f
        for (i in 0 until 12) {
            val a = Math.toRadians((-90 + i * 30).toDouble())
            val x1 = cx + cos(a).toFloat() * (r - 18f)
            val y1 = cy + sin(a).toFloat() * (r - 18f)
            val x2 = cx + cos(a).toFloat() * r
            val y2 = cy + sin(a).toFloat() * r
            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 3f
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Text(
            text = label,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = Color.White.copy(alpha = 0.95f),
            modifier = Modifier.padding(bottom = 34.dp)
        )
    }
}

@Composable
private fun SparkleLayer(sparkles: List<Sparkle>) {
    if (sparkles.isEmpty()) return
    Canvas(modifier = Modifier.fillMaxSize()) {
        for (s in sparkles) {
            val p = s.pos
            // Convert global-ish coordinates to local approx (best-effort):
            // Here we just render around center; the caller sends patch-local offsets.
            val cx = size.width / 2f
            val cy = size.height / 2f
            val x = cx + (p.x * 0.08f)
            val y = cy + (p.y * 0.08f)
            drawCircle(Color.White.copy(alpha = 0.9f), radius = 8f, center = Offset(x, y))
            drawLine(Color.White.copy(alpha = 0.8f), Offset(x - 14f, y), Offset(x + 14f, y), 3f)
            drawLine(Color.White.copy(alpha = 0.8f), Offset(x, y - 14f), Offset(x, y + 14f), 3f)
        }
    }
}

@Composable
private fun ToolShelf(currentTool: ToolType?, modifier: Modifier = Modifier) {
    val alphaInactive = 0.35f
    val alphaActive = 1f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolIcon(res = R.drawable.ui_button_home, label = "Shovel", active = (currentTool == ToolType.SHOVEL), alphaInactive = alphaInactive, alphaActive = alphaActive)
        Spacer(Modifier.width(22.dp))
        ToolIcon(res = R.drawable.food_cookie, label = "Seeds", active = (currentTool == ToolType.SEEDS), alphaInactive = alphaInactive, alphaActive = alphaActive)
        Spacer(Modifier.width(22.dp))
        ToolIcon(res = R.drawable.char_drop_happy, label = "Water", active = (currentTool == ToolType.WATER), alphaInactive = alphaInactive, alphaActive = alphaActive)
    }
}

@Composable
private fun ToolIcon(res: Int, label: String, active: Boolean, alphaInactive: Float, alphaActive: Float) {
    val a by animateFloatAsState(if (active) alphaActive else alphaInactive, tween(250), label = "toolAlpha")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = res),
            contentDescription = label,
            modifier = Modifier
                .size(if (active) 72.dp else 62.dp)
                .alpha(a)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = a)
        )
    }
}

@Composable
private fun DraggableTool2026(
    toolType: ToolType,
    imageRes: Int,
    patchCenter: Offset,
    stage: GardenStage,
    onWorkTick: (intensity: Float) -> Unit,
    onMilestone: (Offset) -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var initialCenter by remember { mutableStateOf(Offset.Zero) }

    val rotation by animateFloatAsState(if (isDragging) -12f else 0f, tween(160), label = "rot")
    val scale by animateFloatAsState(if (isDragging) 1.18f else 1f, spring(dampingRatio = 0.65f), label = "scale")

    // Visual magnet feel: slightly bias toward patch when close
    fun magnetOffset(cx: Float, cy: Float): Offset {
        if (patchCenter == Offset.Zero) return Offset.Zero
        val dx = patchCenter.x - cx
        val dy = patchCenter.y - cy
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        val radius = 260f
        if (dist <= 1f || dist > radius) return Offset.Zero
        val t = ((radius - dist) / radius).coerceIn(0f, 1f)
        return Offset(dx * 0.12f * t, dy * 0.12f * t)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.BottomCenter)
            .padding(bottom = 88.dp)
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(118.dp)
            .zIndex(if (isDragging) 20f else 6f)
            .onGloballyPositioned {
                if (initialCenter == Offset.Zero) {
                    val pos = it.positionInRoot()
                    val size = it.size
                    initialCenter = Offset(pos.x + size.width / 2f, pos.y + size.height / 2f)
                }
            }
            .pointerInput(toolType, stage) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                        offsetX = 0f
                        offsetY = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        offsetX = 0f
                        offsetY = 0f
                    },
                    onDrag = { _, dragAmount ->
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y

                        if (patchCenter != Offset.Zero && initialCenter != Offset.Zero) {
                            val current = initialCenter + Offset(offsetX, offsetY)
                            val mag = magnetOffset(current.x, current.y)
                            offsetX += mag.x
                            offsetY += mag.y

                            val dx = current.x - patchCenter.x
                            val dy = current.y - patchCenter.y
                            val dist = kotlin.math.sqrt(dx * dx + dy * dy)

                            // Work only if close enough to patch
                            if (dist < 240f) {
                                val intensity = (abs(dragAmount.x) + abs(dragAmount.y)).coerceIn(1f, 40f)
                                onWorkTick(intensity)

                                // occasional milestone sparkle
                                if (intensity > 18f) onMilestone(Offset(dx, dy))
                            }
                        }
                    }
                )
            }
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .rotate(rotation)
        )
    }
}

@Composable
private fun DraggableCloud(
    isEnabled: Boolean,
    isMovedAway: Boolean,
    onMovedAway: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }

    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isMovedAway) -420f else offsetX,
        animationSpec = tween(900),
        label = "cloudX"
    )

    if (isMovedAway && animatedOffsetX <= -395f) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.TopCenter)
            .padding(top = 56.dp)
            .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
            .size(170.dp)
            .zIndex(5f)
            .pointerInput(isEnabled) {
                if (isEnabled) {
                    detectDragGestures(
                        onDragEnd = {
                            if (offsetX < -110f) {
                                onMovedAway()
                            } else {
                                offsetX = 0f
                            }
                        },
                        onDragCancel = { offsetX = 0f },
                        onDrag = { _, dragAmount ->
                            val newX = offsetX + dragAmount.x
                            if (newX <= 0f) offsetX = newX
                        }
                    )
                }
            }
    ) {
        Image(
            painter = painterResource(id = R.drawable.char_cloud_happy),
            contentDescription = "Cloud",
            modifier = Modifier.fillMaxSize()
        )
        if (isEnabled && offsetX == 0f) {
            Text("⬅", fontSize = 34.sp, color = Color.White, modifier = Modifier.align(Alignment.CenterStart).offset(x = (-28).dp))
        }
    }
}

@Composable
private fun SunRays(modifier: Modifier = Modifier) {
    val t by rememberInfiniteTransition(label = "rays")
        .animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing)),
            label = "rot"
        )

    Canvas(modifier = modifier.rotate(t)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r1 = size.minDimension * 0.22f
        val r2 = size.minDimension * 0.48f
        for (i in 0 until 12) {
            val a = Math.toRadians((i * 30).toDouble())
            val x1 = cx + cos(a).toFloat() * r1
            val y1 = cy + sin(a).toFloat() * r1
            val x2 = cx + cos(a).toFloat() * r2
            val y2 = cy + sin(a).toFloat() * r2
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 7f
            )
        }
    }
}

// --- LOCAL COPIES OF UTILS TO ENSURE COMPILATION ---

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
        modifier = modifier
            .scale(buttonScale)
            .let { if (size != null) it.size(size) else it },
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
private fun ConfettiBox(burstId: Long, modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
    val colors = listOf(Color(0xFFFFC107), Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFE91E63), Color(0xFFFF5722))
    val particles = remember { mutableStateListOf<ConfettiParticle>() }
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        LaunchedEffect(burstId) {
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
                                size = Size(24f, 16f)
                            )
                        }
                    }
                }
            }
        }
    }
}