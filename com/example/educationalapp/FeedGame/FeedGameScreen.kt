package com.example.educationalapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// --- NOTE: Prefix "Feed" pentru Shockwave ca sa fie unic ---

private enum class MonsterState { IDLE, EATING, PARTY }
private enum class FoodType { HEALTHY, TREAT }
private enum class FeedResult { NONE, CORRECT, WRONG }

private data class FoodDef(
    val id: Int,
    val resId: Int,
    val name: String,
    val type: FoodType
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedMonsterGame(onHome: () -> Unit = {}) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Root size
    var rootSizePx by remember { mutableStateOf(IntSize.Zero) }

    // Foods
    val foods = remember {
        listOf(
            FoodDef(1, R.drawable.food_apple, "Apple", FoodType.HEALTHY),
            FoodDef(2, R.drawable.food_broccoli, "Broccoli", FoodType.HEALTHY),
            FoodDef(3, R.drawable.food_fish, "Fish", FoodType.HEALTHY),
            FoodDef(4, R.drawable.food_cookie, "Cookie", FoodType.TREAT),
            FoodDef(5, R.drawable.food_donut, "Donut", FoodType.TREAT)
        )
    }

    var monsterFrames by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var starImage by remember { mutableStateOf<ImageBitmap?>(null) }

    var monsterState by remember { mutableStateOf(MonsterState.IDLE) }
    var monsterFrameIndex by remember { mutableIntStateOf(0) }

    val monsterScale = remember { Animatable(1f) }

    var monsterMouth by remember { mutableStateOf(Offset.Zero) }

    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var happy by remember { mutableStateOf(0f) }
    var partyTimeLeft by remember { mutableStateOf(0f) }

    var wish by remember { mutableStateOf(foods.random()) }
    var pendingWishAtNanos by remember { mutableStateOf(0L) }

    var lastResult by remember { mutableStateOf(FeedResult.NONE) }
    var lastResultAtNanos by remember { mutableStateOf(0L) }

    val particles = remember { mutableStateListOf<StarParticle>() }
    val shockwaves = remember { mutableStateListOf<FeedShockwave>() }
    var screenFlash by remember { mutableStateOf(0f) }

    var clockNanos by remember { mutableStateOf(0L) }

    val foodSizeDp = 100.dp
    val monsterSizeDp = 360.dp

    val eatThresholdPx = remember(density) { with(density) { 95.dp.toPx() } }
    val trailChance = 0.22f

    fun clamp01(v: Float) = v.coerceIn(0f, 1f)

    fun pickNextWish(currentId: Int): FoodDef {
        val pool = foods.filter { it.id != currentId }
        return pool[Random.nextInt(pool.size)]
    }

    fun spawnBurst(at: Offset, big: Boolean) {
        if (at == Offset.Zero) return
        val count = if (big) 26 else 12
        val shock = if (big) {
            FeedShockwave(center = at, radius = 0f, speed = 950f, width = 9f, alpha = 0.75f)
        } else {
            FeedShockwave(center = at, radius = 0f, speed = 680f, width = 6f, alpha = 0.55f)
        }
        shockwaves.add(shock)
        screenFlash = maxOf(screenFlash, if (big) 0.70f else 0.28f)

        repeat(count) {
            particles.add(StarParticle.burst(at, big = big))
        }
    }

    fun triggerParty(center: Offset) {
        monsterState = MonsterState.PARTY
        partyTimeLeft = 4.5f
        screenFlash = 1f
        happy = 0.12f

        val c = if (center != Offset.Zero) center else monsterMouth
        if (c != Offset.Zero) {
            shockwaves.add(FeedShockwave(center = c, radius = 0f, speed = 1150f, width = 11f, alpha = 0.9f))
            repeat(90) { particles.add(StarParticle.burst(c, big = true)) }
        }
    }

    fun onFed(food: FoodDef) {
        val correct = food.id == wish.id
        lastResult = if (correct) FeedResult.CORRECT else FeedResult.WRONG
        lastResultAtNanos = clockNanos

        monsterState = if (monsterState == MonsterState.PARTY) MonsterState.PARTY else MonsterState.EATING
        monsterFrameIndex = 12

        if (correct) {
            combo = (combo + 1).coerceAtMost(999)
            val comboBoost = 1f + (combo.coerceAtMost(10) / 10f) * 0.35f
            score += (10 + combo * 2)
            happy = clamp01(happy + 0.22f * comboBoost)

            spawnBurst(monsterMouth, big = true)
        } else {
            combo = 0
            score += 2
            val add = if (food.type == FoodType.HEALTHY) 0.10f else 0.06f
            happy = clamp01(happy + add)

            spawnBurst(monsterMouth, big = false)
        }

        pendingWishAtNanos = clockNanos + 260_000_000L

        if (happy >= 1f || combo >= 5) {
            triggerParty(monsterMouth)
            combo = 0
            pendingWishAtNanos = clockNanos + 650_000_000L
        }
    }

    fun resetGame() {
        score = 0
        combo = 0
        happy = 0f
        partyTimeLeft = 0f
        monsterState = MonsterState.IDLE
        monsterFrameIndex = 0
        wish = foods.random()
        pendingWishAtNanos = 0L
        lastResult = FeedResult.NONE
        lastResultAtNanos = 0L
        particles.clear()
        shockwaves.clear()
        screenFlash = 0f
    }

    LaunchedEffect(Unit) {
        val (frames, star) = withContext(Dispatchers.Default) {
            val sheet = withContext(Dispatchers.IO) {
                BitmapFactory.decodeResource(context.resources, R.drawable.monster_sheet)
            }
            val starBmp = withContext(Dispatchers.IO) {
                BitmapFactory.decodeResource(context.resources, R.drawable.vfx_star)
            }
            val f = splitSpriteSheet(sheet, rows = 4, cols = 6, safetyCrop = true).map { it.asImageBitmap() }
            Pair(f, starBmp.asImageBitmap())
        }
        monsterFrames = frames
        starImage = star
    }

    LaunchedEffect(Unit) {
        var lastFrame = 0L
        var frameAcc = 0f
        var partySpawnAcc = 0f
        var danceAcc = 0f

        while (isActive) {
            val now = withFrameNanos { it }
            clockNanos = now
            if (lastFrame == 0L) {
                lastFrame = now
                continue
            }
            val dt = ((now - lastFrame).toDouble() / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
            lastFrame = now

            if (pendingWishAtNanos > 0L && now >= pendingWishAtNanos) {
                wish = pickNextWish(wish.id)
                pendingWishAtNanos = 0L
                lastResult = FeedResult.NONE
            }

            screenFlash = (screenFlash - dt * 1.65f).coerceIn(0f, 1f)

            if (partyTimeLeft > 0f) {
                partyTimeLeft -= dt
                if (partyTimeLeft <= 0f) {
                    partyTimeLeft = 0f
                    if (monsterState == MonsterState.PARTY) {
                        monsterState = MonsterState.IDLE
                        monsterFrameIndex = 0
                    }
                }
            }

            if (monsterState == MonsterState.PARTY) {
                partySpawnAcc += dt
                val widthPx = rootSizePx.width.toFloat().coerceAtLeast(1f)
                while (partySpawnAcc >= 0.06f) {
                    partySpawnAcc -= 0.06f
                    particles.add(StarParticle.confetti(widthPx))
                }
                danceAcc += dt
                val dance = 1f + 0.08f * abs(sin(danceAcc * 7.0f))
                if (monsterScale.value != dance) {
                    monsterScale.snapTo(dance)
                }
            } else {
                if (abs(monsterScale.value - 1f) > 0.01f) {
                    monsterScale.snapTo(1f)
                }
            }

            if (monsterFrames.isNotEmpty()) {
                frameAcc += dt
                val frameDuration = when (monsterState) {
                    MonsterState.IDLE -> 0.10f
                    MonsterState.EATING -> 0.07f
                    MonsterState.PARTY -> 0.06f
                }
                while (frameAcc >= frameDuration) {
                    when (monsterState) {
                        MonsterState.IDLE -> {
                            if (monsterFrameIndex < 0 || monsterFrameIndex > 11) monsterFrameIndex = 0
                            monsterFrameIndex++
                            if (monsterFrameIndex > 11) monsterFrameIndex = 0
                        }
                        MonsterState.EATING -> {
                            if (monsterFrameIndex < 12 || monsterFrameIndex > 23) monsterFrameIndex = 12
                            monsterFrameIndex++
                            if (monsterFrameIndex > 23) {
                                monsterState = MonsterState.IDLE
                                monsterFrameIndex = 0
                            }
                        }
                        MonsterState.PARTY -> {
                            if (monsterFrameIndex < 12 || monsterFrameIndex > 23) monsterFrameIndex = 12
                            monsterFrameIndex++
                            if (monsterFrameIndex > 23) monsterFrameIndex = 12
                        }
                    }
                    frameAcc -= frameDuration
                }
            }

            for (i in shockwaves.size - 1 downTo 0) {
                val s = shockwaves[i]
                s.update(dt)
                if (!s.alive) shockwaves.removeAt(i)
            }

            val maxParticles = if (monsterState == MonsterState.PARTY) 720 else 380
            if (particles.size > maxParticles) {
                val rm = particles.size - maxParticles
                repeat(rm.coerceAtMost(particles.size)) { particles.removeAt(0) }
            }

            for (i in particles.size - 1 downTo 0) {
                val p = particles[i]
                p.update(dt)
                if (!p.alive) particles.removeAt(i)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootSizePx = it.size },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.game_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        TopHud(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp, start = 12.dp, end = 12.dp),
            score = score,
            combo = combo,
            happy = clamp01(happy),
            wish = wish,
            lastResult = lastResult,
            lastResultAtNanos = lastResultAtNanos
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 34.dp)
                .size(monsterSizeDp)
                .scale(monsterScale.value)
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInRoot()
                    val w = coords.size.width.toFloat()
                    val h = coords.size.height.toFloat()
                    monsterMouth = Offset(
                        x = pos.x + w * 0.58f,
                        y = pos.y + h * 0.42f
                    )
                }
                .combinedClickable(
                    onClick = {
                        if (monsterState == MonsterState.PARTY && monsterMouth != Offset.Zero) {
                            spawnBurst(monsterMouth, big = true)
                        }
                    },
                    onDoubleClick = { resetGame() }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (monsterFrames.isNotEmpty()) {
                Image(
                    bitmap = monsterFrames[monsterFrameIndex.coerceIn(0, monsterFrames.lastIndex)],
                    contentDescription = "Monster",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("Loading...", color = Color.White)
            }

            if (monsterMouth != Offset.Zero) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 14.dp, top = 16.dp)
                        .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Wants:",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = wish.resId),
                                contentDescription = wish.name,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            foods.forEach { food ->
                DraggableFoodIcon(
                    food = food,
                    sizeDp = foodSizeDp,
                    mouthPosition = monsterMouth,
                    thresholdPx = eatThresholdPx,
                    enabled = (monsterFrames.isNotEmpty()),
                    onFed = { onFed(food) },
                    emitTrail = { pos ->
                        if (Random.nextFloat() < trailChance) {
                            particles.add(StarParticle.trail(pos))
                        }
                    }
                )
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val star = starImage

            if (monsterMouth != Offset.Zero) {
                val glowA = (0.04f + happy * 0.08f + if (monsterState == MonsterState.PARTY) 0.10f else 0f).coerceIn(0f, 0.25f)
                drawCircle(
                    color = Color.White.copy(alpha = glowA),
                    radius = 95f + 40f * happy,
                    center = monsterMouth
                )
            }

            shockwaves.forEach { it.draw(this) }
            particles.forEach { it.draw(this, star) }
        }

        if (screenFlash > 0f) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha((screenFlash * 0.33f).coerceIn(0f, 0.33f))
            ) {
                drawRect(Color.White)
            }
        }

        if (monsterState == MonsterState.PARTY) {
            Text(
                text = "PARTY!",
                color = Color.White,
                fontSize = 46.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 240.dp)
            )
        }

        Image(
            painter = painterResource(id = R.drawable.ui_button_home),
            contentDescription = "Home",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(64.dp)
                .clickable { onHome() }
        )
    }
}

