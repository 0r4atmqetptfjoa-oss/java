package com.example.educationalapp

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.educationalapp.navigation.MainMenuRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// --- MAPPING RESURSE ---
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
                    .padding(16.dp)
            ) {
                val maxHeight = maxHeight
                
                // HEADER (SCOR & HOME)
                GameHeaderFloating(
                    score = score,
                    onBack = { showExitDialog = true },
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().zIndex(10f)
                )

                Row(
                    modifier = Modifier.fillMaxSize().padding(top = 50.dp), // Loc pentru header
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ZONA OBIECTE (STÂNGA - 70%)
                    Box(
                        modifier = Modifier
                            .weight(0.70f)
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

                    // ZONA RĂSPUNSURI (DREAPTA - 30%)
                    Box(
                        modifier = Modifier
                            .weight(0.30f)
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

@Composable
fun GameHeaderFloating(score: Int, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Buton Home FĂRĂ CHENAR (Doar iconiță albă strălucitoare)
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(64.dp)
                .background(Color.White.copy(alpha=0.15f), CircleShape) // Aproape invizibil
        ) {
            Icon(
                Icons.Default.Home, 
                contentDescription = "Home", 
                tint = Color.White,
                modifier = Modifier.size(42.dp).shadow(4.dp)
            )
        }

        // ZONA SCOR REPARATĂ
        Box(
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Steaua MULT MAI MARE
                Image(
                    painter = painterResource(id = R.drawable.ic_score_star),
                    contentDescription = null,
                    modifier = Modifier
                        .size(85.dp) // GIGANT
                        .graphicsLayer { 
                            shadowElevation = 25f 
                        }
                )
                
                // Textul Scorului MAI MIC
                Text(
                    text = "$score",
                    style = TextStyle(
                        fontSize = 32.sp, // Mult mai mic, ca să nu concureze cu obiectele
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        shadow = Shadow(color = Color(0xFFFF6F00), offset = Offset(3f, 3f), blurRadius = 6f)
                    ),
                    modifier = Modifier.padding(start = 2.dp)
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
            // Text MICȘORAT (32sp)
            OutlinedTextBig(text = "Câte vezi?", fontSize = 32.sp)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val contentHeight = availableHeight - 80.dp 
            
            // Obiecte GIGANT
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
            OutlinedTextBig(text = "Adună-le!", fontSize = 32.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                GameGroupImages(count = levelData.numberA, resId = levelData.itemResId)
                
                Text(
                    " + ",
                    style = TextStyle(
                        fontSize = 60.sp,
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
fun OutlinedTextBig(text: String, fontSize: androidx.compose.ui.unit.TextUnit) {
    Box(contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                color = Color.Black.copy(alpha=0.8f)
            ),
            modifier = Modifier.offset(2.dp, 2.dp)
        )
        Text(
            text = text,
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                color = Color.White,
                shadow = Shadow(color = Color(0xFF2196F3), blurRadius = 10f)
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
                    .size(100.dp) // Mari
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
        targetValue = 1.05f, // Pulsare fină
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
        modifier = Modifier.fillMaxHeight().padding(end = 8.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.End
    ) {
        options.forEach { number ->
            val imageRes = MathAssets.getNumberImage(number)
            
            // BUTOANE STRICT UNIFORME
            Button(
                onClick = { onOptionSelected(number) },
                modifier = Modifier
                    .size(100.dp) // DIMENSIUNE FIXĂ ȘI UNIFORMĂ
                    .shadow(12.dp, CircleShape, spotColor = Color.Black.copy(alpha=0.4f)),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                contentPadding = PaddingValues(0.dp) 
            ) {
                // Imaginea se scalează perfect în interior
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