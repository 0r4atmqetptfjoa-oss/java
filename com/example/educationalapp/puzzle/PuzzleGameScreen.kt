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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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

    var boardSize by remember { mutableStateOf(Size.Zero) }
    var boardOffsetInRoot by remember { mutableStateOf(Offset.Zero) }
    var lastStarted by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    // Start / restart when board size is known (robust to layout changes)
    LaunchedEffect(boardSize) {
        val w = boardSize.width.roundToInt()
        val h = boardSize.height.roundToInt()
        if (w > 0 && h > 0) {
            val key = androidx.compose.ui.unit.IntSize(w, h)
            if (key != lastStarted) {
                lastStarted = key
                viewModel.startGame(context, boardSize.width, boardSize.height)
            }
        }
    }

    // Menú overlay: visible at start, hidden after "Joc Nou", shown again when completed
    var menuVisible by remember { mutableStateOf(true) }
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) menuVisible = true
    }

    // Haptic + sound events (snap / completed)
    LaunchedEffect(uiState.event) {
        when (uiState.event) {
            is PuzzleEvent.Snap -> {
                try {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                } catch (_: Throwable) {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                }
                view.playSoundEffect(SoundEffectConstants.CLICK)
                viewModel.consumeEvent()
            }
            PuzzleEvent.Completed -> {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                view.playSoundEffect(SoundEffectConstants.NAVIGATION_UP)
                viewModel.consumeEvent()
            }
            null -> Unit
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

        // Layout: board area (left) + tray/menu area (right)
        Row(modifier = Modifier.fillMaxSize()) {

            // LEFT: board
            Box(
                modifier = Modifier
                    .weight(0.74f)
                    .fillMaxHeight()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .shadow(16.dp, RoundedCornerShape(14.dp), clip = false)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.10f))
                        .border(6.dp, Color(0xFF8D6E63), RoundedCornerShape(14.dp))
                        .onGloballyPositioned { coordinates ->
                            boardSize = Size(
                                coordinates.size.width.toFloat(),
                                coordinates.size.height.toFloat()
                            )
                            boardOffsetInRoot = coordinates.positionInRoot()
                        }
                ) {
                    // Hint image: slightly stronger + "shadow" duplicate
                    if (uiState.currentThemeResId != 0) {
                        // soft shadow pass
                        Image(
                            painter = painterResource(id = uiState.currentThemeResId),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .offset(2.dp, 2.dp)
                                .alpha(0.12f),
                            contentScale = ContentScale.FillBounds
                        )
                        // main hint
                        Image(
                            painter = painterResource(id = uiState.currentThemeResId),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.58f),
                            contentScale = ContentScale.FillBounds
                        )
                    }

                    // Ghost targets (premium guide)
                    if (!uiState.isLoading && uiState.pieces.isNotEmpty()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokePx = 3.dp.toPx()
                            val strokeColor = Color.White.copy(alpha = 0.78f)

                            for (piece in uiState.pieces) {
                                if (!piece.isLocked) {
                                    val shape = PuzzleShape(piece.config)
                                    val outline = shape.createOutline(
                                        size = Size(piece.width.toFloat(), piece.height.toFloat()),
                                        layoutDirection = layoutDirection,
                                        density = this
                                    )
                                    val path = (outline as? androidx.compose.ui.graphics.Outline.Generic)?.path
                                    if (path != null) {
                                        withTransform({
                                            translate(left = piece.targetX, top = piece.targetY)
                                        }) {
                                            drawPath(
                                                path = path,
                                                color = strokeColor,
                                                style = Stroke(width = strokePx)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(64.dp))
                }
            }

            // RIGHT: tray + menu overlay area (no permanent HUD during play)
            Box(
                modifier = Modifier
                    .weight(0.26f)
                    .fillMaxHeight()
                    .padding(start = 0.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                // Optional helper text (only when menu hidden)
                if (!menuVisible && !uiState.isComplete) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Trage piesele pe tablă.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Se fixează automat când ești aproape.",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }
                }

                if (menuVisible) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .background(Color.Black.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
                            .border(2.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "PUZZLE 4x4",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Piese rămase: ${uiState.pieces.count { !it.isLocked }} / ${uiState.pieces.size.coerceAtLeast(16)}",
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 13.sp
                        )

                        Button(
                            onClick = {
                                // Start overlay: hide menu; Completion: restart and hide.
                                if (uiState.isComplete && boardSize.width > 0f && boardSize.height > 0f) {
                                    viewModel.startGame(context, boardSize.width, boardSize.height)
                                }
                                menuVisible = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6BC0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Joc Nou", color = Color.White)
                        }

                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Înapoi", color = Color.White)
                        }
                    }
                }
            }
        }

        // Pieces layer (in root coords so tray is usable)
        if (!uiState.isLoading && boardSize.width > 0f && boardSize.height > 0f) {

            uiState.pieces.forEach { piece ->
                val magnetDist = uiState.magnetDistancePx
                val d = hypot(piece.currentX - piece.targetX, piece.currentY - piece.targetY)
                val isNear = (magnetDist > 0f && d < magnetDist && !piece.isLocked)

                val scale by animateFloatAsState(
                    targetValue = if (isNear) 1.03f else 1.0f,
                    animationSpec = tween(durationMillis = 120)
                )

                val elevation by animateDpAsState(
                    targetValue = if (piece.isLocked) 2.dp else if (isNear) 14.dp else 10.dp,
                    animationSpec = tween(durationMillis = 120)
                )

                val absX = boardOffsetInRoot.x + piece.currentX
                val absY = boardOffsetInRoot.y + piece.currentY

                val z = if (piece.isLocked) 1f else (10_000 + piece.z + if (isNear) 5 else 0).toFloat()
                val pieceShape = PuzzleShape(piece.config)

                val pieceWdp = with(density) { piece.width.toDp() }
                val pieceHdp = with(density) { piece.height.toDp() }

                Image(
                    bitmap = piece.bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .offset { IntOffset(absX.roundToInt(), absY.roundToInt()) }
                        .size(pieceWdp, pieceHdp)
                        .zIndex(z)
                        // Scale with pivot near "feet" to feel grounded
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 0.78f)
                            scaleX = scale
                            scaleY = scale
                        }
                        // Shape shadow + clip (works on older Compose)
                        .shadow(elevation, pieceShape, clip = true)
                        // Stronger outline like in reference
                        .border(3.dp, Color.White.copy(alpha = 0.82f), pieceShape)
                        .then(if (isNear) Modifier.border(2.dp, Color(0xFFFFD54F), pieceShape) else Modifier)
                        .pointerInput(piece.id, piece.isLocked) {
                            if (!piece.isLocked && !menuVisible) {
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
                )
            }
        }
    }
}