@Composable
private fun TopHud(
    modifier: Modifier,
    score: Int,
    combo: Int,
    happy: Float,
    wish: FoodDef,
    lastResult: FeedResult,
    lastResultAtNanos: Long
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Score: $score",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (combo > 0) "Combo: x$combo" else "Combo: -",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

            Text(
                text = "Drag food to mouth",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Canvas(modifier = Modifier.fillMaxWidth().height(14.dp)) {
            drawRect(Color.Black.copy(alpha = 0.25f), size = size)
            val w = size.width * happy.coerceIn(0f, 1f)
            drawRect(Color.White.copy(alpha = 0.85f), size = size.copy(width = w))
            drawRect(Color.White.copy(alpha = 0.25f), size = size, style = Stroke(width = 2f))
        }

        val show = lastResult != FeedResult.NONE && (System.nanoTime() - lastResultAtNanos) < 900_000_000L
        if (show) {
            Spacer(modifier = Modifier.height(8.dp))
            val (txt, col) = when (lastResult) {
                FeedResult.CORRECT -> "Yummy!" to Color(0xFF00E5FF)
                FeedResult.WRONG -> "Not what I wanted!" to Color(0xFFFFEB3B)
                else -> "" to Color.Transparent
            }
            Text(
                text = txt,
                color = col,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun DraggableFoodIcon(
    food: FoodDef,
    sizeDp: androidx.compose.ui.unit.Dp,
    mouthPosition: Offset,
    thresholdPx: Float,
    enabled: Boolean,
    onFed: () -> Unit,
    emitTrail: (Offset) -> Unit
) {
    val density = LocalDensity.current
    val halfPx = remember(density, sizeDp) { with(density) { sizeDp.toPx() } * 0.5f }

    var startPosRoot by remember { mutableStateOf(Offset.Zero) }
    
    // Explicit type for Animatable to avoid inference errors
    val offset = remember { 
        Animatable(Offset.Zero, Offset.VectorConverter) 
    }
    val scale = remember { Animatable(1f) }

    // External scope for animations that must not block gestures
    val scope = rememberCoroutineScope()

    var visible by remember { mutableStateOf(true) }

    val bobPhase = remember { Random.nextFloat() * (2f * PI.toFloat()) }
    val bob = 1f + 0.03f * abs(sin((System.nanoTime() / 1_000_000_000.0).toFloat() * 1.2f + bobPhase))

    if (!visible) {
        Spacer(modifier = Modifier.size(sizeDp))
        return
    }

    Image(
        painter = painterResource(id = food.resId),
        contentDescription = food.name,
        modifier = Modifier
            .size(sizeDp)
            .scale(scale.value * bob)
            .onGloballyPositioned { coords ->
                startPosRoot = coords.positionInRoot()
            }
            .offset {
                androidx.compose.ui.unit.IntOffset(
                    x = offset.value.x.roundToInt(),
                    y = offset.value.y.roundToInt()
                )
            }
            .pointerInput(enabled, mouthPosition, startPosRoot) {
                if (!enabled) return@pointerInput

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    
                    // Launch animation in external scope (non-blocking)
                    scope.launch { scale.animateTo(1.10f, tween(80)) }

                    val pointerId = down.id

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) break

                        val delta = change.positionChange()
                        if (delta != Offset.Zero) {
                            change.consume()
                            
                            // Use scope.launch to call suspend function snapTo from restricted scope
                            scope.launch {
                                offset.snapTo(offset.value + delta)
                            }

                            if (startPosRoot != Offset.Zero && mouthPosition != Offset.Zero) {
                                val center = Offset(
                                    x = startPosRoot.x + offset.value.x + halfPx,
                                    y = startPosRoot.y + offset.value.y + halfPx
                                )
                                emitTrail(center)
                            }
                        }
                    }

                    // Release logic
                    scope.launch {
                        scale.animateTo(1f, tween(90))
                        
                        if (mouthPosition == Offset.Zero || startPosRoot == Offset.Zero) {
                            offset.animateTo(Offset.Zero, tween(220))
                        } else {
                            val center = Offset(
                                x = startPosRoot.x + offset.value.x + halfPx,
                                y = startPosRoot.y + offset.value.y + halfPx
                            )
                            val dx = center.x - mouthPosition.x
                            val dy = center.y - mouthPosition.y
                            val dist = sqrt(dx * dx + dy * dy)

                            if (dist <= thresholdPx) {
                                onFed()
                                val targetOffset = Offset(
                                    x = (mouthPosition.x - startPosRoot.x - halfPx),
                                    y = (mouthPosition.y - startPosRoot.y - halfPx)
                                )
                                // Animation sequence on external scope
                                offset.animateTo(targetOffset, tween(140))
                                scale.animateTo(0.10f, tween(140))
                                
                                offset.snapTo(Offset.Zero)
                                scale.snapTo(1f)
                                visible = false
                                delay(650)
                                visible = true
                            } else {
                                offset.animateTo(Offset.Zero, tween(220))
                            }
                        }
                    }
                }
            }
    )
}

private fun splitSpriteSheet(
    sheet: Bitmap,
    rows: Int,
    cols: Int,
    safetyCrop: Boolean
): List<Bitmap> {
    if (rows <= 0 || cols <= 0) return emptyList()
    val frameW = sheet.width / cols
    val frameH = sheet.height / rows
    if (frameW <= 0 || frameH <= 0) return emptyList()

    val crop = if (safetyCrop) 1 else 0
    val out = ArrayList<Bitmap>(rows * cols)
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val x = c * frameW + crop
            val y = r * frameH + crop
            val w = (frameW - crop * 2).coerceAtLeast(1)
            val h = (frameH - crop * 2).coerceAtLeast(1)
            out.add(Bitmap.createBitmap(sheet, x.coerceAtLeast(0), y.coerceAtLeast(0), w, h))
        }
    }
    return out
}

