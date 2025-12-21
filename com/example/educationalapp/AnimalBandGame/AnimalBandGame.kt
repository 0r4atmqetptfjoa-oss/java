package com.example.educationalapp.AnimalBandGame

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
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
import androidx.compose.runtime.withFrameNanos
import kotlin.math.sin
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.math.PI
import kotlin.random.Random

@Composable
fun AnimalBandGame(
    viewModel: AnimalBandViewModel = hiltViewModel(),
    onHome: () -> Unit
) {
    val context = LocalContext.current

    // Resurse
    var frogFrames by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var bearFrames by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var catFrames by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var noteImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var assetsLoaded by remember { mutableStateOf(false) }

    // VFX
    val particles = remember { mutableStateListOf<BandParticle>() }
    val shockwaves = remember { mutableStateListOf<BandShockwave>() }
    var rootSizePx by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    
    // Ancore
    var frogAnchor by remember { mutableStateOf(Offset.Zero) }
    var bearAnchor by remember { mutableStateOf(Offset.Zero) }
    var catAnchor by remember { mutableStateOf(Offset.Zero) }

    // Ceas
    var clockNanos by remember { mutableStateOf(0L) }
    var lastBeatIndex by remember { mutableStateOf(-1L) }
    val beatPhase by derivedStateOf {
        if (clockNanos <= 0L) 0f else ((clockNanos % viewModel.beatPeriodNanos).toDouble() / viewModel.beatPeriodNanos.toDouble()).toFloat()
    }

    // --- FIX CRITIC: inScaled = false ---
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val opts = BitmapFactory.Options().apply { inScaled = false }
            
            val frogBmp = BitmapFactory.decodeResource(context.resources, R.drawable.band_frog_sheet, opts)
            val bearBmp = BitmapFactory.decodeResource(context.resources, R.drawable.band_bear_sheet, opts)
            val catBmp = BitmapFactory.decodeResource(context.resources, R.drawable.band_cat_sheet, opts)
            val noteBmp = BitmapFactory.decodeResource(context.resources, R.drawable.vfx_music_note, opts)

            val fFrog = splitSpriteSheet(frogBmp, 4, 6).map { it.asImageBitmap() }
            val fBear = splitSpriteSheet(bearBmp, 4, 6).map { it.asImageBitmap() }
            val fCat = splitSpriteSheet(catBmp, 4, 6).map { it.asImageBitmap() }
            
            withContext(Dispatchers.Main) {
                frogFrames = fFrog
                bearFrames = fBear
                catFrames = fCat
                noteImage = noteBmp?.asImageBitmap()
                assetsLoaded = true
            }
        }
    }

    // Game Loop
    LaunchedEffect(assetsLoaded) {
        if (!assetsLoaded) return@LaunchedEffect
        var lastFrame = 0L
        while (isActive) {
            val now = withFrameNanos { it }
            clockNanos = now
            if (lastFrame == 0L) { lastFrame = now; continue }
            val dt = ((now - lastFrame).toDouble() / 1_000_000_000.0).toFloat().coerceIn(0f, 0.05f)
            lastFrame = now

            val beatIndex = now / viewModel.beatPeriodNanos
            if (beatIndex != lastBeatIndex) {
                lastBeatIndex = beatIndex
                // Beat visual effects
                if (viewModel.frogPlaying) BandVfxUtils.spawnNoteBurst(particles, frogAnchor, viewModel.jam)
                if (viewModel.bearPlaying) BandVfxUtils.spawnNoteBurst(particles, bearAnchor, viewModel.jam)
                if (viewModel.catPlaying) BandVfxUtils.spawnNoteBurst(particles, catAnchor, viewModel.jam)
            }

            if (viewModel.jam >= 1f && !viewModel.isFinalJam) {
                viewModel.isFinalJam = true
                shockwaves.add(BandShockwave(Offset(rootSizePx.width/2f, rootSizePx.height/2f), 0f, 1000f, 20f, 1f))
            }
            
            particles.removeAll { !it.alive }
            particles.forEach { it.update(dt) }
            shockwaves.removeAll { !it.alive }
            shockwaves.forEach { it.update(dt) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootSizePx = it.size },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_music_stage),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (!assetsLoaded) {
            Text("Loading Band...", color = Color.White)
        } else {
            BandHud(Modifier.align(Alignment.TopCenter), beatPhase, viewModel.jam, viewModel.isFinalJam)

            // Bounce effect
            val bouncePx = (-sin(beatPhase * 2f * PI).toFloat() * (8f + viewModel.jam * 14f) * (if (viewModel.activeMusiciansCount() > 0) 1f else 0f))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(0, bouncePx.roundToInt()) }
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                MusicianView(frogFrames, viewModel.frogPlaying, viewModel.frogCombo, "Frog",
                    onClick = { viewModel.onMusicianClick(MusicianId.FROG, clockNanos) },
                    onDouble = { viewModel.toggleMusician(MusicianId.FROG) },
                    onAnchor = { frogAnchor = it }
                )
                MusicianView(bearFrames, viewModel.bearPlaying, viewModel.bearCombo, "Bear",
                    onClick = { viewModel.onMusicianClick(MusicianId.BEAR, clockNanos) },
                    onDouble = { viewModel.toggleMusician(MusicianId.BEAR) },
                    onAnchor = { bearAnchor = it }
                )
                MusicianView(catFrames, viewModel.catPlaying, viewModel.catCombo, "Cat",
                    onClick = { viewModel.onMusicianClick(MusicianId.CAT, clockNanos) },
                    onDouble = { viewModel.toggleMusician(MusicianId.CAT) },
                    onAnchor = { catAnchor = it }
                )
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val note = noteImage
                shockwaves.forEach { it.draw(this) }
                particles.forEach { it.draw(this, note) }
            }
        }
        
        Image(
            painter = painterResource(id = R.drawable.ui_button_home),
            contentDescription = "Back",
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp).size(64.dp).clickable { onHome() }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicianView(
    frames: List<ImageBitmap>,
    playing: Boolean,
    combo: Int,
    label: String,
    onClick: () -> Unit,
    onDouble: () -> Unit,
    onAnchor: (Offset) -> Unit
) {
    var frameIndex by remember { mutableIntStateOf(0) }
    val scale = remember { Animatable(1f) }

    // --- FIX ANIMATION SPEED ---
    LaunchedEffect(playing) {
        var acc = 0f
        while (isActive) {
            val dt = 0.016f 
            withFrameNanos { } 
            acc += dt
            // 0.12f = approx 8 FPS (miscare mai lenta, stil cartoon)
            if (acc >= 0.12f) { 
                acc = 0f
                if (playing) {
                    frameIndex = if (frameIndex < 12) 12 else (frameIndex + 1).takeIf { it <= 23 } ?: 12
                } else {
                    frameIndex = (frameIndex + 1) % 12
                }
            }
        }
    }
    
    LaunchedEffect(combo) {
        if(combo > 0) {
            scale.snapTo(1.1f)
            scale.animateTo(1f)
        }
    }

    Box(
        modifier = Modifier
            .size(220.dp)
            .scale(scale.value)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onDoubleClick = onDouble
            )
            .onGloballyPositioned { coordinates ->
                val pos = coordinates.positionInRoot()
                val size = coordinates.size
                // Calculam ancora aproximativ la "gura" sau centrul instrumentului
                onAnchor(Offset(pos.x + size.width * 0.7f, pos.y + size.height * 0.4f))
            },
        contentAlignment = Alignment.Center
    ) {
        if (frames.isNotEmpty()) {
            Image(bitmap = frames[frameIndex % frames.size], contentDescription = label, modifier = Modifier.fillMaxSize())
        }
        if (combo > 1) {
            Text("x$combo", color = Color.White, modifier = Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
fun BandHud(modifier: Modifier, phase: Float, jam: Float, finalJam: Boolean) {
    Column(modifier.width(300.dp)) {
        Canvas(Modifier.fillMaxWidth().height(10.dp)) {
            drawRect(Color.Gray.copy(0.5f))
            val x = size.width * phase
            drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), 4f)
        }
        Spacer(Modifier.height(4.dp))
        Canvas(Modifier.fillMaxWidth().height(10.dp)) {
            drawRect(Color.Gray.copy(0.5f))
            drawRect(if(finalJam) Color.Red else Color.Cyan, size = size.copy(width = size.width * jam))
        }
    }
}

// Helpers
fun splitSpriteSheet(sheet: Bitmap?, rows: Int, cols: Int): List<Bitmap> {
    if (sheet == null || rows <= 0 || cols <= 0) return emptyList()
    val w = sheet.width / cols
    val h = sheet.height / rows
    if (w <= 0 || h <= 0) return emptyList()
    
    val list = mutableListOf<Bitmap>()
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            list.add(Bitmap.createBitmap(sheet, c * w, r * h, w, h))
        }
    }
    return list
}

