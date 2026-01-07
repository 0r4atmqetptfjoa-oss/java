package com.example.educationalapp

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
// Aceasta clasa face legatura intre logica si fisierele tale din drawable
object MathAssets {
    // Lista obiectelor de numarat
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

    // Mapping pentru numere (0-9)
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
            else -> R.drawable.img_number_0 // Fallback
        }
    }
}

enum class MathGameMode {
    COUNTING, // Doar numărat
    ADDITION  // Adunări simple
}

data class MathLevelData(
    val mode: MathGameMode,
    val numberA: Int,
    val numberB: Int = 0,
    val correctAnswer: Int,
    val options: List<Int>,
    val itemResId: Int // ID-ul resursei grafice (ex: R.drawable.img_math_apple)
)

class MathGameLogic {
    fun generateLevel(levelIndex: Int): MathLevelData {
        // Primele 5 nivele: Numărat. Următoarele 5: Adunări.
        val mode = if (levelIndex < 5) MathGameMode.COUNTING else MathGameMode.ADDITION
        
        // Alegem un obiect random din lista de asset-uri
        val itemRes = MathAssets.items.random()

        if (mode == MathGameMode.COUNTING) {
            // Logică Numărat (1-9)
            val answer = Random.nextInt(1, 10)
            val options = generateOptions(answer)
            return MathLevelData(mode, answer, 0, answer, options, itemRes)
        } else {
            // Logică Adunare (Sume până la 9 pentru a folosi imaginile 0-9)
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

@Composable
fun MathGameScreen(navController: NavController, starState: MutableState<Int>) {
    val gameLogic = remember { MathGameLogic() }
    
    var currentLevelIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    // Generăm primul nivel. Asigura-te ca ai resursele in drawable sau va da eroare la runtime!
    var levelData by remember { mutableStateOf(gameLogic.generateLevel(0)) }
    
    var isGameOver by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }
    
    val transitionState = remember { MutableTransitionState(false).apply { targetState = true } }
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
                    currentLevelIndex++
                    transitionState.targetState = false
                    delay(300)
                    levelData = gameLogic.generateLevel(currentLevelIndex)
                    transitionState.targetState = true
                } else {
                    isGameOver = true
                }
            }
        } else {
            // Feedback eroare (poți adăuga un shake animation aici)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. FUNDALUL (Imagine completă)
        Image(
            painter = painterResource(id = R.drawable.bg_game_math),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (showConfetti) {
            // Imagine confetti peste tot ecranul (sau particule)
            Image(
                painter = painterResource(id = R.drawable.img_confetti), // Asigura-te ca ai img_confetti.png
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
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
                },
                onHome = { navController.navigate(Screen.MainMenu.route) }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .systemBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                GameHeader(
                    progress = (currentLevelIndex + 1) / 10f,
                    score = score,
                    onBack = { navController.navigate(Screen.MainMenu.route) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Zona de Joc
                AnimatedVisibility(
                    visibleState = transitionState,
                    enter = scaleIn(animationSpec = tween(400, easing = OvershootInterpolator(1.2f))) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    MathQuestionCard(levelData = levelData)
                }

                Spacer(modifier = Modifier.weight(1f))

                // Zona Răspunsuri (Butoane cu Imagini)
                AnswerOptionsRow(
                    options = levelData.options,
                    onOptionSelected = { handleAnswer(it) }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
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
                .size(48.dp)
                .background(Color.White, CircleShape)
                .shadow(4.dp, CircleShape)
        ) {
            Icon(Icons.Default.Home, contentDescription = "Home", tint = Color(0xFFFF9800))
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .height(16.dp)
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
            // Folosim imaginea stelei (ic_score_star)
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

@Composable
fun MathQuestionCard(levelData: MathLevelData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
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
                Spacer(modifier = Modifier.height(24.dp))
                
                FlowRow(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    maxItemsInEachRow = 3
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
             modifier = Modifier.width(if(count > 1) 100.dp else 60.dp), 
             horizontalArrangement = Arrangement.Center
         ) {
             // Limităm vizual la 2-3 iconițe ca să nu aglomerăm, 
             // sau le afișăm pe toate dacă sunt mici
             val displayCount = if (count > 3) 3 else count
             repeat(displayCount) {
                 Image(
                     painter = painterResource(id = resId),
                     contentDescription = null,
                     modifier = Modifier.size(40.dp)
                 )
             }
             if (count > 3) {
                 Text("+", fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterVertically))
             }
         }
    }
}

@Composable
fun BouncingImageItem(resId: Int, delay: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val dy by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, delayMillis = delay, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "dy"
    )

    Image(
        painter = painterResource(id = resId),
        contentDescription = null,
        modifier = Modifier
            .size(60.dp)
            .padding(4.dp)
            .graphicsLayer { translationY = dy }
    )
}

@Composable
fun AnswerOptionsRow(options: List<Int>, onOptionSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
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
            .size(90.dp)
            .shadow(8.dp, CircleShape),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        contentPadding = PaddingValues(0.dp) // Important pentru ca imaginea să fie mare
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "Number $number",
            modifier = Modifier.fillMaxSize().padding(10.dp), // Padding mic interior
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