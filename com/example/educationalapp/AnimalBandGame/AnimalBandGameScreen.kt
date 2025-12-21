package com.example.educationalapp.AnimalBandGame

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.educationalapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * V2: animație din sprite sheet prin source-rect + crossfade între frame-uri,
 * și poziționare “pe scenă” (background) cu coordonate relative.
 */
@Composable
fun AnimalBandGame(
    viewModel: AnimalBandViewModel = hiltViewModel(),
    onHome: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var loaded by remember { mutableStateOf(false) }
    var sheets by remember { mutableStateOf<Map<MusicianId, LoadedSheet>>(emptyMap()) }
    var noteImage by remember { mutableStateOf<ImageBitmap?>(null) }

    // anchors pentru VFX
    var frogAnchor by remember { mutableStateOf(Offset.Zero) }
    var bearAnchor by remember { mutableStateOf(Offset.Zero) }
    var catAnchor by remember { mutableStateOf(Offset.Zero) }

    val vfx = remember { BandVfxState() }

    // stage size (pentru shockwave centrat)
    var stageSize by remember { mutableStateOf(IntSize.Zero) }

    // timing loop
    var nowNanos by remember { mutableLongStateOf(0L) }
    var beatPhase by remember { mutableFloatStateOf(0f) }
    var lastBeatIndex by remember { mutableLongStateOf(-1L) }

    // Load assets (IO)
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val all = AnimalBandAssets.loadAll(context)
            val noteBmp = runCatching {
                val opts = BitmapFactory.Options().apply { inScaled = false }
                BitmapFactory.decodeResource(context.resources, R.drawable.vfx_music_note, opts)
            }.getOrNull()

            withContext(Dispatchers.Main) {
                sheets = all
                noteImage = noteBmp?.asImageBitmap()
                loaded = true
            }
        }
    }

    // Main frame loop: update clock, beatPhase, vfx
    LaunchedEffect(loaded) {
        if (!loaded) return@LaunchedEffect

        var lastFrame = 0L
        while (isActive) {
            val t = withFrameNanos { it }
            nowNanos = t

            if (lastFrame == 0L) {
                lastFrame = t
                continue
            }
            val dt = ((t - lastFrame).toDouble() / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
            lastFrame = t

            beatPhase = viewModel.beatPhase(t)

            // beat boundary => spawn notes for performing musicians
            val beatIndex = viewModel.beatIndex(t)
            if (beatIndex != lastBeatIndex) {
                lastBeatIndex = beatIndex
                if (viewModel.frog.performing && frogAnchor != Offset.Zero) BandVfx.spawnNoteBurst(vfx, frogAnchor)
                if (viewModel.bear.performing && bearAnchor != Offset.Zero) BandVfx.spawnNoteBurst(vfx, bearAnchor)
                if (viewModel.cat.performing && catAnchor != Offset.Zero) BandVfx.spawnNoteBurst(vfx, catAnchor)
            }

            // final jam shockwave once (centrat pe stage)
            if (viewModel.isFinalJam && vfx.shockwaves.isEmpty()) {
                val center = if (stageSize != IntSize.Zero) {
                    Offset(stageSize.width * 0.5f, stageSize.height * 0.70f)
                } else {
                    Offset(540f, 800f)
                }
                BandVfx.spawnShockwave(vfx, center)
            }

            BandVfx.update(vfx, dt)
        }
    }

    val bouncePx = (-sin(beatPhase * 2f * PI).toFloat() * (8f + viewModel.jam * 14f))
    val bounceDp = with(density) { bouncePx.toDp() }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { stageSize = it }
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_music_stage),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Image(
            painter = painterResource(id = R.drawable.ui_button_home),
            contentDescription = "Home",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(64.dp)
                .clickable { onHome() }
        )

        if (!loaded || sheets.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BasicText("Loading…")
            }
        } else {
            val songTimeNanos = viewModel.songTimeNanos(nowNanos)

            BandHud(
                beatPhase = beatPhase,
                jam = viewModel.jam,
                finalJam = viewModel.isFinalJam,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )

            // Poziționare relativă pe scenă
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val w = maxWidth
                val h = maxHeight

                // “linia de picioare” (aprox. suprafața scenei)
                val floorY = h * 0.71f

                // Dimensiuni (pe înălțime) – păstrăm aspect ratio al frame-ului!
                val bearH = (h * 0.34f).coerceIn(200.dp, 360.dp)
                val sideH = (h * 0.30f).coerceIn(180.dp, 330.dp)

                // FROG (stânga)
                StageMusician(
                    label = "Frog",
                    loaded = sheets.getValue(MusicianId.FROG),
                    enabled = viewModel.frog.enabled,
                    performing = viewModel.frog.performing,
                    combo = viewModel.frog.combo,
                    judgement = viewModel.frog.lastJudgement,
                    judgementAtNanos = viewModel.frog.lastJudgementAtNanos,
                    nowNanos = nowNanos,
                    songTimeNanos = songTimeNanos,
                    beatPhase = beatPhase,
                    jam = viewModel.jam,
                    height = sideH,
                    xCenter = w * 0.26f,
                    feetY = floorY,
                    z = 0.9f,
                    anchorFrac = Offset(0.55f, 0.62f),
                    bounce = bounceDp,
                    onTap = { viewModel.onMusicianTap(MusicianId.FROG, nowNanos) },
                    onDoubleTap = { viewModel.toggleMusician(MusicianId.FROG) },
                    onLongPress = { viewModel.togglePerforming(MusicianId.FROG) },
                    onAnchor = { frogAnchor = it }
                )

                // BEAR (centru)
                StageMusician(
                    label = "Bear",
                    loaded = sheets.getValue(MusicianId.BEAR),
                    enabled = viewModel.bear.enabled,
                    performing = viewModel.bear.performing,
                    combo = viewModel.bear.combo,
                    judgement = viewModel.bear.lastJudgement,
                    judgementAtNanos = viewModel.bear.lastJudgementAtNanos,
                    nowNanos = nowNanos,
                    songTimeNanos = songTimeNanos,
                    beatPhase = beatPhase,
                    jam = viewModel.jam,
                    height = bearH,
                    xCenter = w * 0.50f,
                    feetY = floorY,
                    z = 1.0f,
                    anchorFrac = Offset(0.62f, 0.42f),
                    bounce = bounceDp,
                    onTap = { viewModel.onMusicianTap(MusicianId.BEAR, nowNanos) },
                    onDoubleTap = { viewModel.toggleMusician(MusicianId.BEAR) },
                    onLongPress = { viewModel.togglePerforming(MusicianId.BEAR) },
                    onAnchor = { bearAnchor = it }
                )

                // CAT (dreapta)
                StageMusician(
                    label = "Cat",
                    loaded = sheets.getValue(MusicianId.CAT),
                    enabled = viewModel.cat.enabled,
                    performing = viewModel.cat.performing,
                    combo = viewModel.cat.combo,
                    judgement = viewModel.cat.lastJudgement,
                    judgementAtNanos = viewModel.cat.lastJudgementAtNanos,
                    nowNanos = nowNanos,
                    songTimeNanos = songTimeNanos,
                    beatPhase = beatPhase,
                    jam = viewModel.jam,
                    height = sideH,
                    xCenter = w * 0.74f,
                    feetY = floorY,
                    z = 0.9f,
                    anchorFrac = Offset(0.55f, 0.55f),
                    bounce = bounceDp,
                    onTap = { viewModel.onMusicianTap(MusicianId.CAT, nowNanos) },
                    onDoubleTap = { viewModel.toggleMusician(MusicianId.CAT) },
                    onLongPress = { viewModel.togglePerforming(MusicianId.CAT) },
                    onAnchor = { catAnchor = it }
                )
            }

            Canvas(Modifier.fillMaxSize()) {
                BandVfx.draw(vfx, this, noteImage)
            }
        }
    }
}

