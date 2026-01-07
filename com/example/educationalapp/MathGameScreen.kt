package com.example.educationalapp

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border // FIX: Import adăugat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex // FIX: Import adăugat
import androidx.navigation.NavController
import com.example.educationalapp.navigation.MainMenuRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// --- CONFIGURARE RESURSE ---
object MathAssets {
    val items = listOf(
        R.drawable.img_math_apple,
        R.drawable.img_math_banana,
        R.drawable.img_math_strawberry,
        R.drawable.img_math_orange,
        R.drawable.img_math_balloon,
        R.drawable.img_math_star,
        R.drawable.img_math_puppy,
        R.drawable.img_math_kitten
    )

    fun getNumberImage(number: Int): Int {
        return when (number) {
            0 -> R.drawable.img_number_0
            1 -> R.drawable.img_number_1
            2 -> R.drawable.img_number_2
            3 -> R.drawable.img_number_3
            4 -> R.drawable.img_number_4
            5 -> R.drawable.img_number_5
            6 -> R.drawable.img_number_6
            7 -> R.drawable.img_number_7
            8 -> R.drawable.img_number_8
            9 -> R.drawable.img_number_9
            else -> R.drawable.img_number_0
        }
    }
}

enum class MathGameMode { COUNTING, ADDITION }

data class MathLevelData(
    val mode: MathGameMode,
    val numberA: Int,
    val numberB: Int = 0,
    val correctAnswer: Int,
    val options: List<Int>,
    val itemResId: Int
)

class MathGameLogic {
    fun generateLevel(levelIndex: Int): MathLevelData {
        val mode = if (levelIndex < 5) MathGameMode.COUNTING else MathGameMode.ADDITION
        val itemRes = MathAssets.items.random()

        if (mode == MathGameMode.COUNTING) {
            val answer = Random.nextInt(1, 6)
            val options = generateOptions(answer)
            return MathLevelData(mode, answer, 0, answer, options, itemRes)
        } else {
            val a = Random.nextInt(1, 4)
            val b = Random.nextInt(1, 4)
            val answer = a + b
            val options = generateOptions(answer)
            return MathLevelData(mode, a, b, answer, options, itemRes)
        }
    }

