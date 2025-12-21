package com.example.educationalapp

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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// --- Animal Band Game Fix ---

@Composable
fun AnimalBandGame(onHome: () -> Unit = {}) {
    val context = LocalContext.current

    // --- CONFIG ---
    val bpm = 120f
    val beatPeriodNanos = remember(bpm) { (60_000_000_000f / bpm).toLong().coerceAtLeast(1L) }

    // --- ROOT SIZE ---
    var rootSizePx by remember { mutableStateOf(IntSize.Zero) }

    // --- RESURSE ---
    var frogFrames by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var bearFrames by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var catFrames by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var noteImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var assetsLoaded by remember { mutableStateOf(false) }

    // --- STARE MUZICANTI ---
    var frogPlaying by remember { mutableStateOf(false) }
    var bearPlaying by remember { mutableStateOf(false) }
    var catPlaying by remember { mutableStateOf(false) }

    var frogCombo by remember { mutableIntStateOf(0) }
    var bearCombo by remember { mutableIntStateOf(0) }
    var catCombo by remember { mutableIntStateOf(0) }

    var frogLastHit by remember { mutableStateOf(BandHitQuality.NONE) }
    var bearLastHit by remember { mutableStateOf(BandHitQuality.NONE) }
    var catLastHit by remember { mutableStateOf(BandHitQuality.NONE) }

    var frogLastHitAt by remember { mutableStateOf(0L) }
    var bearLastHitAt by remember { mutableStateOf(0L) }
    var catLastHitAt by remember { mutableStateOf(0L) }

    var frogAnchor by remember { mutableStateOf(Offset.Zero) }
    var bearAnchor by remember { mutableStateOf(Offset.Zero) }
    var catAnchor by remember { mutableStateOf(Offset.Zero) }

    // --- VFX ---
    val particles = remember { mutableStateListOf<BandParticle>() }
    val shockwaves = remember { mutableStateListOf<BandShockwave>() }

    var jam by remember { mutableStateOf(0f) }
    var screenFlash by remember { mutableStateOf(0f) }
    var isFinalJam by remember { mutableStateOf(false) }
    var finalJamTimeLeft by remember { mutableStateOf(0f) }

    // Beat clock state
    var clockNanos by remember { mutableStateOf(0L) }
    var lastBeatIndex by remember { mutableStateOf(-1L) }
    val beatPhase: Float by derivedStateOf {
        if (clockNanos <= 0L) 0f
        else ((clockNanos % beatPeriodNanos).toDouble() / beatPeriodNanos.toDouble()).toFloat()
    }

    // --- LOAD RESOURCES (CRITICAL FIX: inScaled = false) ---
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val opts = BitmapFactory.Options().apply { inScaled = false } // PREVINE SCALING-UL AUTOMAT

            val frogSheet = BitmapFactory.decodeResource(context.resources, R.drawable.band_frog_sheet, opts)
            val bearSheet = BitmapFactory.decodeResource(context.resources, R.drawable.band_bear_sheet, opts)
            val catSheet = BitmapFactory.decodeResource(context.resources, R.drawable.band_cat_sheet, opts)
            val noteBmp = BitmapFactory.decodeResource(context.resources, R.drawable.vfx_music_note, opts)

            // Folosim 4 randuri, 6 coloane (standardul tau)
            val fFrog = splitSpriteSheet(frogSheet, 4, 6).map { it.asImageBitmap() }
            val fBear = splitSpriteSheet(bearSheet, 4, 6).map { it.asImageBitmap() }
            val fCat = splitSpriteSheet(catSheet, 4, 6).map { it.asImageBitmap() }
            
            val fNote = noteBmp?.asImageBitmap()

            withContext(Dispatchers.Main) {
                frogFrames = fFrog
                bearFrames = fBear
                catFrames = fCat
                noteImage = fNote
                assetsLoaded = true
            }
        }
    }

    fun activeMusiciansCount(): Int {
        var c = 0
        if (frogPlaying) c++
        if (bearPlaying) c++
        if (catPlaying) c++
        return c
    }

    fun synergyMultiplier(): Float {
        return when (activeMusiciansCount()) {
            0 -> 1f
            1 -> 1f
            2 -> 1.25f
            else -> 1.5f
        }
    }

    fun triggerFinalJam(center: Offset) {
        if (isFinalJam) return
        isFinalJam = true
        finalJamTimeLeft = 6.0f
        screenFlash = 1f

        shockwaves.add(
            BandShockwave(
                center = center,
                radius = 0f,
                speed = 900f,
                width = 10f,
                alpha = 0.9f
            )
        )

        repeat(220) {
            particles.add(BandParticle.confettiBurst(center))
        }
    }

    fun addJam(amount: Float) {
        val next = (jam + amount).coerceIn(0f, 1f)
        jam = next
        if (jam >= 1f && !isFinalJam) {
            val center = when {
                bearAnchor != Offset.Zero -> bearAnchor
                frogAnchor != Offset.Zero -> frogAnchor
                catAnchor != Offset.Zero -> catAnchor
                else -> Offset((rootSizePx.width * 0.5f), (rootSizePx.height * 0.5f))
            }
            triggerFinalJam(center)
        }
    }

    fun noteBurst(anchor: Offset, baseCount: Int, energy: Float, extraScatter: Boolean = false) {
        if (anchor == Offset.Zero) return
        val count = (baseCount * (1f + energy * 1.25f)).roundToInt().coerceIn(1, 22)
        repeat(count) {
            particles.add(BandParticle.note(anchor, jam = jam, finale = isFinalJam, extraScatter = extraScatter))
        }
    }

    fun accentHit(anchor: Offset, quality: BandHitQuality, comboNow: Int) {
        val comboBoost = (comboNow.coerceAtMost(20) / 20f)
        when (quality) {
            BandHitQuality.PERFECT -> {
                screenFlash = maxOf(screenFlash, 0.70f)
                shockwaves.add(BandShockwave(center = anchor, radius = 0f, speed = 760f, width = 7.5f, alpha = 0.80f))
                noteBurst(anchor, baseCount = 10 + (comboBoost * 6f).roundToInt(), energy = 1f + comboBoost * 0.6f, extraScatter = true)
            }
            BandHitQuality.GOOD -> {
                screenFlash = maxOf(screenFlash, 0.38f)
                shockwaves.add(BandShockwave(center = anchor, radius = 0f, speed = 560f, width = 5.5f, alpha = 0.60f))
                noteBurst(anchor, baseCount = 6 + (comboBoost * 4f).roundToInt(), energy = 0.65f + comboBoost * 0.45f)
            }
            BandHitQuality.MISS -> {
                screenFlash = maxOf(screenFlash, 0.12f)
                noteBurst(anchor, baseCount = 3, energy = 0.25f)
            }
            BandHitQuality.NONE -> Unit
        }
    }

    fun evaluateHit(nowNanos: Long): BandHitQuality {
        if (nowNanos <= 0L) return BandHitQuality.NONE
        val phase = (nowNanos % beatPeriodNanos).toDouble() / beatPeriodNanos.toDouble()
        val distToBeat = min(phase, 1.0 - phase) * beatPeriodNanos.toDouble()
        val distMs = distToBeat / 1_000_000.0
        return when {
            distMs <= 60.0 -> BandHitQuality.PERFECT
            distMs <= 130.0 -> BandHitQuality.GOOD
            else -> BandHitQuality.MISS
        }
    }

    fun applyHitToMusician(
        musician: MusicianId,
        quality: BandHitQuality,
        anchor: Offset,
        nowNanos: Long
    ) {
        val mult = synergyMultiplier()

        val comboNow: Int = when (musician) {
            MusicianId.FROG -> {
                frogLastHit = quality
                frogLastHitAt = nowNanos
                frogCombo = when (quality) {
                    BandHitQuality.PERFECT, BandHitQuality.GOOD -> (frogCombo + 1).coerceAtMost(999)
                    BandHitQuality.MISS -> 0
                    BandHitQuality.NONE -> frogCombo
                }
                frogCombo
            }
            MusicianId.BEAR -> {
                bearLastHit = quality
                bearLastHitAt = nowNanos
                bearCombo = when (quality) {
                    BandHitQuality.PERFECT, BandHitQuality.GOOD -> (bearCombo + 1).coerceAtMost(999)
                    BandHitQuality.MISS -> 0
                    BandHitQuality.NONE -> bearCombo
                }
                bearCombo
            }
            MusicianId.CAT -> {
                catLastHit = quality
                catLastHitAt = nowNanos
                catCombo = when (quality) {
                    BandHitQuality.PERFECT, BandHitQuality.GOOD -> (catCombo + 1).coerceAtMost(999)
                    BandHitQuality.MISS -> 0
                    BandHitQuality.NONE -> catCombo
                }
                catCombo
            }
        }

        val comboBoost = 1f + (comboNow.coerceAtMost(20) / 20f) * 0.35f
        val base = when (quality) {
            BandHitQuality.PERFECT -> 0.12f
            BandHitQuality.GOOD -> 0.07f
            BandHitQuality.MISS -> -0.04f
            BandHitQuality.NONE -> 0f
        }
        addJam(base * mult * comboBoost)
        accentHit(anchor, quality, comboNow)
    }

    fun onMusicianClick(musician: MusicianId) {
        val now = clockNanos
        val quality = evaluateHit(now)

        when (musician) {
            MusicianId.FROG -> {
                if (!frogPlaying) frogPlaying = true
                applyHitToMusician(MusicianId.FROG, quality, frogAnchor, now)
            }
            MusicianId.BEAR -> {
                if (!bearPlaying) bearPlaying = true
                applyHitToMusician(MusicianId.BEAR, quality, bearAnchor, now)
            }
            MusicianId.CAT -> {
                if (!catPlaying) catPlaying = true
                applyHitToMusician(MusicianId.CAT, quality, catAnchor, now)
            }
        }
    }

    fun onMusicianDoubleTap(musician: MusicianId) {
        when (musician) {
            MusicianId.FROG -> frogPlaying = false
            MusicianId.BEAR -> bearPlaying = false
            MusicianId.CAT -> catPlaying = false
        }
    }

    fun onBeat() {
        val mult = synergyMultiplier()

        if (frogPlaying) {
            addJam(0.012f * mult)
            noteBurst(frogAnchor, baseCount = 2, energy = 0.3f)
        }
        if (bearPlaying) {
            addJam(0.012f * mult)
            noteBurst(bearAnchor, baseCount = 2, energy = 0.3f)
        }
        if (catPlaying) {
            addJam(0.012f * mult)
            noteBurst(catAnchor, baseCount = 2, energy = 0.3f)
        }

        if (isFinalJam) {
            val center = when {
                bearAnchor != Offset.Zero -> bearAnchor
                frogAnchor != Offset.Zero -> frogAnchor
                catAnchor != Offset.Zero -> catAnchor
                else -> Offset((rootSizePx.width * 0.5f), (rootSizePx.height * 0.5f))
            }
            shockwaves.add(BandShockwave(center = center, radius = 0f, speed = 980f, width = 9f, alpha = 0.55f))

            val widthPx = rootSizePx.width.toFloat().coerceAtLeast(1f)
            repeat(18) {
                particles.add(BandParticle.confettiAmbient(widthPx))
            }
            screenFlash = maxOf(screenFlash, 0.18f)
        }
    }

    // --- MAIN FRAME LOOP ---
    LaunchedEffect(Unit) {
        var lastFrame = 0L
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

            val beatIndex = now / beatPeriodNanos
            if (beatIndex != lastBeatIndex) {
                lastBeatIndex = beatIndex
                onBeat()
            }

            screenFlash = (screenFlash - dt * 1.7f).coerceIn(0f, 1f)

            if (isFinalJam) {
                finalJamTimeLeft -= dt
                if (finalJamTimeLeft <= 0f) {
                    isFinalJam = false
                    finalJamTimeLeft = 0f
                    jam = 0.15f
                }
            }

            for (i in shockwaves.size - 1 downTo 0) {
                val s = shockwaves[i]
                s.update(dt)
                if (!s.alive) shockwaves.removeAt(i)
            }

            val maxParticles = if (isFinalJam) 650 else 350
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
        // A. FUNDAL
        Image(
            painter = painterResource(id = R.drawable.bg_music_stage),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (!assetsLoaded) {
            Text("Loading Band...", color = Color.White, modifier = Modifier.align(Alignment.Center))
        } else {
            // B. Glow
            val glowAlpha = (0.05f + jam * 0.10f + (if (isFinalJam) 0.12f else 0f)).coerceIn(0f, 0.35f)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(glowAlpha)
            ) {
                drawRect(color = Color.White.copy(alpha = 0.22f), size = size.copy(height = size.height * 0.12f))
                val c = Offset(size.width * 0.5f, size.height * 0.45f)
                drawCircle(color = Color.White.copy(alpha = 0.18f), radius = min(size.width, size.height) * 0.38f, center = c)
            }

            // C. Beat Bar + Jam Meter
            BandHudTop(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp, start = 12.dp, end = 12.dp),
                beatPhase = beatPhase,
                jam = jam,
                isFinalJam = isFinalJam
            )

            // D. Band area
            val bouncePx = (-sin(beatPhase * 2f * PI).toFloat() * (8f + jam * 14f) * (if (activeMusiciansCount() > 0) 1f else 0f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(0, bouncePx.roundToInt()) }
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                MusicianCharacter(
                    frames = frogFrames,
                    isPlaying = frogPlaying,
                    combo = frogCombo,
                    lastHit = frogLastHit,
                    lastHitAtNanos = frogLastHitAt,
                    label = "Frog",
                    onAnchor = { frogAnchor = it },
                    onClick = { onMusicianClick(MusicianId.FROG) },
                    onDoubleTap = { onMusicianDoubleTap(MusicianId.FROG) }
                )

                MusicianCharacter(
                    frames = bearFrames,
                    isPlaying = bearPlaying,
                    combo = bearCombo,
                    lastHit = bearLastHit,
                    lastHitAtNanos = bearLastHitAt,
                    label = "Bear",
                    onAnchor = { bearAnchor = it },
                    onClick = { onMusicianClick(MusicianId.BEAR) },
                    onDoubleTap = { onMusicianDoubleTap(MusicianId.BEAR) }
                )

                MusicianCharacter(
                    frames = catFrames,
                    isPlaying = catPlaying,
                    combo = catCombo,
                    lastHit = catLastHit,
                    lastHitAtNanos = catLastHitAt,
                    label = "Cat",
                    onAnchor = { catAnchor = it },
                    onClick = { onMusicianClick(MusicianId.CAT) },
                    onDoubleTap = { onMusicianDoubleTap(MusicianId.CAT) }
                )
            }

            // E. VFX layer
            Canvas(modifier = Modifier.fillMaxSize()) {
                val note = noteImage
                shockwaves.forEach { it.draw(this) }
                particles.forEach { p ->
                    p.draw(this, note)
                }
            }

            // F. Screen flash
            if (screenFlash > 0f) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha((screenFlash * 0.35f).coerceIn(0f, 0.35f))
                ) {
                    drawRect(color = Color.White)
                }
            }
        }

        // G. Home button
        Image(
            painter = painterResource(id = R.drawable.ui_button_home),
            contentDescription = "Home",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(64.dp)
                .clickable { onHome() }
        )

        // H. Final Jam Banner
        if (isFinalJam) {
            Text(
                text = "FINAL JAM!",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 190.dp)
                    .scale(1.35f)
            )
        }
    }
}

