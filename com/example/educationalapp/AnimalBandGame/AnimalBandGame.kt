package com.example.educationalapp

import com.example.educationalapp.R
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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

/**
 * AnimalBandGame - Mechanic Upgrade (spectaculos + stabilitate).
 *
 * - Fără "remove background alb": se presupune PNG-uri cu transparență (alpha).
 * - Metronom / Beat timing (Perfect / Good / Miss).
 * - Combo per muzicant + Jam Meter global + Final Jam (confetti + shockwave + flash).
 * - Particulele sunt randate pe Canvas (performanță, fără sute de Image() composables).
 * - Loop frame-synced (withFrameNanos) + dt capped.
 *
 * NOTE:
 * - Acest fișier nu include resurse (imagini). Folosește aceleași R.drawable.* din proiectul tău.
 */
@Composable
fun AnimalBandGame(onHome: () -> Unit = {}) {
    val context = LocalContext.current

    // --- CONFIG ---
    val bpm = 120f
    val beatPeriodNanos = remember(bpm) { (60_000_000_000f / bpm).toLong().coerceAtLeast(1L) }

    // --- ROOT SIZE (pentru VFX spawn corect) ---
    var rootSizePx by remember { mutableStateOf(IntSize.Zero) }

    // --- RESURSE (prelucrate off-main) ---
    var frogFrames by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var bearFrames by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var catFrames by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var noteImage by remember { mutableStateOf<ImageBitmap?>(null) }

    // --- STARE MUZICANTI ---
    var frogPlaying by remember { mutableStateOf(false) }
    var bearPlaying by remember { mutableStateOf(false) }
    var catPlaying by remember { mutableStateOf(false) }

    var frogCombo by remember { mutableIntStateOf(0) }
    var bearCombo by remember { mutableIntStateOf(0) }
    var catCombo by remember { mutableIntStateOf(0) }

    var frogLastHit by remember { mutableStateOf(HitQuality.NONE) }
    var bearLastHit by remember { mutableStateOf(HitQuality.NONE) }
    var catLastHit by remember { mutableStateOf(HitQuality.NONE) }

    var frogLastHitAt by remember { mutableStateOf(0L) }
    var bearLastHitAt by remember { mutableStateOf(0L) }
    var catLastHitAt by remember { mutableStateOf(0L) }

    // Anchor (poziția de unde ies VFX) în coordonate Root
    var frogAnchor by remember { mutableStateOf(Offset.Zero) }
    var bearAnchor by remember { mutableStateOf(Offset.Zero) }
    var catAnchor by remember { mutableStateOf(Offset.Zero) }

    // --- VFX / GAME STATE ---
    val particles = remember { mutableStateListOf<Particle>() }
    val shockwaves = remember { mutableStateListOf<Shockwave>() }

    var jam by remember { mutableStateOf(0f) } // 0..1
    var screenFlash by remember { mutableStateOf(0f) } // 0..1
    var isFinalJam by remember { mutableStateOf(false) }
    var finalJamTimeLeft by remember { mutableStateOf(0f) } // seconds

    // Beat clock state
    var clockNanos by remember { mutableStateOf(0L) }
    var lastBeatIndex by remember { mutableStateOf(-1L) }
    val beatPhase: Float by derivedStateOf {
        if (clockNanos <= 0L) 0f
        else ((clockNanos % beatPeriodNanos).toDouble() / beatPeriodNanos.toDouble()).toFloat()
    }

    // --- LOAD RESOURCES ---
    LaunchedEffect(Unit) {
        val (frog, bear, cat, note) = withContext(Dispatchers.Default) {
            // Decode on IO, split on Default
            val frogSheet = withContext(Dispatchers.IO) {
                BitmapFactory.decodeResource(context.resources, R.drawable.band_frog_sheet)
            }
            val bearSheet = withContext(Dispatchers.IO) {
                BitmapFactory.decodeResource(context.resources, R.drawable.band_bear_sheet)
            }
            val catSheet = withContext(Dispatchers.IO) {
                BitmapFactory.decodeResource(context.resources, R.drawable.band_cat_sheet)
            }
            val noteBmp = withContext(Dispatchers.IO) {
                BitmapFactory.decodeResource(context.resources, R.drawable.vfx_music_note)
            }

            val frogFramesBmp = splitSpriteSheet(frogSheet, rows = 4, cols = 6, safetyCrop = true)
            val bearFramesBmp = splitSpriteSheet(bearSheet, rows = 4, cols = 6, safetyCrop = true)
            val catFramesBmp = splitSpriteSheet(catSheet, rows = 4, cols = 6, safetyCrop = true)

            Quad(
                frogFramesBmp.map { it.asImageBitmap() },
                bearFramesBmp.map { it.asImageBitmap() },
                catFramesBmp.map { it.asImageBitmap() },
                noteBmp.asImageBitmap()
            )
        }

        frogFrames = frog
        bearFrames = bear
        catFrames = cat
        noteImage = note
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

    fun clampJam(v: Float): Float = v.coerceIn(0f, 1f)

    fun triggerFinalJam(center: Offset) {
        if (isFinalJam) return
        isFinalJam = true
        finalJamTimeLeft = 6.0f
        screenFlash = 1f

        // Big shockwave + massive confetti
        shockwaves.add(
            Shockwave(
                center = center,
                radius = 0f,
                speed = 900f,
                width = 10f,
                alpha = 0.9f
            )
        )

        // Confetti burst
        repeat(220) {
            particles.add(Particle.confettiBurst(center))
        }
    }

    fun addJam(amount: Float) {
        val next = clampJam(jam + amount)
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
            particles.add(Particle.note(anchor, jam = jam, finale = isFinalJam, extraScatter = extraScatter))
        }
    }

    fun accentHit(anchor: Offset, quality: HitQuality, comboNow: Int) {
        val comboBoost = (comboNow.coerceAtMost(20) / 20f) // 0..1
        when (quality) {
            HitQuality.PERFECT -> {
                screenFlash = maxOf(screenFlash, 0.70f)
                shockwaves.add(Shockwave(center = anchor, radius = 0f, speed = 760f, width = 7.5f, alpha = 0.80f))
                noteBurst(anchor, baseCount = 10 + (comboBoost * 6f).roundToInt(), energy = 1f + comboBoost * 0.6f, extraScatter = true)
            }
            HitQuality.GOOD -> {
                screenFlash = maxOf(screenFlash, 0.38f)
                shockwaves.add(Shockwave(center = anchor, radius = 0f, speed = 560f, width = 5.5f, alpha = 0.60f))
                noteBurst(anchor, baseCount = 6 + (comboBoost * 4f).roundToInt(), energy = 0.65f + comboBoost * 0.45f)
            }
            HitQuality.MISS -> {
                screenFlash = maxOf(screenFlash, 0.12f)
                noteBurst(anchor, baseCount = 3, energy = 0.25f)
            }
            HitQuality.NONE -> Unit
        }
    }

    fun evaluateHit(nowNanos: Long): HitQuality {
        if (nowNanos <= 0L) return HitQuality.NONE
        val phase = (nowNanos % beatPeriodNanos).toDouble() / beatPeriodNanos.toDouble() // 0..1
        val distToBeat = min(phase, 1.0 - phase) * beatPeriodNanos.toDouble()
        val distMs = distToBeat / 1_000_000.0
        return when {
            distMs <= 60.0 -> HitQuality.PERFECT
            distMs <= 130.0 -> HitQuality.GOOD
            else -> HitQuality.MISS
        }
    }

    fun applyHitToMusician(
        musician: MusicianId,
        quality: HitQuality,
        anchor: Offset,
        nowNanos: Long
    ) {
        val mult = synergyMultiplier()

        val comboNow: Int = when (musician) {
            MusicianId.FROG -> {
                frogLastHit = quality
                frogLastHitAt = nowNanos
                frogCombo = when (quality) {
                    HitQuality.PERFECT, HitQuality.GOOD -> (frogCombo + 1).coerceAtMost(999)
                    HitQuality.MISS -> 0
                    HitQuality.NONE -> frogCombo
                }
                frogCombo
            }
            MusicianId.BEAR -> {
                bearLastHit = quality
                bearLastHitAt = nowNanos
                bearCombo = when (quality) {
                    HitQuality.PERFECT, HitQuality.GOOD -> (bearCombo + 1).coerceAtMost(999)
                    HitQuality.MISS -> 0
                    HitQuality.NONE -> bearCombo
                }
                bearCombo
            }
            MusicianId.CAT -> {
                catLastHit = quality
                catLastHitAt = nowNanos
                catCombo = when (quality) {
                    HitQuality.PERFECT, HitQuality.GOOD -> (catCombo + 1).coerceAtMost(999)
                    HitQuality.MISS -> 0
                    HitQuality.NONE -> catCombo
                }
                catCombo
            }
        }

        // Jam gain/loss (cu boost din combo)
        val comboBoost = 1f + (comboNow.coerceAtMost(20) / 20f) * 0.35f
        val base = when (quality) {
            HitQuality.PERFECT -> 0.12f
            HitQuality.GOOD -> 0.07f
            HitQuality.MISS -> -0.04f
            HitQuality.NONE -> 0f
        }
        addJam(base * mult * comboBoost)

        // VFX
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

        // Groove: dacă muzicanții cântă, la fiecare beat ies note + se umple Jam (mai mic decât la tap).
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

        // Final Jam intensifică pe beat
        if (isFinalJam) {
            val center = when {
                bearAnchor != Offset.Zero -> bearAnchor
                frogAnchor != Offset.Zero -> frogAnchor
                catAnchor != Offset.Zero -> catAnchor
                else -> Offset((rootSizePx.width * 0.5f), (rootSizePx.height * 0.5f))
            }
            shockwaves.add(Shockwave(center = center, radius = 0f, speed = 980f, width = 9f, alpha = 0.55f))

            val widthPx = rootSizePx.width.toFloat().coerceAtLeast(1f)
            repeat(18) {
                particles.add(Particle.confettiAmbient(widthPx))
            }
            screenFlash = maxOf(screenFlash, 0.18f)
        }
    }

    // --- MAIN FRAME LOOP (VFX + beat + finale) ---
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

            // Beat tick
            val beatIndex = now / beatPeriodNanos
            if (beatIndex != lastBeatIndex) {
                lastBeatIndex = beatIndex
                onBeat()
            }

            // Update screen flash
            screenFlash = (screenFlash - dt * 1.7f).coerceIn(0f, 1f)

            // Update finale timer
            if (isFinalJam) {
                finalJamTimeLeft -= dt
                if (finalJamTimeLeft <= 0f) {
                    isFinalJam = false
                    finalJamTimeLeft = 0f
                    jam = 0.15f // un mic "afterglow"
                }
            }

            // Update shockwaves
            for (i in shockwaves.size - 1 downTo 0) {
                val s = shockwaves[i]
                s.update(dt)
                if (!s.alive) shockwaves.removeAt(i)
            }

            // Update particles (limit defensiv)
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
        // A. FUNDAL (Scena)
        Image(
            painter = painterResource(id = R.drawable.bg_music_stage),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // B. Glow / Lights (în funcție de Jam)
        val glowAlpha = (0.05f + jam * 0.10f + (if (isFinalJam) 0.12f else 0f)).coerceIn(0f, 0.35f)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(glowAlpha)
        ) {
            // bandă de lumină sus + un "spot" în centru
            drawRect(color = Color.White.copy(alpha = 0.22f), size = size.copy(height = size.height * 0.12f))
            val c = Offset(size.width * 0.5f, size.height * 0.45f)
            drawCircle(color = Color.White.copy(alpha = 0.18f), radius = min(size.width, size.height) * 0.38f, center = c)
        }

        // C. Beat Bar + Jam Meter
        HudTop(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp, start = 12.dp, end = 12.dp),
            beatPhase = beatPhase,
            jam = jam,
            isFinalJam = isFinalJam
        )

        // D. Band area (bounce pe beat)
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
            // 1. Frog (Drums)
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

            // 2. Bear (Trumpet)
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

            // 3. Cat (Guitar)
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

        // E. VFX layer (Canvas)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val note = noteImage

            // Shockwaves
            shockwaves.forEach { it.draw(this) }

            // Particles
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

        // G. UI button Home (păstrat din proiect)
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
private fun HudTop(
    modifier: Modifier,
    beatPhase: Float,
    jam: Float,
    isFinalJam: Boolean
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Beat bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        ) {
            // Background
            drawRect(Color.Black.copy(alpha = 0.25f), size = size)

            // Tick marks
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

            // Moving indicator
            val x = (beatPhase.coerceIn(0f, 1f)) * size.width
            drawLine(
                color = if (isFinalJam) Color.White else Color.White.copy(alpha = 0.85f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 4f
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Jam meter
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

private enum class HitQuality(val label: String, val color: Color) {
    PERFECT("Perfect", Color(0xFF00E5FF)),
    GOOD("Good", Color(0xFFFFEB3B)),
    MISS("Miss", Color(0xFFFF5252)),
    NONE("", Color.Transparent)
}

/**
 * Muzicant (sprite sheet) + overlay UI (combo + last hit).
 *
 * Click:
 *  - pornește (dacă era oprit) și e evaluat timingul (Perfect/Good/Miss).
 * Double-click:
 *  - oprește muzicantul.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MusicianCharacter(
    frames: List<ImageBitmap>,
    isPlaying: Boolean,
    combo: Int,
    lastHit: HitQuality,
    lastHitAtNanos: Long,
    label: String,
    onAnchor: (Offset) -> Unit,
    onClick: () -> Unit,
    onDoubleTap: () -> Unit
) {
    val characterSize = 220.dp
    val scaleAnim = remember { Animatable(1f) }
    var currentFrame by remember { mutableIntStateOf(0) }

    // Frame animator (frame-synced)
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
            val frameDuration = if (isPlaying) 0.060f else 0.10f
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

    // Micro feedback la start/stop
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
                onClick = {
                    // micro feedback la click
                    onClick()
                },
                onDoubleClick = {
                    onDoubleTap()
                }
            )
            .onGloballyPositioned { coordinates ->
                val pos = coordinates.positionInRoot()
                val size = coordinates.size
                // Anchor: aproximăm instrumentul în partea dreaptă / sus a personajului
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
            Text("Loading...", color = Color.White)
        }

        // Overlay: combo + last hit
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
            val showHit = lastHit != HitQuality.NONE && (System.nanoTime() - lastHitAtNanos) < 900_000_000L
            if (showHit) {
                Text(
                    text = lastHit.label,
                    color = lastHit.color
                )
            }
        }
    }
}

/**
 * Split simplu sprite-sheet în frames.
 * safetyCrop = true: taie 1px margine (ajută dacă sheet-ul are "bleeding").
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

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

/**
 * Particule VFX.
 * - NOTE: folosește imaginea notei (tint dinamic + rotație).
 * - CONFETTI: dreptunghiuri colorate + rotație.
 */
private class Particle private constructor(
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

        // Basic physics
        val dragFactor = (1f - drag * dt).coerceIn(0.0f, 1.0f)
        vx *= dragFactor
        vy = (vy + gravity * dt) * dragFactor

        x += vx * dt
        y += vy * dt

        rotationDeg += rotationSpeedDeg * dt

        if (kind == Kind.NOTE) {
            // gentle wave sideways around baseX
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

                // subtle sparkle
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
            Color(0xFFFFEB3B), // Galben
            Color(0xFF03A9F4), // Albastru
            Color(0xFFFF4081), // Roz
            Color(0xFF4CAF50), // Verde
            Color(0xFFFF9800), // Portocaliu
            Color(0xFFB388FF), // Mov
            Color(0xFF69F0AE)  // Mint
        )

        fun note(origin: Offset, jam: Float, finale: Boolean, extraScatter: Boolean): Particle {
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
            return Particle(
                kind = Kind.NOTE,
                x = origin.x,
                y = origin.y,
                vx = vx,
                vy = vy,
                baseX = origin.x,
                waveAmp = waveAmp,
                waveFreq = waveFreq,
                rotationDeg = Random.nextFloat() * 360f,
                rotationSpeedDeg = (-240f + Random.nextFloat() * 480f),
                size = size,
                life = life,
                age = 0f,
                color = palette.random(),
                gravity = -40f, // notes float up, so slight negative gravity
                drag = 0.22f
            )
        }

        fun confettiBurst(center: Offset): Particle {
            val angle = Random.nextDouble(0.0, 2.0 * PI).toFloat()
            val speed = 240f + Random.nextFloat() * 720f
            val vx = cos(angle) * speed
            val vy = sin(angle) * speed - 260f
            val size = 10f + Random.nextFloat() * 14f
            val life = 1.6f + Random.nextFloat() * 1.4f
            return Particle(
                kind = Kind.CONFETTI,
                x = center.x,
                y = center.y,
                vx = vx,
                vy = vy,
                baseX = center.x,
                waveAmp = 0f,
                waveFreq = 0f,
                rotationDeg = Random.nextFloat() * 360f,
                rotationSpeedDeg = (-360f + Random.nextFloat() * 720f),
                size = size,
                life = life,
                age = 0f,
                color = palette.random(),
                gravity = 880f,
                drag = 0.08f
            )
        }

        fun confettiAmbient(rootWidthPx: Float): Particle {
            val x = Random.nextFloat() * rootWidthPx
            val y = -40f
            val vx = (-80f + Random.nextFloat() * 160f)
            val vy = (70f + Random.nextFloat() * 230f)
            val size = 9f + Random.nextFloat() * 10f
            val life = 1.3f + Random.nextFloat() * 1.0f
            return Particle(
                kind = Kind.CONFETTI,
                x = x,
                y = y,
                vx = vx,
                vy = vy,
                baseX = x,
                waveAmp = 0f,
                waveFreq = 0f,
                rotationDeg = Random.nextFloat() * 360f,
                rotationSpeedDeg = (-260f + Random.nextFloat() * 520f),
                size = size,
                life = life,
                age = 0f,
                color = palette.random(),
                gravity = 920f,
                drag = 0.06f
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
