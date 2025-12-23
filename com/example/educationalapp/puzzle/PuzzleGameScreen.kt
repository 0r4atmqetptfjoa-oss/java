package com.example.educationalapp.puzzle

import android.view.SoundEffectConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.educationalapp.R
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

private data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    var size: Float
)

@Composable
fun PuzzleGameScreen(
    viewModel: PuzzleViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current
    val view = LocalView.current

    var boardOffset by remember { mutableStateOf(Offset.Zero) }
    var boardSizePx by remember { mutableStateOf(Size.Zero) }

    var trayWidthPx by remember { mutableStateOf(0f) }
    val gapDp = 12.dp
    val gapPx = with(density) { gapDp.toPx() }

    var draggingId by remember { mutableStateOf<Int?>(null) }

    val particles = remember { mutableStateListOf<Particle>() }
    var prevLocked by remember { mutableStateOf(setOf<Int>()) }

    // Update particles (compatibilitate: delay)
    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            if (particles.isEmpty()) continue
            val dt = 0.016f
            val gravity = 1200f
            val friction = 0.985f

            val it = particles.listIterator()
            while (it.hasNext()) {
                val p = it.next()
                p.vy += gravity * dt
                p.vx *= friction
                p.vy *= friction
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.life -= dt
                if (p.life <= 0f) it.remove()
            }
        }
    }

    // Sparkle on lock + confetti on complete
    LaunchedEffect(uiState.pieces, uiState.isComplete, boardOffset) {
        val lockedNow = uiState.pieces.filter { it.isLocked }.map { it.id }.toSet()
        val newlyLocked = lockedNow - prevLocked
        if (newlyLocked.isNotEmpty()) {
            newlyLocked.forEach { id ->
                val p = uiState.pieces.firstOrNull { it.id == id } ?: return@forEach
                val cx = boardOffset.x + p.targetX + p.width / 2f
                val cy = boardOffset.y + p.targetY + p.height / 2f
                spawnSparkle(particles, cx, cy)
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
        }
        prevLocked = lockedNow

        if (uiState.isComplete && boardSizePx != Size.Zero) {
            spawnConfetti(particles, boardOffset.x, boardOffset.y, boardSizePx.width, boardSizePx.height)
        }
    }

    // Auto-start / auto-next when layout is ready
    var startedLayoutKey by remember { mutableStateOf("") }
    LaunchedEffect(boardSizePx, trayWidthPx) {
        if (boardSizePx.width > 0f && boardSizePx.height > 0f && trayWidthPx > 0f) {
            val key = "${boardSizePx.width.toInt()}x${boardSizePx.height.toInt()}_${trayWidthPx.toInt()}"
            if (key != startedLayoutKey) {
                startedLayoutKey = key
                viewModel.startGame(
                    context = context,
                    boardWidth = boardSizePx.width,
                    boardHeight = boardSizePx.height,
                    trayStartX = boardSizePx.width + gapPx,
                    trayWidth = trayWidthPx,
                    trayHeight = boardSizePx.height
                )
            }
        }
    }

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete && boardSizePx != Size.Zero && trayWidthPx > 0f) {
            delay(900L)
            viewModel.startGame(
                context = context,
                boardWidth = boardSizePx.width,
                boardHeight = boardSizePx.height,
                trayStartX = boardSizePx.width + gapPx,
                trayWidth = trayWidthPx,
                trayHeight = boardSizePx.height
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(id = R.drawable.bg_puzzle_table),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Back (home)
        Image(
            painter = painterResource(id = R.drawable.ui_btn_home),
            contentDescription = "Back",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
                .size(56.dp)
                .zIndex(10000f)
                .clickableNoIndication { onBack() }
        )

        val outerPad = 12.dp

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(outerPad)
        ) {
            val trayWdp: Dp = minOf(320.dp, maxOf(220.dp, maxWidth * 0.26f))
            trayWidthPx = with(density) { trayWdp.toPx() }

            val availW = maxWidth - trayWdp - gapDp
            val availH = maxHeight

            val aspect = 4f / 3f
            var boardW = availW
            var boardH = boardW / aspect
            if (boardH > availH) {
                boardH = availH
                boardW = boardH * aspect
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(gapDp),
                verticalAlignment = Alignment.Top
            ) {

                // BOARD
                BoardBox(
                    modifier = Modifier.size(boardW, boardH),
                    themeResId = uiState.currentThemeResId,
                    pieces = uiState.pieces,
                    onGloballyPositioned = { offset, size ->
                        boardOffset = offset
                        boardSizePx = size
                    }
                )

                // TRAY (HUD only, pieces are drawn in overlay)
                TrayBox(
                    modifier = Modifier
                        .width(trayWdp)
                        .height(boardH),
                    remaining = uiState.pieces.count { !it.isLocked },
                    onShuffle = { viewModel.shuffleTray() }
                )
            }
        }

        // PARTICLES + PIECES overlay in root-space
        if (!uiState.isLoading && boardSizePx != Size.Zero) {

            val trayScale = remember(trayWidthPx, boardSizePx.height, uiState.pieces.size) {
                if (trayWidthPx <= 0f || boardSizePx.height <= 0f || uiState.pieces.isEmpty()) 0.75f
                else {
                    val cellW = trayWidthPx / 2f
                    val cellH = boardSizePx.height / 8f
                    val p = uiState.pieces.first()
                    val sx = (cellW * 0.92f) / p.width.toFloat()
                    val sy = (cellH * 0.92f) / p.height.toFloat()
                    min(0.85f, maxOf(0.55f, min(sx, sy)))
                }
            }

            // Particles
            Canvas(modifier = Modifier.fillMaxSize().zIndex(500f)) {
                particles.forEach { p ->
                    drawCircle(
                        color = Color.White.copy(alpha = (p.life / 0.9f).coerceIn(0f, 1f)),
                        radius = p.size,
                        center = Offset(p.x, p.y)
                    )
                }
            }

            // Pieces
            uiState.pieces.forEach { piece ->
                val isDragging = draggingId == piece.id
                val inTray = !piece.isLocked && piece.currentX >= (uiState.trayStartX - 4f)

                val scaleTarget = when {
                    isDragging -> 1.06f
                    inTray -> trayScale
                    else -> 1f
                }
                val scale by animateFloatAsState(
                    targetValue = scaleTarget,
                    animationSpec = tween<Float>(durationMillis = 140),
                    label = "scale"
                )

                val rotTarget = if (isDragging) (if (piece.id % 2 == 0) 1.8f else -1.8f) else 0f
                val rot by animateFloatAsState(
                    targetValue = rotTarget,
                    animationSpec = tween<Float>(durationMillis = 160),
                    label = "rot"
                )

                val elevation by animateDpAsState(
                    targetValue = if (isDragging) 18.dp else if (inTray) 6.dp else 10.dp,
                    animationSpec = tween<Dp>(durationMillis = 140),
                    label = "elev"
                )

                val absX = boardOffset.x + piece.currentX
                val absY = boardOffset.y + piece.currentY

                val wDp = with(density) { piece.width.toDp() }
                val hDp = with(density) { piece.height.toDp() }
                val shape = PuzzleShape(piece.config)

                Image(
                    bitmap = piece.bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .offset { IntOffset(absX.roundToInt(), absY.roundToInt()) }
                        .size(wDp, hDp)
                        .zIndex(if (isDragging) 2000f else piece.id.toFloat())
                        .shadow(elevation, shape, clip = false)
                        .clip(shape)
                        .border(
                            width = 2.dp,
                            color = if (piece.isLocked) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.88f),
                            shape = shape
                        )
                        .pointerInput(piece.id, piece.isLocked) {
                            if (!piece.isLocked) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggingId = piece.id
                                        viewModel.onPiecePickUp(piece.id)
                                    },
                                    onDragEnd = {
                                        viewModel.onPieceDrop(piece.id)
                                        draggingId = null
                                    },
                                    onDragCancel = {
                                        viewModel.onPieceDrop(piece.id)
                                        draggingId = null
                                    }
                                ) { _, dragAmount ->
                                    viewModel.onPieceDrag(piece.id, dragAmount.x, dragAmount.y)
                                }
                            }
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            rotationZ = rot
                        )
                )
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Text("Se încarcă...", color = Color.White)
            }
        }
    }
}