    private fun generateOptions(correct: Int): List<Int> {
        val opts = mutableSetOf(correct)
        while (opts.size < 3) {
            val distraction = correct + Random.nextInt(-3, 4)
            if (distraction in 1..9 && distraction != correct) {
                opts.add(distraction)
            }
        }
        return opts.shuffled()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MathGameScreen(navController: NavController, starState: MutableState<Int>) {
    val gameLogic = remember { MathGameLogic() }
    
    var currentLevelIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var levelData by remember { mutableStateOf(gameLogic.generateLevel(0)) }
    
    var isGameOver by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    
    var isContentVisible by remember { mutableStateOf(false) }
    val overshootInterpolator = remember { OvershootInterpolator(1.2f) }

    LaunchedEffect(Unit) { isContentVisible = true }
    
    val scope = rememberCoroutineScope()

    fun handleAnswer(selected: Int) {
        if (selected == levelData.correctAnswer) {
            score += 10
            starState.value += 1
            showConfetti = true
            
            scope.launch {
                delay(1000)
                showConfetti = false
                if (currentLevelIndex < 9) {
                    isContentVisible = false
                    delay(300)
                    currentLevelIndex++
                    levelData = gameLogic.generateLevel(currentLevelIndex)
                    isContentVisible = true
                } else {
                    isGameOver = true
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_game_math),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (showConfetti) {
            Image(
                painter = painterResource(id = R.drawable.img_confetti),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showExitDialog) {
             ExitDialog(
                 onConfirm = { 
                     navController.navigate(MainMenuRoute) { popUpTo(MainMenuRoute) { inclusive = true } } 
                 },
                 onDismiss = { showExitDialog = false }
             )
        }

        if (isGameOver) {
            MathGameOverDialog(
                score = score,
                onRestart = {
                    currentLevelIndex = 0
                    score = 0
                    isGameOver = false
                    levelData = gameLogic.generateLevel(0)
                    isContentVisible = true
                },
                onHome = { navController.navigate(MainMenuRoute) }
            )
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(12.dp)
            ) {
                val maxHeight = maxHeight
                
                // FIX: zIndex acum va funcționa corect datorită importului
                GameHeaderFloating(
                    score = score,
                    onBack = { showExitDialog = true },
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().zIndex(10f)
                )

                Row(
                    modifier = Modifier.fillMaxSize().padding(top = 60.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.75f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isContentVisible,
                            enter = scaleIn(animationSpec = tween(400, easing = { overshootInterpolator.getInterpolation(it) })) + fadeIn(),
                            exit = scaleOut(animationSpec = tween(250)) + fadeOut()
                        ) {
                            MathQuestionAreaMaximized(levelData = levelData, availableHeight = maxHeight)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(0.25f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        AnswerOptionsColumn(
                            options = levelData.options,
                            onOptionSelected = { handleAnswer(it) }
                        )
                    }
                }
            }
        }
    }
}

// --- UI COMPONENTS ---

@Composable
fun GameHeaderFloating(score: Int, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(64.dp)
                .background(Color.White.copy(alpha=0.2f), CircleShape)
                .border(2.dp, Color.White.copy(alpha=0.5f), CircleShape) // FIX: border acum funcționează
        ) {
            Icon(
                Icons.Default.Home, 
                contentDescription = "Home", 
                tint = Color.White,
                modifier = Modifier.size(40.dp).shadow(4.dp)
            )
        }

        Box(
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier
                .size(90.dp)
                .background(Brush.radialGradient(listOf(Color(0xFFFFD700).copy(alpha=0.6f), Color.Transparent)))
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_score_star),
                    contentDescription = null,
                    modifier = Modifier
                        .size(70.dp)
                        .graphicsLayer { 
                            shadowElevation = 20f 
                            scaleX = 1.1f
                            scaleY = 1.1f
                        }
                )
                Text(
                    text = "$score",
                    style = TextStyle(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        shadow = Shadow(color = Color(0xFFFF6F00), offset = Offset(4f, 4f), blurRadius = 8f)
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MathQuestionAreaMaximized(levelData: MathLevelData, availableHeight: Dp) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (levelData.mode == MathGameMode.COUNTING) {
            OutlinedTextBig(text = "Câte vezi?")
            
            Spacer(modifier = Modifier.height(10.dp))
            
            val contentHeight = availableHeight - 100.dp 
            
            val itemSize = when (levelData.numberA) {
                1 -> contentHeight * 0.85f 
                2 -> contentHeight * 0.55f 
                3 -> contentHeight * 0.50f 
                else -> contentHeight * 0.40f 
            }

            FlowRow(
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.Center,
                maxItemsInEachRow = 3
            ) {
                repeat(levelData.numberA) {
                    BouncingImageItem(resId = levelData.itemResId, delay = it * 100, size = itemSize)
                }
            }

        } else {
            OutlinedTextBig(text = "Adună-le!")
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                GameGroupImages(count = levelData.numberA, resId = levelData.itemResId)
                
                Text(
                    " + ",
                    style = TextStyle(
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        shadow = Shadow(color = Color.Black.copy(alpha=0.5f), offset = Offset(4f, 4f), blurRadius = 8f)
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                
                GameGroupImages(count = levelData.numberB, resId = levelData.itemResId)
            }
        }
    }
}

@Composable
fun OutlinedTextBig(text: String) {
    Box(contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black.copy(alpha=0.8f)
            ),
            modifier = Modifier.offset(3.dp, 3.dp)
        )
        Text(
            text = text,
            style = TextStyle(
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                shadow = Shadow(color = Color(0xFF2196F3), blurRadius = 15f)
            )
        )
    }
}

@Composable
fun GameGroupImages(count: Int, resId: Int) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                modifier = Modifier
                    .size(110.dp)
                    .shadow(8.dp, CircleShape, spotColor = Color.Black.copy(alpha=0.4f))
            )
        }
    }
}

@Composable
fun BouncingImageItem(resId: Int, delay: Int, size: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, delayMillis = delay, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Image(
        painter = painterResource(id = resId),
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .scale(scale)
            .graphicsLayer { 
                shadowElevation = 30f 
                spotShadowColor = Color.Black.copy(alpha=0.5f)
            }
    )
}

@Composable
fun AnswerOptionsColumn(options: List<Int>, onOptionSelected: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxHeight().padding(end = 16.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.End
    ) {
        options.forEach { number ->
            val imageRes = MathAssets.getNumberImage(number)
            Button(
                onClick = { onOptionSelected(number) },
                modifier = Modifier
                    .size(110.dp)
                    .shadow(12.dp, CircleShape, spotColor = Color.Black.copy(alpha=0.4f)),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                contentPadding = PaddingValues(0.dp)
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = "Number $number",
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun ExitDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pauză", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold, fontSize = 28.sp) },
        text = { Text("Vrei să ieși la meniu?", fontSize = 20.sp) },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))) {
                Text("Da, Ies", fontSize = 18.sp)
            }
        },
        dismissButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                Text("Nu, Joc", fontSize = 18.sp)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun MathGameOverDialog(score: Int, onRestart: () -> Unit, onHome: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text("Bravo!", textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineMedium, color = Color(0xFFFF9800))
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Scor final: $score", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Row { repeat(3) { Image(painter = painterResource(id = R.drawable.ic_score_star), contentDescription = null, modifier = Modifier.size(60.dp)) } }
            }
        },
        confirmButton = {
            Button(onClick = onRestart, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                Text("Din nou", fontSize = 20.sp)
            }
        },
        dismissButton = {
            Button(onClick = onHome, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                Text("Meniu", fontSize = 20.sp)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(32.dp)
    )
}