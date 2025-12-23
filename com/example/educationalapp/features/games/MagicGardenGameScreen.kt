package com.example.educationalapp.features.games

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
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

@Composable
fun MagicGardenGameScreen(
    viewModel: MagicGardenViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val soundPlayer = remember { AlphabetSoundPlayer(context) }
    val scope = rememberCoroutineScope()

    var confettiBurstId by remember { mutableLongStateOf(0L) }
    var gardenCenter by remember { mutableStateOf(Offset.Zero) }
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

    MagicConfettiBox(burstId = confettiBurstId) {
        Box(modifier = Modifier.fillMaxSize()) {

            val bgBreath by rememberInfiniteTransition(label = "bg").animateFloat(
                initialValue = 1.02f, targetValue = 1.06f,
                animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing)), label = "bgScale"
            )

            Image(
                painter = painterResource(id = R.drawable.bg_sunny_meadow),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().scale(bgBreath),
                contentScale = ContentScale.Crop
            )

            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ui_button_home),
                    contentDescription = "Back",
                    modifier = Modifier.size(62.dp).pointerInput(Unit) { detectTapGestures { onBack() } }
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Recoltă: ${uiState.harvestCount}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Sun
            val sunAlpha by animateFloatAsState(if (uiState.isCloudMoved || uiState.stage == GardenStage.GROWN) 1f else 0.25f, tween(450), label = "sunAlpha")
            val sunPulse by rememberInfiniteTransition(label = "sun").animateFloat(1f, 1.06f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "sunPulse")

            Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 64.dp, end = 40.dp)) {
                Image(painter = painterResource(id = R.drawable.char_sun_happy), contentDescription = "Sun", modifier = Modifier.size(140.dp).alpha(sunAlpha).scale(sunPulse))
                if (uiState.isCloudMoved || uiState.stage == GardenStage.GROWN) {
                    SunRays(modifier = Modifier.matchParentSize().alpha(0.55f))
                }
            }

            // Cloud - Enabled only at SPROUT stage
            DraggableCloud(
                isEnabled = (uiState.stage == GardenStage.SPROUT),
                isMovedAway = uiState.isCloudMoved,
                onMovedAway = {
                    soundPlayer.playClick()
                    viewModel.onCloudMovedAway()
                }
            )

            // Garden Patch
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
                        gardenCenter = Offset(pos.x + it.size.width / 2f, pos.y + it.size.height / 2f)
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

            // Tools
            ToolShelf(
                currentTool = viewModel.currentTool(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
            )

            viewModel.currentTool()?.let { tool ->
                val toolRes = when (tool) {
                    ToolType.SHOVEL -> R.drawable.tool_shovel
                    ToolType.SEEDS -> R.drawable.tool_seed_bag
                    ToolType.WATER -> R.drawable.tool_watering_can
                }
                DraggableTool2026(
                    toolType = tool,
                    imageRes = toolRes,
                    patchCenter = gardenCenter,
                    stage = uiState.stage,
                    onWorkTick = { intensity ->
                        val d = (0.004f * intensity).coerceIn(0.001f, 0.03f)
                        viewModel.addActionProgress(d)
                    },
                    onMilestone = { at ->
                        spawnSparkle(at)
                        soundPlayer.playClick()
                    }
                )
            }

            if (uiState.stage == GardenStage.GROWN) {
                val pop by animateFloatAsState(1f, spring(dampingRatio = 0.6f), label = "pop")
                Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp).scale(pop)) {
                    Text("Atinge planta!", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }

            MagicSquishyButton(
                onClick = { soundPlayer.playClick(); viewModel.resetGame() },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 18.dp, end = 16.dp),
                size = 64.dp
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
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Corectat: Folosim DIRT în loc de GRASS
        val dirtRes = when (stage) {
            GardenStage.DIRT -> R.drawable.prop_dirt_flat
            GardenStage.DUG -> R.drawable.prop_dirt_hole
            else -> R.drawable.prop_dirt_flat
        }
        
        Image(painter = painterResource(id = dirtRes), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)

        when (stage) {
            GardenStage.DIRT -> {} // Empty
            GardenStage.DUG -> {} // Hole is in bg
            GardenStage.SEEDED -> {} // Seeds hidden
            // Corectat: Folosim SPROUT în loc de WATERED
            GardenStage.SPROUT -> {
                Image(painter = painterResource(id = R.drawable.prop_plant_sprout), contentDescription = "Sprout", modifier = Modifier.size(130.dp).offset(y = (-10).dp))
            }
            GardenStage.GROWN -> {
                val scale by animateFloatAsState(if (isCelebrating) 1.25f else 1.15f, spring(dampingRatio = 0.55f), label = "plantScale")
                Image(painter = painterResource(id = plantRes), contentDescription = "Plant", modifier = Modifier.size(260.dp).scale(scale).offset(y = (-20).dp))
            }
        }

        // Corectat: Verificare DIRT/DUG/SEEDED
        if (stage == GardenStage.DIRT || stage == GardenStage.DUG || stage == GardenStage.SEEDED) {
            val label = when (stage) {
                GardenStage.DIRT -> "SAPĂ"
                GardenStage.DUG -> "SEMINȚE"
                GardenStage.SEEDED -> "APĂ"
                else -> ""
            }
            ProgressRing(progress = progress, label = label)
        }
        SparkleLayer(sparkles = sparkles)
    }
}

