package com.example.educationalapp.puzzle

import android.view.SoundEffectConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.educationalapp.R
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun PuzzleGameScreen(
    viewModel: PuzzleViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val layoutDirection = LocalLayoutDirection.current

    // Premium: “snap pop” pulse on the last snapped piece
    var snapPulseId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(uiState.event) {
        when (uiState.event) {
            PuzzleEvent.Snap -> {
                // subtle haptic + click
                runCatching { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove) }
                runCatching { view.playSoundEffect(SoundEffectConstants.CLICK) }

                // pulse last snapped (if provided)
                val id = uiState.lastSnapId
                if (id != null) {
                    snapPulseId = id
                    // short pulse window
                    kotlinx.coroutines.delay(180)
                    snapPulseId = null
                }
                viewModel.consumeEvent()
            }
            PuzzleEvent.Complete -> {
                runCatching { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress) }
                runCatching { view.playSoundEffect(SoundEffectConstants.CLICK) }
                viewModel.consumeEvent()
            }
            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Background (table)
        Image(
            painter = painterResource(id = R.drawable.bg_puzzle_table),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val maxWpx = with(density) { maxWidth.toPx() }
            val maxHpx = with(density) { maxHeight.toPx() }
            val gapPx = with(density) { 16.dp.toPx() }

            // Layout: board + tray in the same coordinate space (origin = board top-left)
            val minTrayPx = with(density) { 200.dp.toPx() }
            val boardPx = run {
                // keep board large but ensure we still have room for a tray on the right
                val candidate = (maxWpx - gapPx - minTrayPx).coerceAtLeast(maxWpx * 0.60f)
                minOf(candidate, maxHpx * 0.92f)
            }

            val trayPxW = (maxWpx - boardPx - gapPx).coerceAtLeast(minTrayPx)
            val trayPxH = boardPx
            val boardTopLeft = Offset(0f, ((maxHpx - boardPx) / 2f).coerceAtLeast(0f))
            val trayTopLeft = Offset(boardPx + gapPx, boardTopLeft.y)

            val boardDp = with(density) { boardPx.toDp() }
            val trayDpW = with(density) { trayPxW.toDp() }
            val trayDpH = with(density) { trayPxH.toDp() }

            // Start game once the layout is known AND user presses Start (phase = START)
            // NOTE: Start is triggered from the overlay button, not automatically.

            // Precompute paths for ghost targets (only for pieces not locked)
            val piecePaths: Map<Int, Path> = remember(uiState.pieces) {
                uiState.pieces.associate { piece ->
                    val shape = PuzzleShape(piece.config)
                    val outline = shape.createOutline(
                        size = Size(piece.width.toFloat(), piece.height.toFloat()),
                        layoutDirection = layoutDirection,
                        density = density
                    )
                    val p = when (outline) {
                        is androidx.compose.ui.graphics.Outline.Generic -> outline.path
                        else -> Path()
                    }
                    piece.id to p
                }
            }

            // BOARD + TRAY containers (visual only; pieces are drawn absolutely on top)
            Row(modifier = Modifier.fillMaxSize()) {

                // Board area
                Box(
                    modifier = Modifier
                        .offset { IntOffset(boardTopLeft.x.roundToInt(), boardTopLeft.y.roundToInt()) }
                        .size(boardDp)
                        .shadow(12.dp, RoundedCornerShape(16.dp), clip = false)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF101018).copy(alpha = 0.25f))
                        .border(2.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                ) {
                    // “Washed out” hint image: faux-blur + desaturate + low alpha
                    val cm = remember {
                        ColorMatrix().apply { setToSaturation(0.20f) }
                    }
                    val cf = remember { ColorFilter.colorMatrix(cm) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Faux blur passes
                        val blurOffsets = listOf(
                            Offset(0f, 0f),
                            Offset(1f, 0f),
                            Offset(-1f, 0f),
                            Offset(0f, 1f),
                            Offset(0f, -1f)
                        )
                        blurOffsets.forEachIndexed { i, o ->
                            Image(
                                painter = painterResource(id = uiState.currentThemeResId),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset(o.x.dp, o.y.dp)
                                    .alpha(if (i == 0) 0.16f else 0.05f),
                                contentScale = ContentScale.FillBounds,
                                colorFilter = cf
                            )
                        }

                        // Slight white wash so the pieces pop
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.06f))
                        )

                        // Grid lines (always visible, subtle)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = Stroke(width = with(density) { 1.2.dp.toPx() })
                            val w = size.width
                            val h = size.height
                            for (i in 1 until 4) {
                                val x = w * (i / 4f)
                                val y = h * (i / 4f)
                                drawLine(
                                    color = Color.White.copy(alpha = 0.12f),
                                    start = Offset(x, 0f),
                                    end = Offset(x, h),
                                    strokeWidth = stroke.width
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = 0.12f),
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = stroke.width
                                )
                            }
                            // board inner stroke
                            drawRect(
                                color = Color.White.copy(alpha = 0.10f),
                                style = Stroke(width = with(density) { 2.dp.toPx() })
                            )
                        }

                        // Ghost targets (after start, only for missing pieces)
                        if (uiState.phase != PuzzlePhase.START && uiState.pieces.isNotEmpty()) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokePx = with(density) { 2.dp.toPx() }
                                uiState.pieces.forEach { piece ->
                                    if (!piece.isLocked) {
                                        val p = piecePaths[piece.id] ?: return@forEach
                                        // translate to target position (board-local coords)
                                        withTransform({
                                            translate(left = piece.targetX, top = piece.targetY)
                                        }) {
                                            drawPath(
                                                path = p,
                                                color = Color.White.copy(alpha = 0.35f),
                                                style = Stroke(width = strokePx)
                                            )
                                            drawPath(
                                                path = p,
                                                color = Color.Black.copy(alpha = 0.10f),
                                                style = Stroke(width = with(density) { 4.dp.toPx() })
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // START overlay (Version A: centered on board)
                    if (uiState.phase == PuzzlePhase.START) {
                        BoardOverlayCard(
                            title = "PUZZLE 4x4",
                            subtitle = "Trage piesele din dreapta pe tablă.\nSe fixează automat când ești aproape.",
                            primaryText = "Start",
                            secondaryText = "Înapoi",
                            onPrimary = {
                                viewModel.startGame(
                                    context = context,
                                    boardWidth = boardPx,
                                    boardHeight = boardPx,
                                    trayX = trayTopLeft.x - boardTopLeft.x,
                                    trayY = trayTopLeft.y - boardTopLeft.y,
                                    trayW = trayPxW,
                                    trayH = trayPxH
                                )
                            },
                            onSecondary = onBack
                        )
                    }

                    // COMPLETION overlay (centered on board)
                    if (uiState.phase == PuzzlePhase.COMPLETE) {
                        BoardOverlayCard(
                            title = "BRAVO!",
                            subtitle = "Ai terminat puzzle-ul.",
                            primaryText = "Joc Nou",
                            secondaryText = "Înapoi",
                            onPrimary = {
                                viewModel.restartLast(context)
                            },
                            onSecondary = onBack
                        )
                    }

                    if (uiState.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(52.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(with(density) { gapPx.toDp() }))

                // Tray area (visual container; pieces are still drawn absolutely)
                Box(
                    modifier = Modifier
                        .offset { IntOffset(trayTopLeft.x.roundToInt(), trayTopLeft.y.roundToInt()) }
                        .size(trayDpW, trayDpH)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.12f))
                        .border(2.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    // Minimal HUD inside tray (only while playing)
                    if (uiState.phase == PuzzlePhase.PLAYING) {
                        val remaining = uiState.pieces.count { !it.isLocked }
                        Text(
                            text = "Piese rămase: $remaining/16",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Pieces layer (absolute in the whole BoxWithConstraints)
            val trayOriginX = trayTopLeft.x - boardTopLeft.x
            val trayOriginY = trayTopLeft.y - boardTopLeft.y

            // Extra premium: tray intro animation
            val trayProgress by animateFloatAsState(
                targetValue = if (uiState.phase == PuzzlePhase.PLAYING && !uiState.isLoading) 1f else 0f,
                animationSpec = tween(durationMillis = 260)
            )

            uiState.pieces.forEach { piece ->
                val isDragging = (uiState.draggingId == piece.id)
                val isSnappingPulse = (snapPulseId == piece.id)

                val d = hypot(piece.currentX - piece.targetX, piece.currentY - piece.targetY)
                val isNear = (!piece.isLocked && uiState.magnetDistancePx > 0f && d < uiState.magnetDistancePx)

                // Premium “lift” during drag; subtle rotation
                val liftScaleTarget = when {
                    isDragging -> 1.10f
                    isSnappingPulse -> 1.10f
                    isNear -> 1.04f
                    else -> 1.0f
                }
                val scale by animateFloatAsState(liftScaleTarget, tween(110))

                val rotTarget = if (isDragging) 1.8f else 0f
                val rotation by animateFloatAsState(rotTarget, tween(140))

                val elevation by animateDpAsState(
                    targetValue = when {
                        piece.isLocked -> 2.dp
                        isDragging -> 20.dp
                        isNear -> 16.dp
                        else -> 10.dp
                    },
                    animationSpec = tween(110)
                )

                // Optional smoothing: animate when NOT dragging
                val animSpec = if (isDragging) tween(durationMillis = 0) else tween(durationMillis = 90)
                val animX by animateFloatAsState(piece.currentX, animSpec)
                val animY by animateFloatAsState(piece.currentY, animSpec)

                // If still in tray at start, apply tray intro slide a bit
                val isInTray = (animX >= trayOriginX - 8f && animX <= trayOriginX + trayPxW + 8f && animY >= trayOriginY - 8f && animY <= trayOriginY + trayPxH + 8f)
                val introShift = if (isInTray) (1f - trayProgress) * with(density) { 14.dp.toPx() } else 0f

                val absX = boardTopLeft.x + animX + introShift
                val absY = boardTopLeft.y + animY

                val pieceWdp = with(density) { piece.width.toDp() }
                val pieceHdp = with(density) { piece.height.toDp() }

                val z = if (piece.isLocked) 1f else (10_000 + piece.z).toFloat()

                val shape = PuzzleShape(piece.config)

                Image(
                    bitmap = piece.bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .offset { IntOffset(absX.roundToInt(), absY.roundToInt()) }
                        .size(pieceWdp, pieceHdp)
                        .zIndex(z)
                        .shadow(elevation = elevation, shape = shape, clip = false)
                        .clip(shape)
                        .then(if (isNear) Modifier.border(2.dp, Color(0xFFFFD54F), shape) else Modifier)
                        .pointerInput(piece.id, piece.isLocked, uiState.phase) {
                            if (!piece.isLocked && uiState.phase == PuzzlePhase.PLAYING) {
                                detectDragGestures(
                                    onDragStart = { viewModel.onPieceDragStart(piece.id) },
                                    onDrag = { change, dragAmount ->
                                        change.consumeAllChanges()
                                        viewModel.onPieceDrag(piece.id, dragAmount.x, dragAmount.y)
                                    },
                                    onDragEnd = { viewModel.onPieceDrop(piece.id) },
                                    onDragCancel = { viewModel.onPieceDrop(piece.id) }
                                )
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            rotationZ = rotation
                        }
                )
            }
        }
    }
}

@Composable
private fun BoardOverlayCard(
    title: String,
    subtitle: String,
    primaryText: String,
    secondaryText: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .border(2.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                .padding(18.dp)
                .widthIn(min = 240.dp, max = 380.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(10.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.88f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onPrimary,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6BC0)),
                modifier = Modifier.fillMaxWidth()
            ) { Text(primaryText, color = Color.White, fontWeight = FontWeight.Bold) }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onSecondary,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                modifier = Modifier.fillMaxWidth()
            ) { Text(secondaryText, color = Color.White, fontWeight = FontWeight.Bold) }
        }
    }
}