@Composable
private fun BoardBox(
    modifier: Modifier,
    themeResId: Int,
    pieces: List<PuzzlePiece>,
    onGloballyPositioned: (offset: Offset, size: Size) -> Unit
) {
    Box(
        modifier = modifier
            .shadow(14.dp, RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
            .border(2.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .onGloballyPositioned { coordinates ->
                val s = coordinates.size
                if (s.width > 0 && s.height > 0) {
                    onGloballyPositioned(coordinates.localToRoot(Offset.Zero), Size(s.width.toFloat(), s.height.toFloat()))
                }
            }
    ) {
        // Hint image: desaturat + alpha
        if (themeResId != 0) {
            val cm = remember {
                ColorMatrix().apply { setToSaturation(0.15f) }
            }
            Image(
                painter = painterResource(id = themeResId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(0.24f),
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(cm)
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.06f)))
        }

        // Grid 4x4 + outline targets
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cols = 4
            val rows = 4
            val cellW = size.width / cols
            val cellH = size.height / rows

            for (i in 1 until cols) {
                val x = i * cellW
                drawLine(
                    color = Color.White.copy(alpha = 0.18f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 2f
                )
            }
            for (j in 1 until rows) {
                val y = j * cellH
                drawLine(
                    color = Color.White.copy(alpha = 0.18f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 2f
                )
            }

            pieces.filter { !it.isLocked }.forEach { piece ->
                val outline = PuzzleShape(piece.config).createOutline(
                    size = Size(piece.width.toFloat(), piece.height.toFloat()),
                    layoutDirection = layoutDirection,
                    density = this
                )
                if (outline is Outline.Generic) {
                    val p = Path()
                    p.addPath(outline.path, piece.targetX, piece.targetY)
                    drawPath(
                        path = p,
                        color = Color.White.copy(alpha = 0.25f),
                        style = Stroke(width = 2.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrayBox(
    modifier: Modifier,
    remaining: Int,
    onShuffle: () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(10.dp, RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.20f), RoundedCornerShape(18.dp))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Piese: $remaining/16", color = Color.White.copy(alpha = 0.92f))
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onShuffle,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C))
                ) {
                    Text("Shuffle", color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Trage piesele din grilă în tablă.\nSe fixează automat când ești aproape.",
                color = Color.White.copy(alpha = 0.72f)
            )
        }
    }
}

private fun spawnSparkle(particles: MutableList<Particle>, x: Float, y: Float) {
    repeat(14) {
        val a = Random.nextFloat() * (Math.PI.toFloat() * 2f)
        val sp = 280f + Random.nextFloat() * 520f
        particles.add(
            Particle(
                x = x,
                y = y,
                vx = kotlin.math.cos(a) * sp,
                vy = kotlin.math.sin(a) * sp - 420f,
                life = 0.55f + Random.nextFloat() * 0.25f,
                size = 3f + Random.nextFloat() * 4f
            )
        )
    }
}

private fun spawnConfetti(particles: MutableList<Particle>, bx: Float, by: Float, bw: Float, bh: Float) {
    repeat(120) {
        val x = bx + Random.nextFloat() * bw
        val y = by + Random.nextFloat() * (bh * 0.25f)
        val vx = -400f + Random.nextFloat() * 800f
        val vy = -1100f + Random.nextFloat() * 300f
        particles.add(
            Particle(
                x = x,
                y = y,
                vx = vx,
                vy = vy,
                life = 0.9f + Random.nextFloat() * 0.7f,
                size = 3f + Random.nextFloat() * 5f
            )
        )
    }
}

private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier = composed {
    clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() }
    ) { onClick() }
}