@Composable
private fun ProgressRing(progress: Float, label: String) {
    Canvas(modifier = Modifier.fillMaxSize().padding(28.dp)) {
        val stroke = Stroke(width = 12f)
        drawArc(Color.White.copy(alpha = 0.3f), -90f, 360f, false, style = stroke)
        drawArc(Color(0xFF8BC34A), -90f, 360f * progress.coerceIn(0f, 1f), false, style = stroke)
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Text(text = label, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(bottom = 20.dp))
    }
}

@Composable
private fun SparkleLayer(sparkles: List<Sparkle>) {
    if (sparkles.isEmpty()) return
    Canvas(modifier = Modifier.fillMaxSize()) {
        for (s in sparkles) {
            val p = s.pos
            val cx = size.width / 2f
            val cy = size.height / 2f
            val x = cx + (p.x * 0.08f)
            val y = cy + (p.y * 0.08f)
            drawCircle(Color.Yellow, radius = 6f, center = Offset(x, y))
        }
    }
}

@Composable
private fun ToolShelf(currentTool: ToolType?, modifier: Modifier = Modifier) {
    val alphaInactive = 0.4f
    val alphaActive = 1f
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        ToolIcon(R.drawable.tool_shovel, "Sapă", currentTool == ToolType.SHOVEL, alphaInactive, alphaActive)
        Spacer(Modifier.width(22.dp))
        ToolIcon(R.drawable.tool_seed_bag, "Semințe", currentTool == ToolType.SEEDS, alphaInactive, alphaActive)
        Spacer(Modifier.width(22.dp))
        ToolIcon(R.drawable.tool_watering_can, "Apă", currentTool == ToolType.WATER, alphaInactive, alphaActive)
    }
}

@Composable
private fun ToolIcon(res: Int, label: String, active: Boolean, alphaInactive: Float, alphaActive: Float) {
    val a by animateFloatAsState(if (active) alphaActive else alphaInactive, tween(250), label = "toolAlpha")
    val s by animateFloatAsState(if (active) 1.1f else 1f, tween(250), label = "toolScale")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painter = painterResource(id = res), contentDescription = label, modifier = Modifier.size(70.dp).scale(s).alpha(a))
    }
}

@Composable
private fun DraggableTool2026(toolType: ToolType, imageRes: Int, patchCenter: Offset, stage: GardenStage, onWorkTick: (Float) -> Unit, onMilestone: (Offset) -> Unit) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var initialCenter by remember { mutableStateOf(Offset.Zero) }
    val rotation by animateFloatAsState(if (isDragging) -15f else 0f, tween(160), label = "rot")
    val scale by animateFloatAsState(if (isDragging) 1.2f else 1f, spring(dampingRatio = 0.65f), label = "scale")

    Box(
        modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.BottomCenter).padding(bottom = 88.dp)
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(120.dp).zIndex(if (isDragging) 20f else 6f)
            .onGloballyPositioned { if (initialCenter == Offset.Zero) { val pos = it.positionInRoot(); initialCenter = Offset(pos.x + it.size.width / 2f, pos.y + it.size.height / 2f) } }
            .pointerInput(toolType, stage) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false; offsetX = 0f; offsetY = 0f },
                    onDragCancel = { isDragging = false; offsetX = 0f; offsetY = 0f },
                    onDrag = { _, dragAmount ->
                        offsetX += dragAmount.x; offsetY += dragAmount.y
                        if (patchCenter != Offset.Zero && initialCenter != Offset.Zero) {
                            val cur = initialCenter + Offset(offsetX, offsetY)
                            val dist = kotlin.math.sqrt(kotlin.math.pow((cur.x - patchCenter.x).toDouble(), 2.0) + kotlin.math.pow((cur.y - patchCenter.y).toDouble(), 2.0)).toFloat()
                            if (dist < 200f) {
                                val intensity = (abs(dragAmount.x) + abs(dragAmount.y)).coerceIn(1f, 40f)
                                onWorkTick(intensity)
                                if (intensity > 20f) onMilestone(Offset(cur.x - patchCenter.x, cur.y - patchCenter.y))
                            }
                        }
                    }
                )
            }
    ) {
        Image(painter = painterResource(id = imageRes), contentDescription = null, modifier = Modifier.fillMaxSize().scale(scale).rotate(rotation))
    }
}

