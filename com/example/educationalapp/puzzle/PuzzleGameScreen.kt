package com.example.educationalapp.puzzle

import android.view.SoundEffectConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.consume
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.educationalapp.R
import kotlin.math.abs
import kotlin.math.min
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
    var boardOffset by remember { mutableStateOf(Offset.Zero) }

    var restartToken by remember { mutableIntStateOf(0) }
    var draggingId by remember { mutableIntStateOf(-1) }

    // Cerință: 4x4 fixed.
    val grid = 4

    // Start/restart game when we have a measured board size.
    LaunchedEffect(boardSize, restartToken) {
        if (boardSize != Size.Zero) {
            viewModel.startGame(
                context = context,
                boardWidth = boardSize.width,
                boardHeight = boardSize.height
            )
        }
    }

    // Haptic + sound events (snap / victory) by detecting state transitions.
    val lockedIds = remember(uiState.pieces) { uiState.pieces.filter { it.isLocked }.map { it.id }.toSet() }
    var prevLockedIds by remember { mutableStateOf(emptySet<Int>()) }

    LaunchedEffect(lockedIds) {
        val newlyLocked = lockedIds - prevLockedIds
        if (newlyLocked.isNotEmpty()) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            view.playSoundEffect(SoundEffectConstants.CLICK)
        }
        prevLockedIds = lockedIds
    }

    var prevComplete by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete && !prevComplete) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            view.playSoundEffect(SoundEffectConstants.NAVIGATION_UP)
        }
        prevComplete = uiState.isComplete
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(id = R.drawable.bg_puzzle_table),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Back button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(60.dp)
                .zIndex(200f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ui_btn_home),
                contentDescription = "Back",
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onBack() }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 90.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            // Board (left)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0x33000000))
                    .border(2.dp, Color.White.copy(alpha = 0.70f))
                    .onGloballyPositioned { coords ->
                        val size = coords.size
                        boardSize = Size(size.width.toFloat(), size.height.toFloat())
                        boardOffset = coords.localToRoot(Offset.Zero)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (uiState.currentThemeResId != 0) {
                    Image(
                        painter = painterResource(id = uiState.currentThemeResId),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().alpha(0.28f),
                        contentScale = ContentScale.Crop
                    )
                }

                // Optional: faint grid hint
                if (boardSize != Size.Zero) {
                    val alphaGrid = 0.05f
                    val cellW = boardSize.width / grid
                    val cellH = boardSize.height / grid
                    Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                        repeat(grid) {
                            Row(modifier = Modifier.weight(1f)) {
                                repeat(grid) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .border(1.dp, Color.White.copy(alpha = alphaGrid))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // HUD / tray (right)
            val remaining = uiState.pieces.count { !it.isLocked }
            Column(
                modifier = Modifier
                    .widthIn(min = 210.dp, max = 270.dp)
                    .fillMaxHeight()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(1.dp, Color.White.copy(alpha = 0.35f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "PUZZLE ${grid}x$grid",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "Piese rămase: $remaining",
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Button(
                    onClick = { restartToken++ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Joc Nou",
                        fontSize = 16.sp,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Text(
                    text = "Tip: când ești aproape de locul corect, piesa este " +
                        ""magnetizată" și se evidențiază.",
                    color = Color.White.copy(alpha = 0.80f),
                    fontSize = 12.sp
                )
            }
        }

        // Ghost targets (behind pieces): arată forma și poziția unde trebuie pusă piesa.
        uiState.pieces.forEach { piece ->
            if (!piece.isLocked && boardSize != Size.Zero) {
                val absoluteX = boardOffset.x + piece.targetX
                val absoluteY = boardOffset.y + piece.targetY
                val shape = remember(piece.config) { PuzzleShape(piece.config) }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(absoluteX.roundToInt(), absoluteY.roundToInt()) }
                        .size(
                            with(density) { piece.width.toDp() },
                            with(density) { piece.height.toDp() }
                        )
                        .graphicsLayer {
                            this.shape = shape
                            clip = true
                        }
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
                        .zIndex(1f)
                )
            }
        }

        // Pieces overlay - positioned relative to the board origin.
        uiState.pieces.forEach { piece ->
            key(piece.id) {
                val absoluteX = boardOffset.x + piece.currentX
                val absoluteY = boardOffset.y + piece.currentY

                val shape = remember(piece.config) { PuzzleShape(piece.config) }

                val isDragging = draggingId == piece.id

                val elevationDp by animateDpAsState(
                    targetValue = when {
                        piece.isLocked -> 0.dp
                        isDragging -> 18.dp
                        else -> 12.dp
                    },
                    label = "piece_elevation"
                )

                // Snap animation only on lock.
                val targetOffset = IntOffset(absoluteX.roundToInt(), absoluteY.roundToInt())
                val animatedOffset by animateIntOffsetAsState(
                    targetValue = targetOffset,
                    animationSpec = if (piece.isLocked) tween(durationMillis = 160) else snap(),
                    label = "piece_offset"
                )

                // Highlight when near its target.
                val hintDistance = (min(piece.width, piece.height) * 0.22f).coerceIn(34f, 160f)
                val isNearTarget = !piece.isLocked &&
                    abs(piece.currentX - piece.targetX) < hintDistance &&
                    abs(piece.currentY - piece.targetY) < hintDistance

                val borderColor = when {
                    piece.isLocked -> Color.Transparent
                    isNearTarget -> Color(0xFFFFD54F).copy(alpha = 0.92f)
                    else -> Color.Transparent
                }

                val scale by animateFloatAsState(
                    targetValue = when {
                        piece.isLocked -> 1.0f
                        isDragging -> 1.06f
                        isNearTarget -> 1.03f
                        else -> 1.0f
                    },
                    animationSpec = tween(durationMillis = 90),
                    label = "piece_scale"
                )

                Image(
                    bitmap = piece.bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .offset { animatedOffset }
                        .zIndex(
                            when {
                                piece.isLocked -> 10f
                                isDragging -> 999f
                                else -> 40f
                            }
                        )
                        .size(
                            with(density) { piece.width.toDp() },
                            with(density) { piece.height.toDp() }
                        )
                        .graphicsLayer {
                            shadowElevation = with(density) { elevationDp.toPx() }
                            this.shape = shape
                            clip = true
                            scaleX = scale
                            scaleY = scale
                        }
                        .border(2.dp, borderColor, shape)
                        .pointerInput(piece.id) {
                            detectDragGestures(
                                onDragStart = {
                                    if (!piece.isLocked) {
                                        draggingId = piece.id
                                        viewModel.bringToFront(piece.id)
                                    }
                                },
                                onDragCancel = { draggingId = -1 },
                                onDragEnd = {
                                    draggingId = -1
                                    viewModel.onPieceDrop(piece.id)
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                viewModel.onPieceDrag(piece.id, dragAmount.x, dragAmount.y)
                            }
                        }
                )
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .zIndex(500f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        if (uiState.isComplete) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.80f))
                    .clickable { }
                    .zIndex(600f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "BRAVO!",
                        color = Color(0xFF00E676),
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { restartToken++ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                    ) {
                        Text(
                            "Joc Nou",
                            fontSize = 22.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
