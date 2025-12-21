package com.example.educationalapp.shapes

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.educationalapp.R

@Composable
fun ShapesGameScreen(
    viewModel: ShapesViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // AICI ERA PROBLEMA: Folosim numele corecte ale imaginilor încărcate de tine
    val robotImageRes = when (uiState.gameState) {
        GameState.CORRECT_FEEDBACK -> R.drawable.mascot_robot_happy // Corectat (fără _tablet)
        GameState.WRONG_FEEDBACK -> R.drawable.mascot_robot_sad     // Corectat (fără _tablet)
        else -> R.drawable.mascot_robot_holding_tablet              // Acesta rămâne corect
    }

    val tabletOffsetY by animateDpAsState(
        targetValue = when (uiState.gameState) {
            GameState.CORRECT_FEEDBACK -> (-110).dp
            GameState.WRONG_FEEDBACK -> 80.dp
            else -> 50.dp
        },
        label = "tabletMove"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        
        Image(
            painter = painterResource(id = R.drawable.bg_shapes_layer1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Image(
            painter = painterResource(id = R.drawable.bg_shapes_layer2),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .align(Alignment.BottomCenter),
            contentScale = ContentScale.FillWidth
        )

        Row(modifier = Modifier.fillMaxSize()) {
            
            Box(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(380.dp)
                        .offset(y = 30.dp)
                ) {
                    Image(
                        painter = painterResource(id = robotImageRes),
                        contentDescription = "Robot",
                        modifier = Modifier.fillMaxSize()
                    )

                    // Afișăm forma geometrică (își schimbă poziția cu robotul)
                    Box(
                        modifier = Modifier
                            .size(width = 130.dp, height = 90.dp)
                            .align(Alignment.Center)
                            .offset(y = tabletOffsetY)
                            .padding(5.dp)
                    ) {
                        RobotScreenShape(shape = uiState.targetShape)
                    }
                }
                
                Box(modifier = Modifier.align(Alignment.TopStart).padding(20.dp)) {
                    Text(
                        text = "Scor: ${uiState.score}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Image(
                    painter = painterResource(id = R.drawable.ui_btn_home),
                    contentDescription = "Back",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(20.dp)
                        .size(60.dp)
                        .clickable { onBack() }
                )
            }

            Box(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(uiState.options) { item ->
                        val isWrong = uiState.wrongSelectionId == item.id
                        val isEnabled = uiState.gameState == GameState.WAITING_INPUT

                        ShapeOptionCard(
                            item = item,
                            isWrong = isWrong,
                            isEnabled = isEnabled,
                            onClick = { 
                                viewModel.onOptionSelected(item)
                            }
                        )
                    }
                }
            }
        }
        
        if (uiState.gameState == GameState.CORRECT_FEEDBACK) {
             Text(
                 "BRAVO!",
                 modifier = Modifier
                     .align(Alignment.Center)
                     .offset(y = (-150).dp, x = (-100).dp),
                 fontSize = 50.sp,
                 color = Color.Green,
                 fontWeight = FontWeight.Black,
                 style = androidx.compose.ui.text.TextStyle(
                     shadow = androidx.compose.ui.graphics.Shadow(
                         color = Color.Black, blurRadius = 10f
                     )
                 )
             )
        }
    }
}

@Composable
fun RobotScreenShape(shape: ShapeType) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2
        
        val strokeStyle = Stroke(width = 8f)
        val shapeColor = Color(0xFF00E5FF)

        when (shape) {
            ShapeType.CIRCLE -> drawCircle(color = shapeColor, radius = h/2.2f, style = strokeStyle)
            ShapeType.SQUARE -> drawRect(color = shapeColor, topLeft = Offset(cx - h/2.5f, cy - h/2.5f), size = Size(h*0.8f, h*0.8f), style = strokeStyle)
            ShapeType.RECTANGLE -> drawRect(color = shapeColor, topLeft = Offset(w*0.1f, h*0.2f), size = Size(w*0.8f, h*0.6f), style = strokeStyle)
            ShapeType.TRIANGLE -> {
                val path = Path().apply {
                    moveTo(cx, h * 0.1f)
                    lineTo(w * 0.8f, h * 0.9f)
                    lineTo(w * 0.2f, h * 0.9f)
                    close()
                }
                drawPath(path, shapeColor, style = strokeStyle)
            }
            ShapeType.STAR -> {
                val path = Path().apply {
                     moveTo(cx, 0f)
                     lineTo(w * 0.65f, cy)
                     lineTo(w, cy)
                     lineTo(w * 0.75f, h * 0.75f)
                     lineTo(w * 0.85f, h)
                     lineTo(cx, h * 0.85f)
                     lineTo(w * 0.15f, h)
                     lineTo(w * 0.25f, h * 0.75f)
                     lineTo(0f, cy)
                     lineTo(w * 0.35f, cy)
                     close()
                }
                 drawPath(path, shapeColor, style = strokeStyle)
            }
            ShapeType.HEART -> {
                val path = Path().apply {
                    moveTo(cx, h * 0.8f)
                    cubicTo(w * 0.1f, h * 0.4f, 0f, 0f, cx, h * 0.3f)
                    cubicTo(w, 0f, w * 0.9f, h * 0.4f, cx, h * 0.8f)
                }
                drawPath(path, shapeColor, style = strokeStyle)
            }
        }
    }
}

@Composable
fun ShapeOptionCard(
    item: ShapeItem,
    isWrong: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(targetValue = if (isWrong) 0.8f else 1f, label = "scale")
    val alpha by animateFloatAsState(targetValue = if (isWrong) 0.5f else 1f, label = "alpha")
    
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -5f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "floatVal"
    )

    Box(
        modifier = Modifier
            .size(130.dp)
            .scale(scale)
            .offset(y = floatY.dp)
            .alpha(alpha)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.5f))
            .clickable(enabled = isEnabled) { onClick() }
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = item.imageRes),
            contentDescription = item.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}