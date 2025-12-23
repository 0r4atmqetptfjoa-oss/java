package com.example.educationalapp.features.games

import android.view.SoundEffectConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
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
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun ShadowMatchGameScreen(
    viewModel: ShadowMatchViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val density = LocalDensity.current
    val view = LocalView.current
    val context = LocalContext.current
    val sound = remember { AlphabetSoundPlayer(context) }

    val dropZones = remember { mutableStateMapOf<Int, Offset>() }
    var hoveredTargetId by remember { mutableStateOf<Int?>(null) }
    var matchBurstId by remember { mutableStateOf<Int?>(null) }

    val magnetRangePx = with(density) { 64.dp.toPx() } // Magnet mai puternic
    val snapRangePx = with(density) { 48.dp.toPx() }

    LaunchedEffect(uiState.lastMatchedId) {
        val id = uiState.lastMatchedId ?: return@LaunchedEffect
        matchBurstId = id
        delay(400)
        if (matchBurstId == id) matchBurstId = null
        viewModel.clearLastMatched()
    }

    LaunchedEffect(uiState.isLevelComplete) {
        if (uiState.isLevelComplete) {
            sound.playCorrect()
            delay(1200)
            viewModel.nextLevel()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background
        Image(
            painter = painterResource(R.drawable.bg_game_shapes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShadowSquishyButton(
                onClick = { sound.playClick(); onBack() },
                size = 56.dp
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ui_button_home),
                    contentDescription = "Home",
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Găsește Umbra",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Nivel ${uiState.level}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            // Placeholder dreapta pentru simetrie
            Spacer(Modifier.size(56.dp))
        }

        // Layout Principal
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TABLA CU UMBRE (Stânga - mai lată)
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Aranjăm umbrele într-un Grid flexibil
                val rows = uiState.targets.chunked(2)
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { item ->
                            ShadowTarget(
                                item = item,
                                isHovered = hoveredTargetId == item.id,
                                burst = matchBurstId == item.id,
                                modifier = Modifier
                                    .size(130.dp) // Mărime fixă generoasă
                                    .onGloballyPositioned { coords ->
                                        val pos = coords.positionInRoot()
                                        val center = Offset(
                                            x = pos.x + coords.size.width / 2f,
                                            y = pos.y + coords.size.height / 2f
                                        )
                                        dropZones[item.id] = center
                                    }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // TAVA CU PIESE (Dreapta - coloană)
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Animale",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                
                // Lista de piese de tras
                LazyGridLikeColumn(items = uiState.tray) { item ->
                    DraggablePiece(
                        item = item,
                        magnetRangePx = magnetRangePx,
                        snapRangePx = snapRangePx,
                        getTargetCenter = { dropZones[item.id] },
                        onHover = { hoveredTargetId = it },
                        onHoverClear = { if (hoveredTargetId == item.id) hoveredTargetId = null },
                        onDropSuccess = {
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            sound.playCorrect()
                            viewModel.onMatched(item.id)
                        },
                        onDropFail = { sound.playWrong() }
                    )
                }

                // Reset Button - Folosim icon_alphabet_replay care există sigur
                ShadowSquishyButton(
                    onClick = {
                        sound.playClick()
                        viewModel.resetGame()
                    },
                    size = 64.dp
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.icon_alphabet_replay),
                        contentDescription = "Reset",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        if (uiState.isLevelComplete) {
            ShadowConfettiBox(burstId = System.currentTimeMillis())
        }
    }
}

// Helper simplu pentru grid vertical
@Composable
fun LazyGridLikeColumn(
    items: List<ShadowMatchItem>,
    content: @Composable (ShadowMatchItem) -> Unit
) {
    // Afișăm câte 2 pe rând dacă sunt multe, sau una sub alta
    val chunks = items.chunked(2)
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        chunks.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { item ->
                    content(item)
                }
            }
        }
    }
}

@Composable
private fun ShadowTarget(
    item: ShadowMatchItem,
    isHovered: Boolean,
    burst: Boolean,
    modifier: Modifier = Modifier,
) {
    val burstScale by animateFloatAsState(if (burst) 1.2f else 1f, tween(300), label = "burst")
    
    // Pulsare ușoară pentru umbrele necompletate ca indiciu
    val pulse by rememberInfiniteTransition().animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1500), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .scale(if (item.isMatched) burstScale else pulse)
            .clip(RoundedCornerShape(16.dp))
            // Dacă e hover, punem un glow
            .background(if (isHovered) Color.Yellow.copy(alpha = 0.3f) else Color.Transparent)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. UMBRA (Fundal)
        Image(
            painter = painterResource(id = item.kind.shadowRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(Color.Black.copy(alpha = 0.6f)) // Întunecăm umbra pentru contrast
        )

        // 2. IMAGINEA COLORATĂ (Dacă e potrivită)
        if (item.isMatched) {
            Image(
                painter = painterResource(id = item.kind.imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            // Stea de succes
            Image(
                painter = painterResource(R.drawable.ui_icon_star),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
                    .offset(x = 8.dp, y = (-8).dp)
            )
        }
    }
}

@Composable
private fun DraggablePiece(
    item: ShadowMatchItem,
    magnetRangePx: Float,
    snapRangePx: Float,
    getTargetCenter: () -> Offset?,
    onHover: (Int) -> Unit,
    onHoverClear: () -> Unit,
    onDropSuccess: () -> Unit,
    onDropFail: () -> Unit,
) {
    var originCenter by remember { mutableStateOf<Offset?>(null) }
    var offset by remember(item.id) { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(if (dragging) 1.2f else 1f, tween(200), label = "lift")
    val rotation by animateFloatAsState(if (dragging) -10f else 0f, tween(200), label = "tilt")

    Box(
        modifier = Modifier
            .size(100.dp) // Mărimea piesei în tavă
            .zIndex(if (dragging) 100f else 1f)
            .onGloballyPositioned { coords ->
                if (!dragging) {
                    val pos = coords.positionInRoot()
                    originCenter = Offset(
                        x = pos.x + coords.size.width / 2f,
                        y = pos.y + coords.size.height / 2f
                    )
                }
            }
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            }
            .pointerInput(item.id) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = {
                        dragging = false
                        val target = getTargetCenter()
                        val origin = originCenter
                        // Verificăm distanța de fixare
                        if (target != null && origin != null && 
                            hypot(origin.x + offset.x - target.x, origin.y + offset.y - target.y) <= snapRangePx) {
                            offset = Offset.Zero
                            onHoverClear()
                            onDropSuccess()
                        } else {
                            // Revine în tavă
                            offset = Offset.Zero
                            onHoverClear()
                            onDropFail()
                        }
                    },
                    onDragCancel = { dragging = false; offset = Offset.Zero; onHoverClear() },
                    onDrag = { change, dragAmount ->
                        change.consumeAllChanges()
                        offset += dragAmount
                        
                        // Magnet effect visual
                        val target = getTargetCenter()
                        val origin = originCenter
                        if (target != null && origin != null) {
                            val currentPos = origin + offset
                            val dx = target.x - currentPos.x
                            val dy = target.y - currentPos.y
                            if (hypot(dx, dy) <= magnetRangePx) {
                                onHover(item.id)
                            } else {
                                onHoverClear()
                            }
                        }
                    }
                )
            }
    ) {
        // Imaginea piesei de mutat
        Image(
            painter = painterResource(id = item.kind.imageRes),
            contentDescription = item.kind.label,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

private operator fun Offset.plus(other: Offset) = Offset(x + other.x, y + other.y)

// --- COMPONENTE LOCALE ---

@Composable
private fun ShadowSquishyButton(
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
    val buttonScale by animateFloatAsState(if (isPressed) 0.86f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "btnScale")
    Surface(onClick = onClick, modifier = modifier.scale(buttonScale).let { if (size != null) it.size(size) else it }, shape = shape, color = color, shadowElevation = elevation, interactionSource = interactionSource) { Box(contentAlignment = Alignment.Center, content = content) }
}

private data class ShadowConfettiParticle(
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
private fun ShadowConfettiBox(burstId: Long, modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
    val colors = listOf(Color(0xFFFFC107), Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFE91E63), Color(0xFFFF5722))
    val particles = remember { mutableStateListOf<ShadowConfettiParticle>() }
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val startY = with(density) { -40.dp.toPx() } // Fix explicit
        val endYLimit = heightPx + with(density) { 120.dp.toPx() } // Fix explicit

        LaunchedEffect(burstId) {
            particles.clear()
            if (burstId > 0L) {
                repeat(80) { id ->
                    val startX = Random.nextFloat() * widthPx
                    particles.add(ShadowConfettiParticle(id, startX, startY, colors.random(), Random.nextFloat() * 0.4f + 0.6f, (Random.nextFloat() - 0.5f) * 260f, Random.nextFloat() * 360f, (Random.nextFloat() - 0.5f) * 220f, 720f + (Random.nextFloat() * 320f)))
                }
                var lastTime = withFrameNanos { it }
                while (isActive && particles.isNotEmpty()) {
                    withFrameNanos { now ->
                        val dt = (now - lastTime) / 1_000_000_000f; lastTime = now
                        val t = now / 1_000_000_000f
                        val newParticles = particles.map { p -> val sway = (sin((t * 4.8f + p.id).toDouble()) * 28.0).toFloat(); p.apply { x += (vx + sway) * dt; y += vy * dt; currentRotation += rotationSpeed * dt } }.filter { it.y < endYLimit }
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