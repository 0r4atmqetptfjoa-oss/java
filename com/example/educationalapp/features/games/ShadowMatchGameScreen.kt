package com.example.educationalapp.features.games

import android.view.SoundEffectConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.educationalapp.R
import com.example.educationalapp.alphabet.AlphabetSoundPlayer
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

    val magnetRangePx = with(density) { 56.dp.toPx() }
    val snapRangePx = with(density) { 42.dp.toPx() }

    LaunchedEffect(uiState.lastMatchedId) {
        val id = uiState.lastMatchedId ?: return@LaunchedEffect
        matchBurstId = id
        kotlinx.coroutines.delay(350L)
        if (matchBurstId == id) matchBurstId = null
        viewModel.clearLastMatched()
    }

    LaunchedEffect(uiState.isLevelComplete) {
        if (uiState.isLevelComplete) {
            sound.playCorrect()
            kotlinx.coroutines.delay(900L)
            viewModel.nextLevel()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.bg_game_shapes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShadowSquishyButton(
                onClick = {
                    sound.playClick()
                    onBack()
                },
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
                    text = "Umbre 2026",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Text(
                    text = "Nivel ${uiState.level}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            IconButton(onClick = { sound.playClick(); onBack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 92.dp, start = 18.dp, end = 18.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(0.68f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .border(2.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Potrivește forma peste umbră",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                val rows = uiState.targets.chunked(2)
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { item ->
                            ShadowTarget(
                                item = item,
                                isHovered = hoveredTargetId == item.id,
                                burst = matchBurstId == item.id,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
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
                        if (row.size < 2) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(0.32f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(2.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(24.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Piese",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(10.dp))

                val trayRows = uiState.tray.chunked(2)
                trayRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { item ->
                            DraggablePiece(
                                item = item,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
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
                                onDropFail = {
                                    sound.playWrong()
                                }
                            )
                        }
                        if (row.size < 2) Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.weight(1f))

                ShadowSquishyButton(
                    onClick = {
                        sound.playClick()
                        viewModel.resetGame()
                    },
                    size = 56.dp
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ui_button_home),
                        contentDescription = "Reset",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        if (uiState.isLevelComplete) {
            ShadowConfettiBox(burstId = System.currentTimeMillis())
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
    val icon = remember(item.kind) { iconFor(item.kind) }
    val accent = remember(item.kind) { colorFor(item.kind) }

    val burstScale by animateFloatAsState(if (burst) 1.12f else 1f, tween(durationMillis = 260), label = "burst")

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .border(
                width = if (isHovered) 3.dp else 2.dp,
                color = if (isHovered) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.30f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, item.kind.label, tint = Color.Black.copy(alpha = 0.55f), modifier = Modifier.fillMaxSize(0.64f))
        if (item.isMatched) {
            Icon(icon, item.kind.label, tint = accent, modifier = Modifier.fillMaxSize(0.64f).graphicsLayer(scaleX = burstScale, scaleY = burstScale))
            Box(Modifier.size(18.dp).align(Alignment.TopEnd).background(Color.White.copy(alpha = 0.65f), CircleShape))
        }
    }
}

@Composable
private fun DraggablePiece(
    item: ShadowMatchItem,
    modifier: Modifier,
    magnetRangePx: Float,
    snapRangePx: Float,
    getTargetCenter: () -> Offset?,
    onHover: (Int) -> Unit,
    onHoverClear: () -> Unit,
    onDropSuccess: () -> Unit,
    onDropFail: () -> Unit,
) {
    val icon = remember(item.kind) { iconFor(item.kind) }
    val accent = remember(item.kind) { colorFor(item.kind) }
    var originCenter by remember { mutableStateOf<Offset?>(null) }
    var offset by remember(item.id) { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }
    val lift by animateFloatAsState(if (dragging) 1.06f else 1f, tween(durationMillis = 160), label = "lift")

    Box(
        modifier = modifier
            .zIndex(if (dragging) 10f else 0f)
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                originCenter = Offset(pos.x + coords.size.width / 2f, pos.y + coords.size.height / 2f)
            }
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .graphicsLayer(scaleX = lift, scaleY = lift)
            .pointerInput(item.id) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = {
                        dragging = false
                        val target = getTargetCenter()
                        val origin = originCenter
                        if (target != null && origin != null && hypot(origin.x + offset.x - target.x, origin.y + offset.y - target.y) <= snapRangePx) {
                            offset = Offset.Zero; onHoverClear(); onDropSuccess()
                        } else {
                            offset = Offset.Zero; onHoverClear(); onDropFail()
                        }
                    },
                    onDragCancel = { dragging = false; offset = Offset.Zero; onHoverClear() },
                    onDrag = { change, dragAmount ->
                        change.consumeAllChanges()
                        offset += dragAmount
                        val target = getTargetCenter()
                        val origin = originCenter
                        if (target != null && origin != null) {
                            val current = origin + offset
                            val dx = target.x - current.x; val dy = target.y - current.y
                            if (hypot(dx, dy) <= magnetRangePx) {
                                onHover(item.id)
                                offset += Offset(dx * 0.12f, dy * 0.12f)
                            } else {
                                onHoverClear()
                            }
                        }
                    }
                )
            }
            .clip(RoundedCornerShape(20.dp))
            .shadow(if (dragging) 14.dp else 8.dp, RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, item.kind.label, tint = accent, modifier = Modifier.fillMaxSize(0.70f))
    }
}

private fun iconFor(kind: ShadowKind) = when (kind) {
    ShadowKind.HEART -> Icons.Filled.Favorite
    ShadowKind.STAR -> Icons.Filled.Star
    ShadowKind.INFO -> Icons.Filled.Info
    ShadowKind.HOME -> Icons.Filled.Home
    ShadowKind.FACE -> Icons.Filled.Face
    ShadowKind.BUILD -> Icons.Filled.Build
}

private fun colorFor(kind: ShadowKind) = when (kind) {
    ShadowKind.HEART -> Color(0xFFE74C3C)
    ShadowKind.STAR -> Color(0xFFFFC107)
    ShadowKind.INFO -> Color(0xFF29B6F6)
    ShadowKind.HOME -> Color(0xFF66BB6A)
    ShadowKind.FACE -> Color(0xFFAB47BC)
    ShadowKind.BUILD -> Color(0xFFFF7043)
}

private operator fun Offset.plus(other: Offset) = Offset(x + other.x, y + other.y)

// --- UTILS LOCALE REDENUMITE PENTRU A EVITA CONFLICTELE ---

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

// REDENUMIT: ShadowConfettiParticle
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
        LaunchedEffect(burstId) {
            particles.clear()
            if (burstId > 0L) {
                repeat(80) { id ->
                    val startX = Random.nextFloat() * widthPx
                    val startY = -with(density) { 40.dp.toPx() }
                    particles.add(ShadowConfettiParticle(id, startX, startY, colors.random(), Random.nextFloat() * 0.4f + 0.6f, (Random.nextFloat() - 0.5f) * 260f, Random.nextFloat() * 360f, (Random.nextFloat() - 0.5f) * 220f, 720f + (Random.nextFloat() * 320f)))
                }
                var lastTime = withFrameNanos { it }
                while (isActive && particles.isNotEmpty()) {
                    withFrameNanos { now ->
                        val dt = (now - lastTime) / 1_000_000_000f; lastTime = now
                        val t = now / 1_000_000_000f
                        val newParticles = particles.map { p -> val sway = (sin((t * 4.8f + p.id).toDouble()) * 28.0).toFloat(); p.apply { x += (vx + sway) * dt; y += vy * dt; currentRotation += rotationSpeed * dt } }.filter { it.y < heightPx + with(density) { 120.dp.toPx() } }
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