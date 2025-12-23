package com.example.educationalapp.features.games

import android.view.SoundEffectConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.educationalapp.R
import com.example.educationalapp.alphabet.AlphabetSoundPlayer
import com.example.educationalapp.alphabet.ConfettiBox
import com.example.educationalapp.alphabet.SquishyButton
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun ShadowMatchGameScreen(
    viewModel: ShadowMatchViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val density = LocalDensity.current
    val view = LocalView.current
    val sound = remember { AlphabetSoundPlayer() }

    // Drop zones (centru in coordonate root)
    val dropZones = remember { mutableStateMapOf<Int, Offset>() }
    var hoveredTargetId by remember { mutableStateOf<Int?>(null) }

    // burst mic pe target cand se potriveste
    var matchBurstId by remember { mutableStateOf<Int?>(null) }

    // praguri (dp -> px)
    val magnetRangePx = with(density) { 56.dp.toPx() }
    val snapRangePx = with(density) { 42.dp.toPx() }

    LaunchedEffect(uiState.lastMatchedId) {
        val id = uiState.lastMatchedId ?: return@LaunchedEffect
        matchBurstId = id
        kotlinx.coroutines.delay(350L)
        if (matchBurstId == id) matchBurstId = null
        viewModel.clearLastMatched()
    }

    // Nivel complet -> urmatorul nivel automat
    LaunchedEffect(uiState.isLevelComplete) {
        if (uiState.isLevelComplete) {
            sound.playCorrect()
            kotlinx.coroutines.delay(900L)
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
            SquishyButton(
                imageRes = R.drawable.ui_button_home,
                size = 56.dp,
                onClick = {
                    sound.playClick()
                    onBack()
                }
            )

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

        // Layout: tabla stanga + tava dreapta
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 92.dp, start = 18.dp, end = 18.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // BOARD
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

            // TRAY
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

                // reset
                SquishyButton(
                    imageRes = R.drawable.ui_button_refresh,
                    size = 56.dp,
                    onClick = {
                        sound.playClick()
                        viewModel.resetGame()
                    }
                )
            }
        }

        // Confetti overlay la final
        if (uiState.isLevelComplete) {
            ConfettiBox(modifier = Modifier.fillMaxSize())
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

    val burstScale by animateFloatAsState(
        targetValue = if (burst) 1.12f else 1f,
        animationSpec = tween(durationMillis = 260),
        label = "burst"
    )

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
        // umbra (aceeasi iconita, tint negru)
        Icon(
            imageVector = icon,
            contentDescription = item.kind.label,
            tint = Color.Black.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxSize(0.64f)
        )

        // piesa potrivita
        if (item.isMatched) {
            Icon(
                imageVector = icon,
                contentDescription = item.kind.label,
                tint = accent,
                modifier = Modifier
                    .fillMaxSize(0.64f)
                    .graphicsLayer(scaleX = burstScale, scaleY = burstScale)
            )

            // mic highlight
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.TopEnd)
                    .background(Color.White.copy(alpha = 0.65f), CircleShape)
            )
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

    val lift by animateFloatAsState(
        targetValue = if (dragging) 1.06f else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "lift"
    )

    Box(
        modifier = modifier
            .zIndex(if (dragging) 10f else 0f)
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                originCenter = Offset(
                    x = pos.x + coords.size.width / 2f,
                    y = pos.y + coords.size.height / 2f
                )
            }
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .graphicsLayer(scaleX = lift, scaleY = lift)
            .pointerInput(item.id) {
                detectDragGestures(
                    onDragStart = {
                        dragging = true
                    },
                    onDragEnd = {
                        dragging = false
                        val target = getTargetCenter()
                        val origin = originCenter
                        if (target != null && origin != null) {
                            val current = origin + offset
                            val dist = hypot(current.x - target.x, current.y - target.y)
                            if (dist <= snapRangePx) {
                                offset = Offset.Zero
                                onHoverClear()
                                onDropSuccess()
                                return@detectDragGestures
                            }
                        }
                        // return to tray
                        offset = Offset.Zero
                        onHoverClear()
                        onDropFail()
                    },
                    onDragCancel = {
                        dragging = false
                        offset = Offset.Zero
                        onHoverClear()
                    },
                    onDrag = { change, dragAmount ->
                        change.consumeAllChanges()

                        // miscarea de baza
                        offset += dragAmount

                        val target = getTargetCenter()
                        val origin = originCenter
                        if (target != null && origin != null) {
                            val current = origin + offset
                            val dx = target.x - current.x
                            val dy = target.y - current.y
                            val dist = hypot(dx, dy)

                            if (dist <= magnetRangePx) {
                                onHover(item.id)
                                // micro-asistenta catre centru (simte elegant)
                                val assist = 0.12f
                                offset += Offset(dx * assist, dy * assist)
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
        Icon(
            imageVector = icon,
            contentDescription = item.kind.label,
            tint = accent,
            modifier = Modifier.fillMaxSize(0.70f)
        )
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
