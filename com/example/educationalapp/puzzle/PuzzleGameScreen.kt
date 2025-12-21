package com.example.educationalapp.puzzle

import android.graphics.Paint as AndroidPaint
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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

    var boardSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    var lastStarted by remember { mutableStateOf(IntSize.Zero) }

    // Start / restart when board size is known (robust to layout changes)
    LaunchedEffect(boardSize) {
        val w = boardSize.width.roundToInt()
        val h = boardSize.height.roundToInt()
        if (w > 0 && h > 0) {
            val key = IntSize(w, h)
            if (key != lastStarted) {
                lastStarted = key
                viewModel.startGame(context, boardSize.width, boardSize.height)
            }
        }
    }

    // Haptic + sound events (snap / completed)
    LaunchedEffect(uiState.event) {
        when (uiState.event) {
            is PuzzleEvent.Snap -> {
                // Subtle feedback
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

        // Background
        Image(
            painter = painterResource(id = R.drawable.bg_puzzle_table),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Row(modifier = Modifier.fillMaxSize()) {

            // LEFT: board + pieces
            BoxWithConstraints(
                modifier = Modifier.weight(0.78f).fillMaxHeight().padding(16.dp)
            ) {
                val parentPxW = with(density) { maxWidth.toPx() }
                val parentPxH = with(density) { maxHeight.toPx() }

                // Board top-left (inside this left container)
                val boardTopLeftX = (parentPxW - boardSize.width) / 2f
                val boardTopLeftY = (parentPxH - boardSize.height) / 2f

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                    // Board box (4:3)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 54.dp, vertical = 18.dp)
                            .aspectRatio(4f / 3f)
                            .background(Color.Black.copy(alpha = 0.18f))
                            .border(6.dp, Color(0xFF8D6E63), RoundedCornerShape(10.dp))
                            .onGloballyPositioned { coordinates ->
                                boardSize = androidx.compose.ui.geometry.Size(
                                    coordinates.size.width.toFloat(),
                                    coordinates.size.height.toFloat()
                                )
                            }
                    ) {
                        if (uiState.currentThemeResId != 0) {
                            Image(
                                painter = painterResource(id = uiState.currentThemeResId),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().alpha(0.25f),
                                contentScale = ContentScale.FillBounds
                            )
                        }

                        // Ghost targets (premium guide)
                        if (!uiState.isLoading && uiState.pieces.isNotEmpty()) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokePx = with(density) { 2.dp.toPx() }
                                drawIntoCanvas { canvas ->
                                    val nc = canvas.nativeCanvas
                                    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                                        style = AndroidPaint.Style.STROKE
                                        strokeWidth = strokePx
                                        color = 0x55FFFFFF
                                    }

                                    for (piece in uiState.pieces) {
                                        if (!piece.isLocked) {
                                            val path = PuzzleShape.createAndroidPath(
                                                config = piece.config,
                                                width = piece.width.toFloat(),
                                                height = piece.height.toFloat()
                                            )
                                            path.offset(piece.targetX, piece.targetY)
                                            nc.drawPath(path, paint)
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

                // Pieces (absolute in this left container; position = boardTopLeft + piece.current)
                val pieceWidthDp = with(density) { (boardSize.width / 4f).toDp() }
                val pieceHeightDp = with(density) { (boardSize.height / 4f).toDp() }

                uiState.pieces.forEach { piece ->
                    val magnetDist = uiState.magnetDistancePx
                    val d = hypot(piece.currentX - piece.targetX, piece.currentY - piece.targetY)
                    val isNear = (magnetDist > 0f && d < magnetDist && !piece.isLocked)

                    val scale by animateFloatAsState(
                        targetValue = if (isNear) 1.035f else 1.0f,
                        animationSpec = tween(durationMillis = 120)
                    )

                    val elevation by animateDpAsState(
                        targetValue = if (piece.isLocked) 2.dp else if (isNear) 14.dp else 10.dp,
                        animationSpec = tween(durationMillis = 120)
                    )

                    val absX = boardTopLeftX + piece.currentX
                    val absY = boardTopLeftY + piece.currentY

                    val z = if (piece.isLocked) 1f else (10_000 + piece.z + if (isNear) 5 else 0).toFloat()
                    val shape = PuzzleShape(piece.config)

                    Image(
                        bitmap = piece.bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .offset { IntOffset(absX.roundToInt(), absY.roundToInt()) }
                            .size(pieceWidthDp, pieceHeightDp)
                            .zIndex(z)
                            .shadow(elevation = elevation, shape = shape, clip = false)
                            .clip(shape)
                            .then(
                                if (isNear) Modifier.border(2.dp, Color(0xFFFFD54F), shape) else Modifier
                            )
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .pointerInput(piece.id, piece.isLocked) {
                                if (!piece.isLocked) {
                                    detectDragGestures(
                                        onDragStart = {
                                            viewModel.onPieceDragStart(piece.id)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consumeAllChanges()
                                            viewModel.onPieceDrag(piece.id, dragAmount.x, dragAmount.y)
                                        },
                                        onDragEnd = {
                                            viewModel.onPieceDrop(piece.id)
                                        },
                                        onDragCancel = {
                                            viewModel.onPieceDrop(piece.id)
                                        }
                                    )
                                }
                            }
                    )
                }

                // Completion overlay
                if (uiState.isComplete) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "BRAVO!",
                                color = Color.White,
                                fontSize = 54.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = {
                                    if (boardSize.width > 0f && boardSize.height > 0f) {
                                        viewModel.startGame(context, boardSize.width, boardSize.height)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                            ) {
                                Text(
                                    "Joc Nou",
                                    fontSize = 22.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // RIGHT: HUD (4x4 only)
            Column(
                modifier = Modifier
                    .weight(0.22f)
                    .fillMaxHeight()
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                    .border(2.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Text(
                    "PUZZLE 4x4",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                val remaining = uiState.pieces.count { !it.isLocked }
                Text(
                    "Piese rămase: $remaining / 16",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.92f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        if (boardSize.width > 0f && boardSize.height > 0f) {
                            viewModel.startGame(context, boardSize.width, boardSize.height)
                        }
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

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    "Trage piesele pe tablă.\nSe fixează automat când ești aproape.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.70f)
                )
            }
        }
    }
}