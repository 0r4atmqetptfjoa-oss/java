package com.example.educationalapp

import com.example.educationalapp.R
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// Stările jocului (păstrate compatibil cu codul vechi)
enum class EggGameState {
    EGG_INTACT,
    EGG_CRACK_1,
    EGG_CRACK_2,
    EGG_BROKEN,
    DRAGON_APPEARED
}

private enum class HitQuality(val label: String, val color: Color) {
    PERFECT("Perfect!", Color(0xFF00E5FF)),
    GOOD("Good!", Color(0xFFFFEB3B)),
    MISS("Oops!", Color(0xFFFF5252)),
    NONE("", Color.Transparent)
}

private enum class DragonMode { RISING, IDLE, CELEBRATE }

/**
 * EggSurpriseGame - UPGRADED:
 * - NU mai folosim SpriteTools
 * - NU mai facem removeBackground (PNG-urile sunt presupuse transparente)
 * - mecanică "tap pe puls": Perfect/Good/Miss + combo + Hatch Meter
 * - hatch cinematic: flash + shockwave + burst shell + sparkles + raze
 * - VFX randate pe Canvas (performant)
 * - loop frame-synced (withFrameNanos) + dt capped
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EggSurpriseGame(onHome: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- CONFIG ---
    val bpm = 120f
    val beatPeriodNanos = remember(bpm) { (60_000_000_000f / bpm).toLong().coerceAtLeast(1L) }

    // --- ROOT SIZE ---
    var rootSizePx by remember { mutableStateOf(IntSize.Zero) }

    // --- GAME STATE ---
    var gameState by remember { mutableStateOf(EggGameState.EGG_INTACT) }

    var hatchProgress by remember { mutableStateOf(0f) } // 0..1
    var combo by remember { mutableIntStateOf(0) }

    var lastHit by remember { mutableStateOf(HitQuality.NONE) }
    var lastHitAtNanos by remember { mutableStateOf(0L) }
    var lastTapAtNanos by remember { mutableStateOf(0L) }

    // Egg shake/feedback
    val eggRotate = remember { Animatable(0f) }
    val eggScale = remember { Animatable(1f) }

    // --- DRAGON STATE ---
    var dragonFrames by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var dragonFrameIndex by remember { mutableIntStateOf(0) }
    var dragonMode by remember { mutableStateOf(DragonMode.IDLE) }
    var dragonRise by remember { mutableStateOf(0f) }            // 0..1
    var celebrateLeft by remember { mutableStateOf(0f) }         // seconds

    // --- VFX ---
    val particles = remember { mutableStateListOf<EggParticle>() }
    val shockwaves = remember { mutableStateListOf<Shockwave>() }
    var shellImage by remember { mutableStateOf<ImageBitmap?>(null) }

    var screenFlash by remember { mutableStateOf(0f) } // 0..1

    // --- CLOCK ---
    var clockNanos by remember { mutableStateOf(0L) }
    var lastBeatIndex by remember { mutableStateOf(-1L) }

    val beatPhase: Float by derivedStateOf {
        if (clockNanos <= 0L) 0f
        else ((clockNanos % beatPeriodNanos).toDouble() / beatPeriodNanos.toDouble()).toFloat()
    }

    // --- ANCHORS (root coords) ---
    var eggAnchor by remember { mutableStateOf(Offset.Zero) }     // locul de "crack"
    var dragonAnchor by remember { mutableStateOf(Offset.Zero) }  // locul de spawn pentru VFX pe dragon

    // Egg stage resources (nu decode, doar painterResource)
    val eggStageRes = remember {
        listOf(
            R.drawable.egg_0_intact,
            R.drawable.egg_1_crack,
            R.drawable.egg_2_crack,
            R.drawable.egg_3_broken
        )
    }

    fun clamp01(x: Float) = x.coerceIn(0f, 1f)

    fun eggStageIndexFromProgress(p: Float): Int {
        val v = clamp01(p)
        return when {
            v < 0.34f -> 0
            v < 0.66f -> 1
            v < 0.92f -> 2
            else -> 3
        }
    }

    fun stateFromProgress(p: Float): EggGameState {
        val idx = eggStageIndexFromProgress(p)
        return when (idx) {
            0 -> EggGameState.EGG_INTACT
            1 -> EggGameState.EGG_CRACK_1
            2 -> EggGameState.EGG_CRACK_2
            else -> EggGameState.EGG_BROKEN
        }
    }

    fun evaluateHit(nowNanos: Long): HitQuality {
        if (nowNanos <= 0L) return HitQuality.NONE
        val phase = (nowNanos % beatPeriodNanos).toDouble() / beatPeriodNanos.toDouble() // 0..1
        val distToBeat = min(phase, 1.0 - phase) * beatPeriodNanos.toDouble()
        val distMs = distToBeat / 1_000_000.0
        return when {
            distMs <= 65.0 -> HitQuality.PERFECT
            distMs <= 140.0 -> HitQuality.GOOD
            else -> HitQuality.MISS
        }
    }

    fun spawnTapVfx(origin: Offset, quality: HitQuality, comboNow: Int) {
        if (origin == Offset.Zero) return

        val comboBoost = (comboNow.coerceAtMost(12) / 12f)
        val intensity = when (quality) {
            HitQuality.PERFECT -> 1.0f
            HitQuality.GOOD -> 0.65f
            HitQuality.MISS -> 0.35f
            HitQuality.NONE -> 0f
        }

        // micro flash + shockwave
        screenFlash = maxOf(screenFlash, 0.18f + intensity * 0.35f)
        shockwaves.add(
            Shockwave(
                center = origin,
                radius = 0f,
                speed = 700f + 300f * intensity,
                width = 6f + 3f * intensity,
                alpha = 0.65f + 0.2f * intensity
            )
        )

        // shell burst + sparkles
        val shells = (8 + intensity * 12f + comboBoost * 6f).roundToInt().coerceIn(6, 28)
        val sparkles = (10 + intensity * 16f + comboBoost * 8f).roundToInt().coerceIn(8, 40)

        repeat(shells) { particles.add(EggParticle.shellBurst(origin, power = 0.9f + intensity, comboBoost = comboBoost)) }
        repeat(sparkles) { particles.add(EggParticle.sparkle(origin, power = 0.7f + intensity, comboBoost = comboBoost)) }
    }

    fun triggerHatch() {
        if (gameState == EggGameState.DRAGON_APPEARED) return
        gameState = EggGameState.DRAGON_APPEARED

        dragonMode = DragonMode.RISING
        dragonRise = 0f
        celebrateLeft = 0f
        dragonFrameIndex = 12 // start pe range-ul energic

        // Cinematic burst
        val origin = if (eggAnchor != Offset.Zero) eggAnchor else Offset(rootSizePx.width * 0.5f, rootSizePx.height * 0.55f)
        screenFlash = 1f
        shockwaves.add(Shockwave(center = origin, radius = 0f, speed = 1050f, width = 10f, alpha = 0.9f))

        repeat(70) { particles.add(EggParticle.shellBurst(origin, power = 1.8f, comboBoost = 1f)) }
        repeat(90) { particles.add(EggParticle.sparkle(origin, power = 1.9f, comboBoost = 1f)) }
    }

    fun resetGame() {
        gameState = EggGameState.EGG_INTACT
        hatchProgress = 0f
        combo = 0
        lastHit = HitQuality.NONE
        lastHitAtNanos = 0L
        lastTapAtNanos = 0L

        dragonMode = DragonMode.IDLE
        dragonRise = 0f
        celebrateLeft = 0f
        dragonFrameIndex = 0

        particles.clear()
        shockwaves.clear()
        screenFlash = 0f

        scope.launch {
            eggRotate.snapTo(0f)
            eggScale.snapTo(1f)
        }
    }

    fun onEggTap() {
        if (gameState == EggGameState.DRAGON_APPEARED) return

        val now = clockNanos
        val quality = evaluateHit(now)

        lastHit = quality
        lastHitAtNanos = now
        lastTapAtNanos = now

        // Shake + bounce (non-blocking)
        scope.launch {
            eggScale.snapTo(1f)
            eggScale.animateTo(0.93f, tween(60))
            eggScale.animateTo(1f, tween(90))
        }
        scope.launch {
            eggRotate.snapTo(0f)
            eggRotate.animateTo(7f, tween(45))
            eggRotate.animateTo(-7f, tween(65))
            eggRotate.animateTo(0f, tween(60))
        }

        // Combo
        combo = when (quality) {
            HitQuality.PERFECT, HitQuality.GOOD -> (combo + 1).coerceAtMost(999)
            HitQuality.MISS -> 0
            HitQuality.NONE -> combo
        }

        // Hatch progress (kid-friendly: Miss încă avansează un pic)
        val comboBoost = 1f + (combo.coerceAtMost(10) / 10f) * 0.35f
        val add = when (quality) {
            HitQuality.PERFECT -> 0.22f
            HitQuality.GOOD -> 0.15f
            HitQuality.MISS -> 0.06f
            HitQuality.NONE -> 0f
        } * comboBoost

        hatchProgress = (hatchProgress + add).coerceIn(0f, 1.15f)
        gameState = stateFromProgress(hatchProgress)

        // VFX
        spawnTapVfx(eggAnchor, quality, combo)

        // Hatch
        if (hatchProgress >= 1f) triggerHatch()
    }

    fun onDragonTap() {
        // Tap = celebrate (VFX + mod energic)
        if (gameState != EggGameState.DRAGON_APPEARED) return

        dragonMode = DragonMode.CELEBRATE
        celebrateLeft = 2.0f

        val origin = if (dragonAnchor != Offset.Zero) dragonAnchor else eggAnchor
        if (origin != Offset.Zero) {
            screenFlash = maxOf(screenFlash, 0.55f)
            shockwaves.add(Shockwave(center = origin, radius = 0f, speed = 980f, width = 9f, alpha = 0.75f))
            repeat(26) { particles.add(EggParticle.sparkle(origin, power = 1.35f, comboBoost = 1f)) }
            repeat(18) { particles.add(EggParticle.shellBurst(origin, power = 1.15f, comboBoost = 1f)) }
        }
    }

    // --- LOAD RESOURCES (off-main) ---
    LaunchedEffect(Unit) {
        val (frames, shell) = withContext(Dispatchers.Default) {
            val sheet = withContext(Dispatchers.IO) {
                BitmapFactory.decodeResource(context.resources, R.drawable.dragon_sheet)
            }
            val shellBmp = withContext(Dispatchers.IO) {
                BitmapFactory.decodeResource(context.resources, R.drawable.vfx_shell_piece)
            }
            val f = splitSpriteSheet(sheet, rows = 4, cols = 6, safetyCrop = true).map { it.asImageBitmap() }
            Pair(f, shellBmp.asImageBitmap())
        }
        dragonFrames = frames
        shellImage = shell
    }

    // --- MAIN LOOP (frame-synced) ---
    LaunchedEffect(Unit) {
        var lastFrame = 0L
        var frameAcc = 0f

        while (isActive) {
            val now = withFrameNanos { it }
            clockNanos = now

            if (lastFrame == 0L) {
                lastFrame = now
                lastBeatIndex = now / beatPeriodNanos
                continue
            }

            val dt = ((now - lastFrame).toDouble() / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
            lastFrame = now

            // Beat tick
            val beatIndex = now / beatPeriodNanos
            if (beatIndex != lastBeatIndex) {
                lastBeatIndex = beatIndex

                // ambient magic pulse (înainte de hatch)
                if (gameState != EggGameState.DRAGON_APPEARED && eggAnchor != Offset.Zero) {
                    val extra = if (combo >= 5) 7 else 4
                    repeat(extra) { particles.add(EggParticle.sparkleAmbient(eggAnchor, comboBoost = (combo.coerceAtMost(10) / 10f))) }

                    // mic bonus de progres când ești pe val (nu rupe kid-friendly)
                    if (combo >= 6 && hatchProgress < 1f) {
                        hatchProgress = (hatchProgress + 0.01f).coerceAtMost(1f)
                        gameState = stateFromProgress(hatchProgress)
                        if (hatchProgress >= 1f) triggerHatch()
                    }
                }

                // dacă dragonul e ieșit, pe beat mai aruncăm sparkles
                if (gameState == EggGameState.DRAGON_APPEARED) {
                    val origin = if (dragonAnchor != Offset.Zero) dragonAnchor else eggAnchor
                    if (origin != Offset.Zero) {
                        repeat(6) { particles.add(EggParticle.sparkle(origin, power = 0.85f, comboBoost = 1f)) }
                    }
                }
            }

            // Auto reset combo dacă nu atingi 3s (mai “curat”)
            if (combo > 0 && lastTapAtNanos > 0L && (now - lastTapAtNanos) > 3_000_000_000L) {
                combo = 0
                lastHit = HitQuality.NONE
            }

            // Screen flash decay
            screenFlash = (screenFlash - dt * 1.8f).coerceIn(0f, 1f)

            // Dragon mode updates
            when (dragonMode) {
                DragonMode.RISING -> {
                    dragonRise = (dragonRise + dt / 0.85f).coerceIn(0f, 1f)
                    if (dragonRise >= 1f) {
                        dragonMode = DragonMode.IDLE
                        dragonFrameIndex = 0
                    }
                }
                DragonMode.CELEBRATE -> {
                    celebrateLeft -= dt
                    if (celebrateLeft <= 0f) {
                        dragonMode = DragonMode.IDLE
                        dragonFrameIndex = 0
                        celebrateLeft = 0f
                    }
                }
                DragonMode.IDLE -> Unit
            }

            // Dragon frame animation
            if (dragonFrames.isNotEmpty() && gameState == EggGameState.DRAGON_APPEARED) {
                frameAcc += dt
                val frameDuration = when (dragonMode) {
                    DragonMode.IDLE -> 0.10f
                    DragonMode.RISING -> 0.07f
                    DragonMode.CELEBRATE -> 0.06f
                }
                while (frameAcc >= frameDuration) {
                    advanceDragonFrame(
                        mode = dragonMode,
                        current = dragonFrameIndex
                    ) { dragonFrameIndex = it }
                    frameAcc -= frameDuration
                }
            }

            // Shockwaves
            for (i in shockwaves.size - 1 downTo 0) {
                val s = shockwaves[i]
                s.update(dt)
                if (!s.alive) shockwaves.removeAt(i)
            }

            // Particles (limit defensiv)
            val maxParticles = if (gameState == EggGameState.DRAGON_APPEARED) 520 else 380
            if (particles.size > maxParticles) {
                val removeCount = particles.size - maxParticles
                repeat(removeCount.coerceAtMost(particles.size)) { particles.removeAt(0) }
            }

            for (i in particles.size - 1 downTo 0) {
                val p = particles[i]
                p.update(dt)
                if (!p.alive) particles.removeAt(i)
            }
        }
    }

    // --- UI ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootSizePx = it.size },
        contentAlignment = Alignment.Center
    ) {
        // A. Background
        Image(
            painter = painterResource(id = R.drawable.bg_magic_forest),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // B. HUD (Beat + Hatch meter)
        HudTop(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp, start = 12.dp, end = 12.dp),
            beatPhase = beatPhase,
            progress = clamp01(hatchProgress),
            combo = combo,
            isHatched = (gameState == EggGameState.DRAGON_APPEARED)
        )

        // C. Scene center
        val eggStageIdx = eggStageIndexFromProgress(hatchProgress)
        val eggResId = eggStageRes[eggStageIdx.coerceIn(0, eggStageRes.lastIndex)]

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            val pulse = 0.96f + 0.08f * abs(sin(beatPhase * 2f * PI)).toFloat()
            val glow = (0.10f + clamp01(hatchProgress) * 0.22f).coerceIn(0f, 0.35f)

            // Aura ring (în spate) desenat în root Canvas (mai jos), dar aici punem și un mic overlay textual
            if (gameState != EggGameState.DRAGON_APPEARED) {
                Image(
                    painter = painterResource(id = eggResId),
                    contentDescription = "Egg",
                    modifier = Modifier
                        .size(320.dp)
                        .scale(eggScale.value * pulse)
                        .rotate(eggRotate.value)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onEggTap() },
                            onDoubleClick = {
                                // optional: un mic boost resetabil (kid-friendly) - double tap = micro burst
                                val origin = if (eggAnchor != Offset.Zero) eggAnchor else Offset(rootSizePx.width * 0.5f, rootSizePx.height * 0.55f)
                                screenFlash = maxOf(screenFlash, 0.28f)
                                shockwaves.add(Shockwave(center = origin, radius = 0f, speed = 780f, width = 7f, alpha = 0.7f))
                                repeat(10) { particles.add(EggParticle.sparkle(origin, power = 0.95f, comboBoost = 0.5f)) }
                            }
                        )
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInRoot()
                            val sz = coords.size
                            // Anchor: zona de “crack” (ușor mai sus de centru)
                            eggAnchor = Offset(
                                x = pos.x + sz.width * 0.52f,
                                y = pos.y + sz.height * 0.48f
                            )
                        }
                )
            } else {
                // Păstrăm oul "broken" ca bază
                Image(
                    painter = painterResource(id = R.drawable.egg_3_broken),
                    contentDescription = "Egg broken",
                    modifier = Modifier
                        .size(320.dp)
                        .scale(1f + glow * 0.4f)
                        .alpha(0.95f)
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInRoot()
                            val sz = coords.size
                            eggAnchor = Offset(pos.x + sz.width * 0.52f, pos.y + sz.height * 0.48f)
                        }
                )

                // Dragon (rising + tap/double tap)
                if (dragonFrames.isNotEmpty()) {
                    val yOff = ((1f - dragonRise) * 90f).roundToInt()
                    val a = if (dragonMode == DragonMode.RISING) dragonRise else 1f
                    val sc = 0.88f + 0.12f * dragonRise

                    Image(
                        bitmap = dragonFrames[dragonFrameIndex.coerceIn(0, dragonFrames.lastIndex)],
                        contentDescription = "Dragon",
                        modifier = Modifier
                            .size(380.dp)
                            .offset(y = yOff.dp)
                            .alpha(a)
                            .scale(sc)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onDragonTap() },
                                onDoubleClick = { resetGame() }
                            )
                            .onGloballyPositioned { coords ->
                                val pos = coords.positionInRoot()
                                val sz = coords.size
                                // Anchor: pieptul dragonului (centru)
                                dragonAnchor = Offset(
                                    x = pos.x + sz.width * 0.52f,
                                    y = pos.y + sz.height * 0.52f
                                )
                            }
                    )
                } else {
                    Text("Loading...", color = Color.White)
                }
            }

            // Mesaj mic de feedback (Perfect/Good/Miss)
            val showHit = lastHit != HitQuality.NONE && (System.nanoTime() - lastHitAtNanos) < 900_000_000L
            if (showHit && gameState != EggGameState.DRAGON_APPEARED) {
                Text(
                    text = lastHit.label,
                    color = lastHit.color,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                )
            }
        }

        // D. VFX Layer (Canvas)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val shell = shellImage
            val timeSec = (clockNanos / 1_000_000_000.0).toFloat()

            // Aura ring around egg (când nu e hatched)
            if (gameState != EggGameState.DRAGON_APPEARED && eggAnchor != Offset.Zero) {
                val p = clamp01(hatchProgress)
                val r = 105f + 22f * abs(sin(beatPhase * 2f * PI)).toFloat() + p * 35f
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f + p * 0.10f),
                    radius = r,
                    center = eggAnchor,
                    style = Stroke(width = 6f)
                )
            }

            // Rays behind dragon (când e hatched)
            if (gameState == EggGameState.DRAGON_APPEARED) {
                val center = if (dragonAnchor != Offset.Zero) dragonAnchor else eggAnchor
                if (center != Offset.Zero) {
                    val rays = 14
                    val baseLen = 190f + 40f * abs(sin(timeSec * 1.1f))
                    val aBase = 0.05f + 0.06f * abs(sin(timeSec * 0.9f))
                    val rot = timeSec * 0.55f
                    for (i in 0 until rays) {
                        val ang = (i.toFloat() * (2f * PI.toFloat() / rays)) + rot
                        val dx = cos(ang) * baseLen
                        val dy = sin(ang) * baseLen
                        drawLine(
                            color = Color.White.copy(alpha = aBase),
                            start = center,
                            end = center + Offset(dx, dy),
                            strokeWidth = 10f
                        )
                    }
                }
            }

            // Shockwaves
            shockwaves.forEach { it.draw(this) }

            // Particles
            particles.forEach { it.draw(this, shell) }
        }

        // E. Screen flash
        if (screenFlash > 0f) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha((screenFlash * 0.33f).coerceIn(0f, 0.33f))
            ) {
                drawRect(Color.White)
            }
        }

        // F. Bottom helper text
        val bottomText = if (gameState == EggGameState.DRAGON_APPEARED) {
            "Apasă dragonul! (Double tap = reset)"
        } else {
            "Apasă pe ou pe puls pentru Perfect!"
        }

        Text(
            text = bottomText,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 46.dp)
        )

        // G. Home button (păstrat ca în proiect)
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
private fun HudTop(
    modifier: Modifier,
    beatPhase: Float,
    progress: Float,
    combo: Int,
    isHatched: Boolean
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Beat bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
        ) {
            drawRect(Color.Black.copy(alpha = 0.25f), size = size)

            val ticks = 8
            val dx = size.width / ticks
            for (i in 0..ticks) {
                val x = i * dx
                drawLine(
                    color = Color.White.copy(alpha = 0.22f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
            }

            val x = beatPhase.coerceIn(0f, 1f) * size.width
            drawLine(
                color = Color.White.copy(alpha = if (isHatched) 1f else 0.85f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 4f
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Hatch meter + combo
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(14.dp)
            ) {
                drawRect(Color.Black.copy(alpha = 0.25f), size = size)
                val w = size.width * progress.coerceIn(0f, 1f)
                drawRect(
                    color = Color.White.copy(alpha = if (isHatched) 1f else 0.85f),
                    size = size.copy(width = w)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = if (combo > 0) "x$combo" else "",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun advanceDragonFrame(
    mode: DragonMode,
    current: Int,
    set: (Int) -> Unit
) {
    val (start, end) = when (mode) {
        DragonMode.IDLE -> 0 to 11
        DragonMode.RISING -> 12 to 23
        DragonMode.CELEBRATE -> 12 to 23
    }
    var c = current
    if (c < start || c > end) c = start
    c++
    if (c > end) c = start
    set(c)
}

/**
 * Sprite sheet split intern (înlocuiește SpriteTools.processSpriteSheet).
 * safetyCrop: taie 1px din margini ca să eviți bleeding/artefacte.
 */
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