@Composable
private fun StageMusician(
    label: String,
    loaded: LoadedSheet,
    enabled: Boolean,
    performing: Boolean,
    combo: Int,
    judgement: Judgement?,
    judgementAtNanos: Long,
    nowNanos: Long,
    songTimeNanos: Long,
    beatPhase: Float,
    jam: Float,
    height: Dp,
    xCenter: Dp,
    feetY: Dp,
    z: Float,
    anchorFrac: Offset,
    bounce: Dp,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
    onAnchor: (Offset) -> Unit
) {
    val spec = loaded.spec
    val frameAspect = loaded.frameW.toFloat() / loaded.frameH.toFloat()
    val width = height * frameAspect

    val interaction = remember { MutableInteractionSource() }

    val pulse = sin(beatPhase * 2f * PI).toFloat()
    val baseScale = if (enabled) 1f else 0.92f
    // mic “squash & stretch” pe beat când cântă (arată mai viu)
    val lively = if (performing) (0.035f + jam * 0.06f) else 0f
    val scaleX = baseScale * (1f + lively * pulse)
    val scaleY = baseScale * (1f - lively * pulse)

    // top-left dintr-un punct “center + feet”
    val x = xCenter - (width / 2f)
    val y = (feetY - height) + bounce

    val showJudgement = judgement != null &&
        judgementAtNanos > 0L &&
        (nowNanos - judgementAtNanos) in 0L..700_000_000L

    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(width = width, height = height)
            .zIndex(z)
            .scale(scaleX = scaleX, scaleY = scaleY)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onTap,
                onDoubleClick = onDoubleTap,
                onLongClick = onLongPress
            )
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                val s = coords.size
                // anchor relativ (instrument / centru interes)
                onAnchor(Offset(pos.x + s.width * anchorFrac.x, pos.y + s.height * anchorFrac.y))
            },
        contentAlignment = Alignment.Center
    ) {
        // shadow “sub picioare” pentru integrare în fundal
        Canvas(Modifier.fillMaxSize()) {
            val shadowW = size.width * 0.55f
            val shadowH = size.height * 0.10f
            val left = (size.width - shadowW) * 0.5f
            val top = size.height * 0.88f
            drawOval(
                color = Color.Black.copy(alpha = if (enabled) 0.18f else 0.10f),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(shadowW, shadowH)
            )
        }

        SpriteSheetActor(
            sheet = loaded.sheet,
            spec = spec,
            frameW = loaded.frameW,
            frameH = loaded.frameH,
            enabled = enabled,
            performing = performing,
            songTimeNanos = songTimeNanos,
            modifier = Modifier.fillMaxSize()
        )

        if (showJudgement) {
            val txt = when (judgement) {
                Judgement.PERFECT -> "Perfect"
                Judgement.GOOD -> "Good"
                Judgement.MISS -> "Miss"
                null -> ""
            }
            BasicText(txt, modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp))
        }

        if (combo >= 2) {
            BasicText("x$combo", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp))
        }
    }
}

