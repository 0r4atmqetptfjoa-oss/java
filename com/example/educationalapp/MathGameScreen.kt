package com.example.educationalapp

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// --- CULORI DESIGN (Palette Pixar/Disney) ---
private val MathBgGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFFE0F7FA), Color(0xFFB2EBF2)) // Cer senin
)
private val CardBgColor = Color(0xFFFFFFFF)
private val PrimaryColor = Color(0xFFFF9800) // Portocaliu vibrant
private val SecondaryColor = Color(0xFF4CAF50) // Verde crud
private val AccentColor = Color(0xFF2196F3) // Albastru jucăuș
private val TextColor = Color(0xFF37474F)

// --- MODELE DE DATE ---
enum class MathGameMode {
    COUNTING, // Doar numărat
    ADDITION  // Adunări simple
}

data class MathLevelData(
    val mode: MathGameMode,
    val numberA: Int,
    val numberB: Int = 0, // Folosit doar la adunare
    val correctAnswer: Int,
    val options: List<Int>,
    val itemIcon: String // Emoji sau caracter reprezentativ
)

// --- VIEW MODEL LOCAL (Pentru simplitate și integritate) ---
class MathGameLogic {
    private val icons = listOf("🍎", "🍌", "🍓", "🍊", "🎈", "⭐️", "🐶", "🐱")

    fun generateLevel(levelIndex: Int): MathLevelData {
        // Primele 5 nivele sunt de numărat, următoarele 5 sunt adunări
        val mode = if (levelIndex < 5) MathGameMode.COUNTING else MathGameMode.ADDITION
        val icon = icons.random()

        if (mode == MathGameMode.COUNTING) {
            // Logică Numărat (1-9)
            val answer = Random.nextInt(1, 10)
            val options = generateOptions(answer)
            return MathLevelData(mode, answer, 0, answer, options, icon)
        } else {
            // Logică Adunare (Sume până la 10)
            val a = Random.nextInt(1, 6)
            val b = Random.nextInt(1, 6)
            val answer = a + b
            val options = generateOptions(answer)
            return MathLevelData(mode, a, b, answer, options, icon)
        }
    }

    private fun generateOptions(correct: Int): List<Int> {
        val opts = mutableSetOf(correct)
        while (opts.size < 3) {
            val distraction = correct + Random.nextInt(-3, 4)
            if (distraction > 0 && distraction != correct) {
                opts.add(distraction)
            }
        }
        return opts.shuffled()
    }
}

@Composable
fun MathGameScreen(navController: NavController, starState: MutableState<Int>) {
    val gameLogic = remember { MathGameLogic() }
    
    // State-ul jocului
    var currentLevelIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var levelData by remember { mutableStateOf(gameLogic.generateLevel(0)) }
    var isGameOver by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }
    
    // Animație pentru tranziția între întrebări
    val transitionState = remember { MutableTransitionState(false).apply { targetState = true } }
    
    // Scope pentru corutine (delay-uri)
    val scope = rememberCoroutineScope()

    fun handleAnswer(selected: Int) {
        if (selected == levelData.correctAnswer) {
            // Răspuns Corect
            score += 10
            starState.value += 1
            showConfetti = true
            
            // Sunet de succes (Aici poți apela SoundManager)
            // ex: SoundManager.playSuccess()

            scope.launch {
                delay(1500) // Așteptăm să sărbătorească copilul
                showConfetti = false
                if (currentLevelIndex < 9) {
                    currentLevelIndex++
                    transitionState.targetState = false // Reset animație ieșire
                    delay(300)
                    levelData = gameLogic.generateLevel(currentLevelIndex)
                    transitionState.targetState = true // Start animație intrare
                } else {
                    isGameOver = true
                }
            }
        } else {
            // Răspuns Greșit - Feedback vizual simplu (shake sau sunet)
            // ex: SoundManager.playError()
        }
    }

    // --- UI PRINCIPAL ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MathBgGradient)
    ) {
        if (showConfetti) {
            // Aici ar fi un ParticleSystem complex, dar simulăm cu un overlay simplu sau emoji
            ConfettiOverlay()
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
                // 1. Header (Buton Back + Progres + Stele)
                GameHeader(
                    progress = (currentLevelIndex + 1) / 10f,
                    score = score,
                    onBack = { navController.navigate(Screen.MainMenu.route) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Zona de Joc (Întrebarea Vizuală)
                // Folosim AnimatedVisibility pentru efecte la schimbarea nivelului
                AnimatedVisibility(
                    visibleState = transitionState,
                    enter = scaleIn(animationSpec = tween(500, easing = OvershootInterpolator(1.2f))) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    MathQuestionCard(levelData = levelData)
                }

                Spacer(modifier = Modifier.weight(1f))

                // 3. Zona de Răspunsuri (Butoane Mari)
                AnswerOptionsRow(
                    options = levelData.options,
                    onOptionSelected = { handleAnswer(it) }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// --- COMPONENTE UI ---

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
            Icon(Icons.Default.Home, contentDescription = "Home", tint = AccentColor)
        }

        // Bara de progres stilizată
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
                    .background(
                        Brush.horizontalGradient(listOf(SecondaryColor, Color(0xFF81C784)))
                    )
            )
        }

        // Afișaj Scor / Stele
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .shadow(2.dp, RoundedCornerShape(16.dp))
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$score",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextColor
            )
        }
    }
}

