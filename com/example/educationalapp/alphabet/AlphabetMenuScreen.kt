package com.example.educationalapp.alphabet

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AlphabetMenuScreen(
    onPlayClick: () -> Unit,
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current
    val soundPlayer = remember { AlphabetSoundPlayer(context) }

    // Sound toggle local (pentru click-uri din meniu).
    var soundOn by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(soundOn) { soundPlayer.isEnabled = soundOn }

    // Curățăm resursele când părăsim meniul
    DisposableEffect(Unit) {
        onDispose { soundPlayer.release() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. FUNDAL
        Image(
            painter = painterResource(id = AlphabetUi.Backgrounds.menu),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Toggle sound (dreapta sus)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(18.dp)
        ) {
            MenuScaleButton(
                onClick = { soundOn = !soundOn },
                modifier = Modifier.size(50.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White, CircleShape)
                        .border(2.dp, Color(0xFFEEEEEE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = AlphabetUi.Icons.soundOn),
                        contentDescription = "Sound",
                        modifier = Modifier
                            .size(28.dp)
                            .alpha(if (soundOn) 1f else 0.45f)
                    )
                }
            }
        }

        // 2. CONȚINUT CENTRAL (Titlu + Mascota)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // TITLU
            val infiniteTransition = rememberInfiniteTransition(label = "title")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 1.05f,
                animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
                label = "scale"
            )

            Box(modifier = Modifier.scale(scale)) {
                Text(
                    text = "Alphabet\nAdventure",
                    style = TextStyle(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD700),
                        shadow = Shadow(color = Color(0xFFE65100), blurRadius = 8f, offset = androidx.compose.ui.geometry.Offset(4f, 4f))
                    ),
                    textAlign = TextAlign.Center,
                    lineHeight = 50.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // MASCOTA
            val rotateAnim by infiniteTransition.animateFloat(
                initialValue = -5f, targetValue = 5f,
                animationSpec = infiniteRepeatable(tween(1500, easing = EaseInOutQuad), RepeatMode.Reverse),
                label = "rotate"
            )

            Image(
                painter = painterResource(id = AlphabetUi.Mascot.normal),
                contentDescription = null,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(0.6f)
                    .rotate(rotateAnim),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(10.dp))
        }

        // 3. ZONA DE JOS

        // Butonul JOACĂ
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
        ) {
            MenuScaleButton(
                onClick = {
                    soundPlayer.playClick()
                    onPlayClick()
                },
                modifier = Modifier
                    .width(240.dp)
                    .height(80.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFFFFEB3B), Color(0xFFFFC107))),
                            RoundedCornerShape(30.dp)
                        )
                        .border(4.dp, Color.White, RoundedCornerShape(30.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "JOACĂ",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFE65100),
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        // Butonul HOME
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            MenuScaleButton(
                onClick = {
                    soundPlayer.playClick()
                    onBackToHome()
                },
                modifier = Modifier.size(50.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White, CircleShape)
                        .border(2.dp, Color(0xFFEEEEEE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = AlphabetUi.Icons.home),
                        contentDescription = "Home",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MenuScaleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(targetValue = if (isPressed) 0.9f else 1f, label = "press")

    Box(
        modifier = modifier
            .scale(animatedScale)
            .shadow(8.dp, RoundedCornerShape(30.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}