@Composable
private fun SpriteSheetActor(
    sheet: ImageBitmap,
    spec: SpriteSheetSpec,
    frameW: Int,
    frameH: Int,
    enabled: Boolean,
    performing: Boolean,
    songTimeNanos: Long,
    modifier: Modifier = Modifier
) {
    val range = if (enabled && performing) spec.play else spec.idle
    val framePeriodSec = if (enabled && performing) spec.playFramePeriodSec else spec.idleFramePeriodSec
    val framePeriodNanos = (framePeriodSec * 1_000_000_000f).toLong().coerceAtLeast(1L)

    // poziție fracționară => crossfade între două frame-uri
    val pos = (songTimeNanos.toDouble() / framePeriodNanos.toDouble())
    val i0 = floor(pos).toLong()
    val frac = (pos - i0).toFloat().coerceIn(0f, 1f)

    fun idxFor(i: Long): Int {
        if (range.count <= 0) return range.start
        val local = ((i % range.count).toInt()).coerceAtLeast(0)
        return range.start + local
    }

    val idx0 = idxFor(i0)
    val idx1 = idxFor(i0 + 1)

    Canvas(modifier = modifier) {
        val dst = IntSize(size.width.roundToInt(), size.height.roundToInt()).let {
            IntSize(it.width.coerceAtLeast(1), it.height.coerceAtLeast(1))
        }

        fun drawFrame(index: Int, alpha: Float) {
            val col = index % spec.cols
            val row = index / spec.cols
            val srcOffset = IntOffset(col * frameW, row * frameH)
            val srcSize = IntSize(frameW, frameH)
            drawImageRect(
                image = sheet,
                srcOffset = srcOffset,
                srcSize = srcSize,
                dstSize = dst,
                alpha = alpha.coerceIn(0f, 1f),
                filterQuality = FilterQuality.High
            )
        }

        // Crossfade: frame0 -> frame1
        drawFrame(idx0, 1f - frac)
        drawFrame(idx1, frac)
    }
}