@Composable
private fun DraggableCloud(isEnabled: Boolean, isMovedAway: Boolean, onMovedAway: () -> Unit) {
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(if (isMovedAway) -450f else offsetX, tween(900), label = "cloudX")
    if (isMovedAway && animatedOffsetX <= -400f) return

    Box(modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.TopCenter).padding(top = 56.dp).offset { IntOffset(animatedOffsetX.roundToInt(), 0) }.size(180.dp).zIndex(5f)
        .pointerInput(isEnabled) {
            if (isEnabled) detectDragGestures(onDragEnd = { if (offsetX < -100f) onMovedAway() else offsetX = 0f }, onDragCancel = { offsetX = 0f }, onDrag = { _, dragAmount -> val newX = offsetX + dragAmount.x; if (newX <= 0f) offsetX = newX })
        }
    ) {
        Image(painter = painterResource(id = R.drawable.char_cloud_happy), contentDescription = "Cloud", modifier = Modifier.fillMaxSize())
        if (isEnabled && offsetX == 0f) Text("⬅", fontSize = 36.sp, color = Color.White, modifier = Modifier.align(Alignment.CenterStart).offset(x = (-30).dp))
    }
}

@Composable
private fun SunRays(modifier: Modifier = Modifier) {
    val t by rememberInfiniteTransition(label = "rays").animateFloat(0f, 360f, infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "rot")
    Canvas(modifier = modifier.rotate(t)) {
        val cx = size.width / 2f; val cy = size.height / 2f
        val r1 = size.minDimension * 0.22f; val r2 = size.minDimension * 0.48f
        for (i in 0 until 12) {
            val a = Math.toRadians((i * 30).toDouble())
            drawLine(Color.White.copy(alpha = 0.5f), Offset(cx + cos(a).toFloat() * r1, cy + sin(a).toFloat() * r1), Offset(cx + cos(a).toFloat() * r2, cy + sin(a).toFloat() * r2), 7f)
        }
    }
}

@Composable
private fun MagicSquishyButton(onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp? = null, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp), color: Color = Color.White, elevation: Dp = 4.dp, content: @Composable BoxScope.() -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(if (isPressed) 0.86f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "btnScale")
    Surface(onClick = onClick, modifier = modifier.scale(buttonScale).let { if (size != null) it.size(size) else it }, shape = shape, color = color, shadowElevation = elevation, interactionSource = interactionSource) { Box(contentAlignment = Alignment.Center, content = content) }
}

private data class MagicConfettiParticle(val id: Int, var x: Float, var y: Float, val color: Color, val scale: Float, val rotationSpeed: Float, var currentRotation: Float, var vx: Float, var vy: Float)

@Composable
private fun MagicConfettiBox(burstId: Long, modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
    val colors = listOf(Color(0xFFFFC107), Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFE91E63), Color(0xFFFF5722))
    val particles = remember { mutableStateListOf<MagicConfettiParticle>() }
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val startY = with(density) { -40.dp.toPx() }
        val endYLimit = heightPx + with(density) { 120.dp.toPx() }
        LaunchedEffect(burstId) {
            particles.clear()
            if (burstId > 0L) {
                repeat(80) { id -> particles.add(MagicConfettiParticle(id, Random.nextFloat() * widthPx, startY, colors.random(), Random.nextFloat() * 0.4f + 0.6f, (Random.nextFloat() - 0.5f) * 260f, Random.nextFloat() * 360f, (Random.nextFloat() - 0.5f) * 220f, 720f + (Random.nextFloat() * 320f))) }
                var lastTime = withFrameNanos { it }
                while (isActive && particles.isNotEmpty()) {
                    withFrameNanos { now ->
                        val dt = (now - lastTime) / 1_000_000_000f; lastTime = now
                        val newParticles = particles.map { p -> val sway = (sin((now / 1e9 * 4.8 + p.id).toDouble()) * 28.0).toFloat(); p.apply { x += (vx + sway) * dt; y += vy * dt; currentRotation += rotationSpeed * dt } }.filter { it.y < endYLimit }
                        particles.clear(); particles.addAll(newParticles)
                    }
                }
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            if (particles.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize().zIndex(999f)) {
                    particles.forEach { p -> withTransform({ translate(p.x, p.y); rotate(p.currentRotation); scale(p.scale, p.scale) }) { drawRect(p.color, Offset(-12f, -8f), Size(24f, 16f)) } }
                }
            }
        }
    }
}