/**
 * Particule pentru ou/dragon:
 * - SHELL: folosește vfx_shell_piece (tinted) cu rotație
 * - SPARKLE: cercuri luminoase (fără asset extra)
 */
private class EggParticle private constructor(
    private val kind: Kind,
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

    private enum class Kind { SHELL, SPARKLE }

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

    fun draw(scope: DrawScope, shell: ImageBitmap?) {
        if (!alive) return
        val t = (age / life).coerceIn(0f, 1f)
        val alpha = (1f - t).coerceIn(0f, 1f)

        when (kind) {
            Kind.SHELL -> {
                if (shell == null) return
                val base = shell.width.toFloat().coerceAtLeast(1f)
                val sf = (sizePx / base).coerceIn(0.15f, 2.8f)
                val half = (base * sf) * 0.5f

                scope.withTransform({
                    translate(x - half, y - half)
                    rotate(rotationDeg, pivot = Offset(half, half))
                    scale(sf, sf, pivot = Offset(half, half))
                }) {
                    drawImage(
                        image = shell,
                        topLeft = Offset.Zero,
                        alpha = alpha,
                        colorFilter = ColorFilter.tint(color)
                    )
                }
            }

            Kind.SPARKLE -> {
                val r = (sizePx * 0.25f).coerceAtLeast(2f)
                scope.drawCircle(
                    color = color.copy(alpha = alpha * 0.75f),
                    radius = r,
                    center = Offset(x, y)
                )
                scope.drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.35f),
                    radius = r * 1.8f,
                    center = Offset(x, y),
                    style = Stroke(width = 2f)
                )
            }
        }
    }

    companion object {
        private val shellPalette = listOf(
            Color(0xFFE3F2FD),
            Color(0xFFBBDEFB),
            Color(0xFFB3E5FC),
            Color(0xFFFFFFFF)
        )
        private val sparklePalette = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFB388FF),
            Color(0xFF69F0AE),
            Color(0xFF00E5FF),
            Color(0xFFFFEB3B)
        )

        fun shellBurst(origin: Offset, power: Float, comboBoost: Float): EggParticle {
            val angle = Random.nextDouble(0.0, 2.0 * PI).toFloat()
            val speed = (240f + Random.nextFloat() * 620f) * power * (1f + comboBoost * 0.25f)
            val vx = cos(angle) * speed
            val vy = sin(angle) * speed - (420f * power)
            val size = (14f + Random.nextFloat() * 20f) * (0.9f + power * 0.2f)
            val life = 1.1f + Random.nextFloat() * 1.0f
            return EggParticle(
                kind = Kind.SHELL,
                x = origin.x,
                y = origin.y,
                vx = vx,
                vy = vy,
                rotationDeg = Random.nextFloat() * 360f,
                rotationSpeedDeg = (-420f + Random.nextFloat() * 840f),
                sizePx = size,
                life = life,
                age = 0f,
                color = shellPalette.random(),
                gravity = 980f,
                drag = 0.08f
            )
        }

        fun sparkle(origin: Offset, power: Float, comboBoost: Float): EggParticle {
            val angle = Random.nextDouble(0.0, 2.0 * PI).toFloat()
            val speed = (120f + Random.nextFloat() * 520f) * power * (1f + comboBoost * 0.18f)
            val vx = cos(angle) * speed
            val vy = sin(angle) * speed - (240f * power)
            val size = 10f + Random.nextFloat() * 18f
            val life = 0.55f + Random.nextFloat() * 0.75f
            return EggParticle(
                kind = Kind.SPARKLE,
                x = origin.x,
                y = origin.y,
                vx = vx,
                vy = vy,
                rotationDeg = 0f,
                rotationSpeedDeg = 0f,
                sizePx = size,
                life = life,
                age = 0f,
                color = sparklePalette.random(),
                gravity = 420f,
                drag = 0.12f
            )
        }

        fun sparkleAmbient(origin: Offset, comboBoost: Float): EggParticle {
            val r = 40f + Random.nextFloat() * 80f
            val a = Random.nextDouble(0.0, 2.0 * PI).toFloat()
            val x = origin.x + cos(a) * r
            val y = origin.y + sin(a) * r * 0.65f

            val vx = (-60f + Random.nextFloat() * 120f) * (1f + comboBoost * 0.25f)
            val vy = (-120f - Random.nextFloat() * 140f) * (1f + comboBoost * 0.25f)

            val size = 8f + Random.nextFloat() * 14f
            val life = 0.50f + Random.nextFloat() * 0.55f

            return EggParticle(
                kind = Kind.SPARKLE,
                x = x,
                y = y,
                vx = vx,
                vy = vy,
                rotationDeg = 0f,
                rotationSpeedDeg = 0f,
                sizePx = size,
                life = life,
                age = 0f,
                color = sparklePalette.random(),
                gravity = 260f,
                drag = 0.20f
            )
        }
    }
}

/**
 * Shockwave ring (cerc expandabil).
 */
private class Shockwave(
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
        alpha -= dt * 0.85f
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
