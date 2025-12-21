package com.example.educationalapp.puzzle

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.educationalapp.R
import kotlin.math.roundToInt

@Composable
fun PuzzleGameScreen(
    viewModel: PuzzleViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        viewModel.loadGame(context)
    }

    val pieceSizeDp = 160.dp
    val pieceSizePx = with(density) { pieceSizeDp.toPx() }
    
    var slotsStartPos by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE1F5FE))
    ) {
        // Header
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
                    .size(64.dp)
                    .clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(20.dp))
            if (viewModel.isGameOver) {
                Text("BRAVO! Puzzle Complet!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            } else {
                Text("Rezolvă Puzzle-ul", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0277BD))
            }
        }

        if (viewModel.isLoading) {
            Text("Se încarcă...", modifier = Modifier.align(Alignment.Center))
        } else {
            // GRID (Tinta)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
                    .onGloballyPositioned { slotsStartPos = it.positionInRoot() }
            ) {
                Row {
                    Box(Modifier.size(pieceSizeDp).border(2.dp, Color.Gray.copy(0.5f)))
                    Box(Modifier.size(pieceSizeDp).border(2.dp, Color.Gray.copy(0.5f)))
                }
                Row {
                    Box(Modifier.size(pieceSizeDp).border(2.dp, Color.Gray.copy(0.5f)))
                    Box(Modifier.size(pieceSizeDp).border(2.dp, Color.Gray.copy(0.5f)))
                }
            }

            // PIESELE
            viewModel.pieces.forEach { piece ->
                Image(
                    bitmap = piece.bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                piece.currentPos.x.roundToInt(),
                                piece.currentPos.y.roundToInt()
                            )
                        }
                        .size(pieceSizeDp)
                        .pointerInput(piece.id, piece.isLocked) {
                            if (!piece.isLocked) {
                                detectDragGestures(
                                    onDragEnd = {
                                        viewModel.onPieceRelease(piece.id, pieceSizePx, slotsStartPos)
                                    }
                                ) { _, dragAmount ->
                                    viewModel.onPieceDrag(piece.id, androidx.compose.ui.geometry.Offset(dragAmount.x, dragAmount.y))
                                }
                            }
                        }
                )
            }
        }
    }
}