@Composable
fun MathQuestionCard(levelData: MathLevelData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp), // Fix height pentru consistență
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = CardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (levelData.mode == MathGameMode.COUNTING) {
                // MODUL: NUMĂRAT
                Text(
                    "Câte vezi?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AccentColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                // Grid de obiecte animate
                FlowRow(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    maxItemsInEachRow = 3
                ) {
                    repeat(levelData.numberA) {
                        BouncingItem(emoji = levelData.itemIcon, delay = it * 100)
                    }
                }

            } else {
                // MODUL: ADUNARE (Vizual: Grup A + Grup B)
                Text(
                    "Adună-le pe toate!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = PrimaryColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Grupul A
                    GameGroupContainer(count = levelData.numberA, emoji = levelData.itemIcon)
                    
                    Text(
                        " + ",
                        style = MaterialTheme.typography.displayMedium,
                        color = TextColor,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    // Grupul B
                    GameGroupContainer(count = levelData.numberB, emoji = levelData.itemIcon)
                }
            }
        }
    }
}

@Composable
fun GameGroupContainer(count: Int, emoji: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFFF0F0F0), RoundedCornerShape(16.dp))
            .padding(8.dp)
    ) {
        // Folosim un layout simplu pentru grupuri mici
        Column {
             // Împărțim vizual dacă sunt multe, dar pentru sume mici e ok un grid mic
             val rows = if(count > 2) 2 else 1
             val cols = (count + 1) / 2
             
             // Simplificare randare: Doar listăm obiectele
             Row(modifier = Modifier.width(if(count > 1) 100.dp else 50.dp), horizontalArrangement = Arrangement.Center) {
                 repeat(count) {
                     Text(text = emoji, fontSize = 32.sp)
                 }
             }
        }
    }
}

@Composable
fun BouncingItem(emoji: String, delay: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val dy by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, delayMillis = delay, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "dy"
    )

    Text(
        text = emoji,
        fontSize = 48.sp,
        modifier = Modifier
            .padding(8.dp)
            .graphicsLayer { translationY = dy }
    )
}

@Composable
fun AnswerOptionsRow(options: List<Int>, onOptionSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        options.forEach { option ->
            GameButton(text = option.toString(), onClick = { onOptionSelected(option) })
        }
    }
}

@Composable
fun GameButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .size(90.dp) // Butoane mari, rotunde
            .shadow(8.dp, CircleShape),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
            color = AccentColor
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
                color = PrimaryColor
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Ai adunat $score puncte!", fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                // 3 Stele mari animate
                Row {
                    repeat(3) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onRestart, colors = ButtonDefaults.buttonColors(containerColor = SecondaryColor)) {
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
        tonalElevation = 10.dp,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun ConfettiOverlay() {
    // Un simplu overlay care arată "Bravo!" și confetti emojis care cad
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "BRAVO!",
            style = MaterialTheme.typography.displayLarge.copy(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha=0.3f),
                    blurRadius = 8f
                )
            ),
            color = Color.White
        )
    }
}