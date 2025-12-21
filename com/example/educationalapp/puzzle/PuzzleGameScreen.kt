package com.example.educationalapp.puzzle

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.educationalapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.math.sqrt

// Asigura-te ca ai acest fisier in pachetul 'com.example.educationalapp.puzzle'

@Composable
fun PuzzleGameScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Dimensiuni piesa
    val pieceSizeDp = 150.dp
    val pieceSizePx = with(density) { pieceSizeDp.toPx() }

    // State
    var puzzleBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pieces by remember { mutableStateOf<List<PuzzlePieceState>>(emptyList()) }
    var slots by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var rootPos by remember { mutableStateOf(Offset.Zero) }

    // Load Image (Critical Fix: inScaled = false)
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val opts = BitmapFactory.Options().apply { inScaled = false }
            val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.puzzle_image_full, opts)
            puzzleBitmap = bmp
        }
    }

    // Initialize Pieces ONLY when bitmap is ready
    LaunchedEffect(puzzleBitmap) {
        val bmp = puzzleBitmap ?: return@LaunchedEffect
        // Split 2x2
        val rows = 2
        val cols = 2
        val chunkW = bmp.width / cols
        val chunkH = bmp.height / rows
        
        val newPieces = mutableListOf<PuzzlePieceState>()
        var id = 0
        for(r in 0 until rows) {
            for(c in 0 until cols) {
                val pieceBmp = Bitmap.createBitmap(bmp, c * chunkW, r * chunkH, chunkW, chunkH)
                newPieces.add(
                    PuzzlePieceState(
                        id = id,
                        bitmap = pieceBmp,
                        correctRow = r,
                        correctCol = c,
                        initialX = 50f + (c * 20f), // Random scatter positions
                        initialY = 400f + (r * 20f)
                    )
                )
                id++
            }
        }
        pieces = newPieces
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE0F7FA))
            .onGloballyPositioned { rootPos = it.positionInRoot() }
    ) {
        // Back Button
        Image(
            painter = painterResource(id = R.drawable.ui_button_home),
            contentDescription = "Back",
            modifier = Modifier
                .padding(16.dp)
                .size(64.dp)
                .clickable { onBack() }
        )

        if (pieces.isEmpty()) {
            Text("Loading Puzzle...", modifier = Modifier.align(Alignment.Center))
        } else {
            // Drop Zones (Slots)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Complete the Puzzle!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006064))
                Spacer(modifier = Modifier.height(20.dp))
                
                // 2x2 Grid container
                Column {
                    Row {
                        PuzzleSlot(0, 0, pieceSizeDp) { slots = slots + it }
                        PuzzleSlot(0, 1, pieceSizeDp) { slots = slots + it }
                    }
                    Row {
                        PuzzleSlot(1, 0, pieceSizeDp) { slots = slots + it }
                        PuzzleSlot(1, 1, pieceSizeDp) { slots = slots + it }
                    }
                }
            }

            // Draggable Pieces
            pieces.forEach { piece ->
                DraggablePuzzlePiece(
                    piece = piece,
                    sizeDp = pieceSizeDp,
                    slots = slots, // Trebuie sa trecem coordonatele slot-urilor calculate global sau local
                    rootPos = rootPos,
                    onPlaced = { /* Optional: Check win condition */ }
                )
            }
        }
    }
}

@Composable
fun PuzzleSlot(
    row: Int, 
    col: Int, 
    sizeDp: androidx.compose.ui.unit.Dp,
    onReportPos: (Offset) -> Unit
) {
    Box(
        modifier = Modifier
            .size(sizeDp)
            .border(2.dp, Color.Gray.copy(alpha = 0.5f))
            .background(Color.White.copy(alpha = 0.3f))
            .onGloballyPositioned { 
                // Aici ar trebui sa raportam pozitia absoluta catre parinte
                // Dar pentru simplitate, in puzzle-ul simplu, calculam distanta in DraggablePiece
            }
    )
}

data class PuzzlePieceState(
    val id: Int,
    val bitmap: Bitmap,
    val correctRow: Int,
    val correctCol: Int,
    val initialX: Float,
    val initialY: Float
)

@Composable
fun DraggablePuzzlePiece(
    piece: PuzzlePieceState,
    sizeDp: androidx.compose.ui.unit.Dp,
    slots: List<Offset>, // Placeholder pt logica complexa
    rootPos: Offset,
    onPlaced: () -> Unit
) {
    val offsetX = remember { Animatable(piece.initialX, Float.VectorConverter) }
    val offsetY = remember { Animatable(piece.initialY, Float.VectorConverter) }
    val scope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }

    Image(
        bitmap = piece.bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .size(sizeDp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    isDragging = true
                    val pointerId = down.id

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) break
                        
                        val delta = change.positionChange()
                        change.consume()
                        
                        scope.launch {
                            offsetX.snapTo(offsetX.value + delta.x)
                            offsetY.snapTo(offsetY.value + delta.y)
                        }
                    }
                    isDragging = false
                    // Snap logic here if needed (verificare distanta fata de slotul corect)
                }
            }
    )
}