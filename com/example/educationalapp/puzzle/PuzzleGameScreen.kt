package com.example.educationalapp.puzzle

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.educationalapp.R
import kotlin.math.roundToInt

@Composable
fun PuzzleGameScreen(
    viewModel: PuzzleViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current

    var boardOffset by remember { mutableStateOf(Offset.Zero) }
    var boardSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    Box(modifier = Modifier.fillMaxSize()) {
        
        Image(
            painter = painterResource(id = R.drawable.bg_puzzle_table),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(60.dp).zIndex(100f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ui_btn_home),
                contentDescription = "Back",
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize().clickable { onBack() })
        }

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.weight(0.75f).fillMaxHeight().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 60.dp, vertical = 20.dp) 
                        .aspectRatio(4f/3f)
                        .background(Color.Black.copy(alpha = 0.2f))
                        .border(6.dp, Color(0xFF8D6E63), RoundedCornerShape(8.dp))
                        .onGloballyPositioned { coordinates ->
                            if (boardSize == androidx.compose.ui.geometry.Size.Zero) {
                                boardSize = androidx.compose.ui.geometry.Size(
                                    coordinates.size.width.toFloat(),
                                    coordinates.size.height.toFloat()
                                )
                                boardOffset = coordinates.localToRoot(Offset.Zero)
                                viewModel.startGame(context, boardSize.width, boardSize.height)
                            }
                        }
                ) {
                    if (uiState.currentThemeResId != 0) {
                        Image(
                            painter = painterResource(id = uiState.currentThemeResId),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().alpha(0.3f),
                            contentScale = ContentScale.FillBounds
                        )
                    }
                }
            }
            Box(modifier = Modifier.weight(0.25f).fillMaxHeight().background(Color.Black.copy(alpha = 0.3f)))
        }

        if (!uiState.isLoading && boardSize != androidx.compose.ui.geometry.Size.Zero) {
            Box(modifier = Modifier.fillMaxSize()) {
                uiState.pieces.forEach { piece ->
                    
                    val pieceWidthDp = with(density) { piece.width.toDp() }
                    val pieceHeightDp = with(density) { piece.height.toDp() }
                    
                    val elevation by animateDpAsState(targetValue = if (piece.isLocked) 0.dp else 10.dp)
                    val zIndexPiece = if (piece.isLocked) 0f else 20f 

                    val absoluteX = boardOffset.x + piece.currentX
                    val absoluteY = boardOffset.y + piece.currentY

                    Image(
                        bitmap = piece.bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .offset { IntOffset(absoluteX.roundToInt(), absoluteY.roundToInt()) }
                            .size(pieceWidthDp, pieceHeightDp)
                            .zIndex(zIndexPiece)
                            .shadow(elevation)
                            .clip(PuzzleShape(piece.config)) 
                            .pointerInput(piece.id, piece.isLocked) {
                                if (!piece.isLocked) {
                                    detectDragGestures(
                                        onDragEnd = { viewModel.onPieceDrop(piece.id) }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        viewModel.onPieceDrag(piece.id, dragAmount.x, dragAmount.y)
                                    }
                                }
                            }
                    )
                }
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        if (uiState.isComplete) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).clickable {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BRAVO!", color = Color.Green, fontSize = 60.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { boardSize = androidx.compose.ui.geometry.Size.Zero },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                    ) {
                        Text("Joc Nou", fontSize = 24.sp, color = Color.White, modifier = Modifier.padding(10.dp))
                    }
                }
            }
        }
    }
}