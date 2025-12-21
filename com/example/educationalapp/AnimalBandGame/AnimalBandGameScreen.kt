package com.example.educationalapp.AnimalBandGame

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.educationalapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Păstrează semnătura compatibilă cu ce aveai înainte:
 * AnimalBandGame(onHome=...)
 */
@Composable
fun AnimalBandGame(
    viewModel: AnimalBandViewModel = hiltViewModel(),
    onHome: () -> Unit
) {
    val context = LocalContext.current

    var loaded by remember { mutableStateOf(false) }
    var sheets by remember { mutableStateOf<Map<MusicianId, LoadedSheet>>(emptyMap()) }
    var noteImage by remember { mutableStateOf<ImageBitmap?>(null) }

    // anchors pentru VFX
    var frogAnchor by remember { mutableStateOf(Offset.Zero) }
    var bearAnchor by remember { mutableStateOf(Offset.Zero) }
    var catAnchor by remember { mutableStateOf(Offset.Zero) }

    val vfx = remember { BandVfxState() }

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

            // beatPhase
            val period = viewModel.beatPeriodNanos
            beatPhase = if (period > 0) ((t % period).toDouble() / period.toDouble()).toFloat() else 0f

            // beat boundary => spawn notes for performing musicians
            val beatIndex = if (period > 0) (t / period) else 0L
            if (beatIndex != lastBeatIndex) {
                lastBeatIndex = beatIndex
                if (viewModel.frog.performing) BandVfx.spawnNoteBurst(vfx, frogAnchor)
                if (viewModel.bear.performing) BandVfx.spawnNoteBurst(vfx, bearAnchor)
                if (viewModel.cat.performing) BandVfx.spawnNoteBurst(vfx, catAnchor)
            }

            // final jam shockwave once
            if (viewModel.isFinalJam && vfx.shockwaves.isEmpty()) {
                BandVfx.spawnShockwave(vfx, Offset(540f, 800f)) // fallback; vizual ok
            }

            BandVfx.update(vfx, dt)
        }
    }

    // bounce global (sin pe beat)
    val bouncePx = (-sin(beatPhase * 2f * PI).toFloat() * (8f + viewModel.jam * 14f))

    Box(Modifier.fillMaxSize()) {
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
            // loading
        } else {
            BandHud(
                beatPhase = beatPhase,
                jam = viewModel.jam,
                finalJam = viewModel.isFinalJam,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(0, bouncePx.roundToInt()) }
                    .padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                MusicianCard(
                    label = "Frog",
                    loaded = sheets.getValue(MusicianId.FROG),
                    enabled = viewModel.frog.enabled,
                    performing = viewModel.frog.performing,
                    combo = viewModel.frog.combo,
                    onTap = { viewModel.onMusicianTap(MusicianId.FROG, nowNanos) },
                    onDoubleTap = { viewModel.toggleMusician(MusicianId.FROG) },
                    onLongPress = { viewModel.togglePerforming(MusicianId.FROG) }, // opțional
                    onAnchor = { frogAnchor = it }
                )

                MusicianCard(
                    label = "Bear",
                    loaded = sheets.getValue(MusicianId.BEAR),
                    enabled = viewModel.bear.enabled,
                    performing = viewModel.bear.performing,
                    combo = viewModel.bear.combo,
                    onTap = { viewModel.onMusicianTap(MusicianId.BEAR, nowNanos) },
                    onDoubleTap = { viewModel.toggleMusician(MusicianId.BEAR) },
                    onLongPress = { viewModel.togglePerforming(MusicianId.BEAR) },
                    onAnchor = { bearAnchor = it }
                )

                MusicianCard(
                    label = "Cat",
                    loaded = sheets.getValue(MusicianId.CAT),
                    enabled = viewModel.cat.enabled,
                    performing = viewModel.cat.performing,
                    combo = viewModel.cat.combo,
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
private fun MusicianCard(
    label: String,
    loaded: LoadedSheet,
    enabled: Boolean,
    performing: Boolean,
    combo: Int,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
    onAnchor: (Offset) -> Unit
) {
    val frames = loaded.frames
    val spec = loaded.spec

    var localFrame by remember { mutableIntStateOf(0) }

    // animator local, cu delta real
    LaunchedEffect(enabled, performing, frames.size) {
        if (frames.isEmpty()) return@LaunchedEffect
        localFrame = 0

        val range = if (enabled && performing) spec.play else spec.idle
        val framePeriod = if (enabled && performing) spec.playFramePeriodSec else spec.idleFramePeriodSec

        var last = 0L
        var acc = 0f

        while (isActive) {
            val now = withFrameNanos { it }
            if (last == 0L) { last = now; continue }
            val dt = ((now - last).toDouble() / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
            last = now

            acc += dt
            if (acc >= framePeriod) {
                acc -= framePeriod
                localFrame = (localFrame + 1) % (range.count.coerceAtLeast(1))
            }
        }
    }

    val interaction = remember { MutableInteractionSource() }
    val scale = if (enabled) 1f else 0.92f

    Box(
        modifier = Modifier
            .size(220.dp)
            .scale(scale)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onTap,
                onDoubleClick = onDoubleTap,
                onLongClick = onLongPress
            )
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                val size = coords.size
                onAnchor(Offset(pos.x + size.width * 0.7f, pos.y + size.height * 0.35f))
            },
        contentAlignment = Alignment.Center
    ) {
        if (frames.isNotEmpty()) {
            val range = if (enabled && performing) spec.play else spec.idle
            val idx = range.safeIndex(localFrame).coerceIn(0, frames.lastIndex)
            Image(bitmap = frames[idx], contentDescription = label, modifier = Modifier.fillMaxSize())
        }
        // dacă vrei text combo, îl poți adăuga aici (minimalism pentru build stabil)
    }
}
