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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
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

enum class MathGameMode {
    COUNTING,
    ADDITION
}

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
            val answer = Random.nextInt(1, 10)
            val options = generateOptions(answer)
            return MathLevelData(mode, answer, 0, answer, options, itemRes)
        } else {
            val a = Random.nextInt(1, 5)
            val b = Random.nextInt(1, 5)
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
    
    // FIX: Folosim un boolean simplu în loc de MutableTransitionState pentru a evita erorile
    var isContentVisible by remember { mutableStateOf(false) }
    
    // Inițializăm interpolatorul o singură dată
    val overshootInterpolator = remember { OvershootInterpolator(1.2f) }

    // Activăm animația de intrare la prima randare
    LaunchedEffect(Unit) {
        isContentVisible = true
    }
    
    val scope = rememberCoroutineScope()

    fun handleAnswer(selected: Int) {
        if (selected == levelData.correctAnswer) {
            score += 10
            starState.value += 1
            showConfetti = true
            
            scope.launch {
                delay(1500)
                showConfetti = false
                if (currentLevelIndex < 9) {
                    // Start animație ieșire
                    isContentVisible = false
                    delay(400) // Așteptăm să se termine animația
                    
                    currentLevelIndex++
                    levelData = gameLogic.generateLevel(currentLevelIndex)
                    
                    // Start animație intrare
                    isContentVisible = true
                } else {
                    isGameOver = true
                }
            }
        } else {
            // Aici poți adăuga feedback vizual pentru eroare (ex: shake)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // FUNDAL
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
                onHome = { navController.navigate(Screen.MainMenu.route) }
            )
        } else {
            // LAYOUT PRINCIPAL (LANDSCAPE OPTIMIZED)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .systemBarsPadding()
            ) {
                // 1. HEADER (Sus)
                GameHeader(
                    progress = (currentLevelIndex + 1) / 10f,
                    score = score,
                    onBack = { navController.navigate(Screen.MainMenu.route) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. CONȚINUTUL (Split Stânga - Dreapta)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // PARTEA STÂNGĂ: Întrebarea (Card Mare)
                    Box(
                        modifier = Modifier
                            .weight(0.65f)
                            .fillMaxHeight()
                            .padding(end = 16.dp)
                    ) {
                        // FIX: Apelăm explicit androidx.compose.animation.AnimatedVisibility
                        // pentru a evita confuzia cu RowScope.AnimatedVisibility
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isContentVisible,
                            enter = scaleIn(animationSpec = tween(400, easing = { overshootInterpolator.getInterpolation(it) })) + fadeIn(),
                            exit = scaleOut(animationSpec = tween(300)) + fadeOut(),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            MathQuestionCard(levelData = levelData)
                        }
                    }

                    // PARTEA DREAPTĂ: Răspunsurile (Coloană verticală)
                    Box(
                        modifier = Modifier
                            .weight(0.35f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Răspunsurile sunt mereu vizibile, sau poți să le animezi și pe ele
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
fun GameHeader(progress: Float, score: Int, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(42.dp)
                .background(Color.White, CircleShape)
                .shadow(4.dp, CircleShape)
        ) {
            Icon(Icons.Default.Home, contentDescription = "Home", tint = Color(0xFFFF9800))
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(Brush.horizontalGradient(listOf(Color(0xFF4CAF50), Color(0xFF81C784))))
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .shadow(2.dp, RoundedCornerShape(16.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_score_star),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$score",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF37474F)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MathQuestionCard(levelData: MathLevelData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.95f),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha=0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (levelData.mode == MathGameMode.COUNTING) {
                Text(
                    "Câte vezi?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF2196F3),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                FlowRow(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.Center,
                    maxItemsInEachRow = 4
                ) {
                    repeat(levelData.numberA) {
                        BouncingImageItem(resId = levelData.itemResId, delay = it * 100)
                    }
                }

            } else {
                Text(
                    "Adună-le!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    GameGroupImages(count = levelData.numberA, resId = levelData.itemResId)
                    
                    Text(
                        " + ",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color(0xFF37474F),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    GameGroupImages(count = levelData.numberB, resId = levelData.itemResId)
                }
            }
        }
    }
}

@Composable
fun GameGroupImages(count: Int, resId: Int) {
    Box(
        modifier = Modifier
            .background(Color(0xFFF0F0F0), RoundedCornerShape(16.dp))
            .padding(8.dp)
    ) {
         Row(
             horizontalArrangement = Arrangement.Center,
             verticalAlignment = Alignment.CenterVertically
         ) {
             val displayCount = if (count > 3) 3 else count
             repeat(displayCount) {
                 Image(
                     painter = painterResource(id = resId),
                     contentDescription = null,
                     modifier = Modifier.size(36.dp)
                 )
             }
             if (count > 3) {
                 Text("+", fontSize = 20.sp, modifier = Modifier.padding(start = 4.dp))
             }
         }
    }
}

@Composable
fun BouncingImageItem(resId: Int, delay: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val dy by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, delayMillis = delay, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "dy"
    )

    Image(
        painter = painterResource(id = resId),
        contentDescription = null,
        modifier = Modifier
            .size(56.dp)
            .padding(4.dp)
            .graphicsLayer { translationY = dy }
    )
}

@Composable
fun AnswerOptionsColumn(options: List<Int>, onOptionSelected: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        options.forEach { number ->
            ImageButton(number = number, onClick = { onOptionSelected(number) })
        }
    }
}

@Composable
fun ImageButton(number: Int, onClick: () -> Unit) {
    val imageRes = MathAssets.getNumberImage(number)
    
    Button(
        onClick = onClick,
        modifier = Modifier
            .size(80.dp)
            .shadow(6.dp, CircleShape),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        contentPadding = PaddingValues(0.dp)
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "Number $number",
            modifier = Modifier.fillMaxSize().padding(8.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun MathGameOverDialog(score: Int, onRestart: () -> Unit, onHome: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                "Super Matematician!",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFFFF9800)
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Ai adunat $score puncte!", fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    repeat(3) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_score_star),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onRestart, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Joacă din nou")
            }
        },
        dismissButton = {
            Button(onClick = onHome, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                Text("Meniu")
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}