private class StarParticle private constructor(
    private var x: Float,
    private var y: Float,
    private var vx: Float,
    private var vy: Float,
    private var rotationDeg: Float,
    private var rotationSpeedDeg: Float,
    private var sizePx: Float,
    private var life: Float,
    private var age: Float,
    private var color: Color,
    private var gravity: Float,
    private var drag: Float
) {
    var alive: Boolean = true
        private set

    fun update(dt: Float) {
        if (!alive) return
        age += dt
        if (age >= life) {
            alive = false
            return
        }

        val dragFactor = (1f - drag * dt).coerceIn(0f, 1f)
        vx *= dragFactor
        vy = (vy + gravity * dt) * dragFactor
        x += vx * dt
        y += vy * dt
        rotationDeg += rotationSpeedDeg * dt
    }

    fun draw(scope: DrawScope, star: ImageBitmap?) {
        if (!alive) return
        val t = (age / life).coerceIn(0f, 1f)
        val alpha = (1f - t).coerceIn(0f, 1f)

        if (star == null) {
            scope.drawCircle(
                color = color.copy(alpha = alpha),
                radius = (sizePx * 0.25f).coerceAtLeast(2f),
                center = Offset(x, y)
            )
            return
        }

        val base = star.width.toFloat().coerceAtLeast(1f)
        val scale = (sizePx / base).coerceIn(0.12f, 2.8f)
        val half = (base * scale) * 0.5f

        scope.withTransform({
            translate(x - half, y - half)
            rotate(rotationDeg, pivot = Offset(half, half))
            scale(scale, scale, pivot = Offset(half, half))
        }) {
            drawImage(
                image = star,
                topLeft = Offset.Zero,
                alpha = alpha,
                colorFilter = ColorFilter.tint(color)
            )
        }

        if (alpha > 0.25f) {
            scope.drawCircle(
                color = Color.White.copy(alpha = alpha * 0.22f),
                radius = sizePx * 0.22f,
                center = Offset(x, y),
                style = Stroke(width = 2f)
            )
        }
    }

    companion object {
        private val palette = listOf(
            Color(0xFFFFEB3B), Color(0xFF00E5FF), Color(0xFFFF4081),
            Color(0xFF69F0AE), Color(0xFFB388FF), Color(0xFFFF9800), Color(0xFFFFFFFF)
        )

        fun burst(origin: Offset, big: Boolean): StarParticle {
            val power = if (big) 1.45f else 1.0f
            val angle = Random.nextDouble(0.0, 2.0 * PI).toFloat()
            val speed = (220f + Random.nextFloat() * 720f) * power
            val vx = cos(angle) * speed
            val vy = sin(angle) * speed - (360f * power)
            val size = (14f + Random.nextFloat() * 22f) * power
            val life = 0.9f + Random.nextFloat() * 0.9f
            return StarParticle(
                x = origin.x, y = origin.y, vx = vx, vy = vy,
                rotationDeg = Random.nextFloat() * 360f,
                rotationSpeedDeg = (-520f + Random.nextFloat() * 1040f),
                sizePx = size, life = life, age = 0f,
                color = palette.random(), gravity = 920f, drag = 0.08f
            )
        }

        fun trail(pos: Offset): StarParticle {
            val vx = (-70f + Random.nextFloat() * 140f)
            val vy = (-190f - Random.nextFloat() * 240f)
            val size = 10f + Random.nextFloat() * 10f
            val life = 0.35f + Random.nextFloat() * 0.35f
            return StarParticle(
                x = pos.x, y = pos.y, vx = vx, vy = vy,
                rotationDeg = Random.nextFloat() * 360f,
                rotationSpeedDeg = (-260f + Random.nextFloat() * 520f),
                sizePx = size, life = life, age = 0f,
                color = palette.random(), gravity = 520f, drag = 0.18f
            )
        }

        fun confetti(rootWidthPx: Float): StarParticle {
            val x = Random.nextFloat() * rootWidthPx
            val y = -40f
            val vx = (-90f + Random.nextFloat() * 180f)
            val vy = (90f + Random.nextFloat() * 260f)
            val size = 12f + Random.nextFloat() * 18f
            val life = 1.2f + Random.nextFloat() * 1.0f
            return StarParticle(
                x = x, y = y, vx = vx, vy = vy,
                rotationDeg = Random.nextFloat() * 360f,
                rotationSpeedDeg = (-360f + Random.nextFloat() * 720f),
                sizePx = size, life = life, age = 0f,
                color = palette.random(), gravity = 980f, drag = 0.06f
            )
        }
    }
}

private class FeedShockwave(
    private val center: Offset,
    private var radius: Float,
    private val speed: Float,
    private val width: Float,
    private var alpha: Float
) {
    var alive: Boolean = true
        private set

    fun update(dt: Float) {
        if (!alive) return
        radius += speed * dt
        alpha -= dt * 0.90f
        if (alpha <= 0f) alive = false
    }

    fun draw(scope: DrawScope) {
        if (!alive) return
        scope.drawCircle(
            color = Color.White.copy(alpha = alpha.coerceIn(0f, 1f)),
            radius = radius.coerceAtLeast(0f),
            center = center,
            style = Stroke(width = width.coerceAtLeast(1f))
        )
    }
}