@Composable
private fun BandHudTop(
    modifier: Modifier,
    beatPhase: Float,
    jam: Float,
    isFinalJam: Boolean
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        ) {
            drawRect(Color.Black.copy(alpha = 0.25f), size = size)
            val ticks = 8
            val dx = size.width / ticks
            for (i in 0..ticks) {
                val x = i * dx
                drawLine(
                    color = Color.White.copy(alpha = 0.25f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
            }
            val x = (beatPhase.coerceIn(0f, 1f)) * size.width
            drawLine(
                color = if (isFinalJam) Color.White else Color.White.copy(alpha = 0.85f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 4f
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
        ) {
            drawRect(Color.Black.copy(alpha = 0.25f), size = size)
            val w = (size.width * jam.coerceIn(0f, 1f))
            drawRect(
                color = if (isFinalJam) Color.White else Color.White.copy(alpha = 0.85f),
                size = size.copy(width = w)
            )
        }
    }
}

private enum class MusicianId { FROG, BEAR, CAT }

private enum class BandHitQuality(val label: String, val color: Color) {
    PERFECT("Perfect", Color(0xFF00E5FF)),
    GOOD("Good", Color(0xFFFFEB3B)),
    MISS("Miss", Color(0xFFFF5252)),
    NONE("", Color.Transparent)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MusicianCharacter(
    frames: List<ImageBitmap>,
    isPlaying: Boolean,
    combo: Int,
    lastHit: BandHitQuality,
    lastHitAtNanos: Long,
    label: String,
    onAnchor: (Offset) -> Unit,
    onClick: () -> Unit,
    onDoubleTap: () -> Unit
) {
    val characterSize = 220.dp
    val scaleAnim = remember { Animatable(1f) }
    var currentFrame by remember { mutableIntStateOf(0) }

    LaunchedEffect(frames, isPlaying) {
        if (frames.isEmpty()) return@LaunchedEffect
        var last = 0L
        var acc = 0f
        while (isActive) {
            val now = withFrameNanos { it }
            if (last == 0L) {
                last = now
                continue
            }
            val dt = ((now - last).toDouble() / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
            last = now
            acc += dt
            // FIX VITEZA: 0.12f in loc de 0.06f pentru a incetini animatia (sa nu mai deruleze rapid)
            val frameDuration = if (isPlaying) 0.12f else 0.18f
            while (acc >= frameDuration) {
                if (isPlaying) {
                    if (currentFrame < 12) currentFrame = 12
                    currentFrame++
                    if (currentFrame > 23) currentFrame = 12
                } else {
                    currentFrame++
                    if (currentFrame > 11) currentFrame = 0
                }
                acc -= frameDuration
            }
        }
    }

    LaunchedEffect(isPlaying) {
        scaleAnim.animateTo(0.95f, animationSpec = tween(70))
        scaleAnim.animateTo(1f, animationSpec = tween(90))
    }

    Box(
        modifier = Modifier
            .size(characterSize)
            .scale(scaleAnim.value)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onClick() },
                onDoubleClick = { onDoubleTap() }
            )
            .onGloballyPositioned { coordinates ->
                val pos = coordinates.positionInRoot()
                val size = coordinates.size
                val ax = pos.x + size.width * 0.72f
                val ay = pos.y + size.height * 0.45f
                onAnchor(Offset(ax, ay))
            },
        contentAlignment = Alignment.Center
    ) {
        if (frames.isNotEmpty()) {
            Image(
                bitmap = frames[currentFrame.coerceIn(0, frames.lastIndex)],
                contentDescription = label,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Nu aratam nimic daca frames e gol, pentru a evita flash-uri
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (combo > 0) {
                Text(
                    text = "x$combo",
                    color = Color.White
                )
            }
            val showHit = lastHit != BandHitQuality.NONE && (System.nanoTime() - lastHitAtNanos) < 900_000_000L
            if (showHit) {
                Text(
                    text = lastHit.label,
                    color = lastHit.color
                )
            }
        }
    }
}

// FUNCȚIE CORECTATĂ PENTRU SPRITE-URI FIXE (FĂRĂ DERULARE)
private fun splitSpriteSheet(
    sheet: Bitmap?,
    rows: Int,
    cols: Int
): List<Bitmap> {
    if (sheet == null || rows <= 0 || cols <= 0) return emptyList()
    
    // Calculăm dimensiunea exactă a unei celule
    val frameW = sheet.width / cols
    val frameH = sheet.height / rows
    
    // Dacă imaginea e prea mică sau invalidă
    if (frameW <= 0 || frameH <= 0) return emptyList()

    val out = ArrayList<Bitmap>(rows * cols)
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val x = c * frameW
            val y = r * frameH
            // Nu mai folosim safetyCrop pentru ca strica gridul perfect
            out.add(Bitmap.createBitmap(sheet, x, y, frameW, frameH))
        }
    }
    return out
}

private data class BandQuad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

private class BandParticle private constructor(
    private val kind: Kind,
    private var x: Float,
    private var y: Float,
    private var vx: Float,
    private var vy: Float,
    private var baseX: Float,
    private var waveAmp: Float,
    private var waveFreq: Float,
    private var rotationDeg: Float,
    private var rotationSpeedDeg: Float,
    private var size: Float,
    private var life: Float,
    private var age: Float,
    private var color: Color,
    private var gravity: Float,
    private var drag: Float
) {
    var alive: Boolean = true
        private set

    private enum class Kind { NOTE, CONFETTI }

    fun update(dt: Float) {
        if (!alive) return
        age += dt
        if (age >= life) {
            alive = false
            return
        }

        val dragFactor = (1f - drag * dt).coerceIn(0.0f, 1.0f)
        vx *= dragFactor
        vy = (vy + gravity * dt) * dragFactor

        x += vx * dt
        y += vy * dt

        rotationDeg += rotationSpeedDeg * dt

        if (kind == Kind.NOTE) {
            x = baseX + (sin(age * waveFreq) * waveAmp)
        }
    }

    fun draw(scope: DrawScope, note: ImageBitmap?) {
        if (!alive) return
        val t = (age / life).coerceIn(0f, 1f)
        val alpha = (1f - t).coerceIn(0f, 1f)

        when (kind) {
            Kind.NOTE -> {
                if (note == null) return
                val half = size * 0.5f
                val pulse = 0.92f + 0.16f * abs(sin(age * 4.8f))
                scope.withTransform({
                    translate(left = x - half, top = y - half)
                    rotate(degrees = rotationDeg, pivot = Offset(half, half))
                    scale(scaleX = pulse, scaleY = pulse, pivot = Offset(half, half))
                }) {
                    drawImage(
                        image = note,
                        topLeft = Offset.Zero,
                        alpha = alpha,
                        colorFilter = ColorFilter.tint(color)
                    )
                }

                if (alpha > 0.25f) {
                    scope.drawCircle(
                        color = color.copy(alpha = alpha * 0.35f),
                        radius = size * 0.08f,
                        center = Offset(x, y)
                    )
                }
            }

            Kind.CONFETTI -> {
                val w = size * (0.55f + 0.45f * abs(sin(age * 9f)))
                val h = size * (0.35f + 0.55f * abs(cos(age * 11f)))
                scope.withTransform({
                    translate(left = x, top = y)
                    rotate(degrees = rotationDeg)
                }) {
                    drawRect(
                        color = color.copy(alpha = alpha),
                        topLeft = Offset(-w * 0.5f, -h * 0.5f),
                        size = Size(w, h)
                    )
                }
            }
        }
    }

    companion object {
        private val palette = listOf(
            Color(0xFFFFEB3B), Color(0xFF03A9F4), Color(0xFFFF4081),
            Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFFB388FF), Color(0xFF69F0AE)
        )

        fun note(origin: Offset, jam: Float, finale: Boolean, extraScatter: Boolean): BandParticle {
            val energy = (0.25f + jam * 1.0f + (if (finale) 0.65f else 0f)).coerceIn(0.25f, 1.75f)
            val spread = if (extraScatter) 0.80 else 0.55
            val angle = (-PI / 2 + Random.nextDouble(-spread, spread)).toFloat()
            val speed = (260f + Random.nextFloat() * 320f) * energy
            val vx = cos(angle) * speed
            val vy = sin(angle) * speed
            val size = (26f + Random.nextFloat() * 22f) * (0.85f + jam * 0.6f)
            val life = (1.0f + Random.nextFloat() * 0.7f) * (if (finale) 1.15f else 1f)
            val waveAmp = 8f + Random.nextFloat() * 18f
            val waveFreq = 6f + Random.nextFloat() * 7f
            return BandParticle(
                kind = Kind.NOTE,
                x = origin.x, y = origin.y, vx = vx, vy = vy, baseX = origin.x,
                waveAmp = waveAmp, waveFreq = waveFreq,
                rotationDeg = Random.nextFloat() * 360f,
                rotationSpeedDeg = (-240f + Random.nextFloat() * 480f),
                size = size, life = life, age = 0f,
                color = palette.random(), gravity = -40f, drag = 0.22f
            )
        }

        fun confettiBurst(center: Offset): BandParticle {
            val angle = Random.nextDouble(0.0, 2.0 * PI).toFloat()
            val speed = 240f + Random.nextFloat() * 720f
            val vx = cos(angle) * speed
            val vy = sin(angle) * speed - 260f
            val size = 10f + Random.nextFloat() * 14f
            val life = 1.6f + Random.nextFloat() * 1.4f
            return BandParticle(
                kind = Kind.CONFETTI,
                x = center.x, y = center.y, vx = vx, vy = vy, baseX = center.x,
                waveAmp = 0f, waveFreq = 0f,
                rotationDeg = Random.nextFloat() * 360f,
                rotationSpeedDeg = (-360f + Random.nextFloat() * 720f),
                size = size, life = life, age = 0f,
                color = palette.random(), gravity = 880f, drag = 0.08f
            )
        }

        fun confettiAmbient(rootWidthPx: Float): BandParticle {
            val x = Random.nextFloat() * rootWidthPx
            val y = -40f
            val vx = (-80f + Random.nextFloat() * 160f)
            val vy = (70f + Random.nextFloat() * 230f)
            val size = 9f + Random.nextFloat() * 10f
            val life = 1.3f + Random.nextFloat() * 1.0f
            return BandParticle(
                kind = Kind.CONFETTI,
                x = x, y = y, vx = vx, vy = vy, baseX = x,
                waveAmp = 0f, waveFreq = 0f,
                rotationDeg = Random.nextFloat() * 360f,
                rotationSpeedDeg = (-260f + Random.nextFloat() * 520f),
                size = size, life = life, age = 0f,
                color = palette.random(), gravity = 920f, drag = 0.06f
            )
        }
    }
}

private class BandShockwave(
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