// VFX Utils
object BandVfxUtils {
    fun spawnNoteBurst(list: MutableList<BandParticle>, pos: Offset, jam: Float) {
        if (pos == Offset.Zero) return
        repeat(3) { list.add(BandParticle.note(pos)) }
    }
}

class BandParticle(var x: Float, var y: Float) { 
    var alive = true
    var age = 0f
    var vx = (Random.nextFloat() - 0.5f) * 200f
    var vy = -300f - Random.nextFloat() * 200f
    
    fun update(dt: Float) { 
        age += dt
        x += vx * dt
        y += vy * dt
        vy += 800f * dt // Gravity
        if(age > 1.5f) alive = false 
    }
    
    fun draw(scope: androidx.compose.ui.graphics.drawscope.DrawScope, img: ImageBitmap?) {
        val alpha = (1f - age / 1.5f).coerceIn(0f, 1f)
        if(img != null) {
            scope.withTransform({
                translate(x, y)
                scale(1f - age*0.3f)
            }) {
                drawImage(img, Offset(-img.width/2f, -img.height/2f), alpha = alpha)
            }
        } else {
            scope.drawCircle(Color.White.copy(alpha), 10f, Offset(x,y))
        }
    }
    companion object { fun note(pos: Offset) = BandParticle(pos.x, pos.y) }
}

class BandShockwave(val center: Offset, var radius: Float, val speed: Float, val width: Float, var alpha: Float) { 
    var alive = true
    fun update(dt: Float) { radius += speed * dt; alpha -= dt * 1.5f; if(alpha<=0) alive=false }
    fun draw(scope: androidx.compose.ui.graphics.drawscope.DrawScope) { 
        scope.drawCircle(Color.White.copy(alpha.coerceIn(0f,1f)), radius, center, style = Stroke(width)) 
    }
}