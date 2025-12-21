package com.example.educationalapp.puzzle

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.hypot
import kotlin.math.min

enum class PuzzlePhase { START, PLAYING, COMPLETE }

enum class PuzzleEvent { None, Snap, Complete }

data class PuzzlePiece(
    val id: Int,
    val bitmap: ImageBitmap,
    val currentX: Float,
    val currentY: Float,
    val targetX: Float,
    val targetY: Float,
    val isLocked: Boolean,
    val width: Int,
    val height: Int,
    val config: PieceConfig,
    val z: Int
)

data class PuzzleUiState(
    val pieces: List<PuzzlePiece> = emptyList(),
    val currentThemeResId: Int = PuzzleAssets.themes.first().resId,
    val isLoading: Boolean = false,
    val phase: PuzzlePhase = PuzzlePhase.START,
    val event: PuzzleEvent = PuzzleEvent.None,
    val lastSnapId: Int? = null,
    val magnetDistancePx: Float = 0f,
    val draggingId: Int? = null
)

@HiltViewModel
class PuzzleViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(PuzzleUiState())
    val uiState: StateFlow<PuzzleUiState> = _uiState.asStateFlow()

    // z-order counter to bring dragged piece on top
    private var zCounter = 0

    // last known geometry for quick restart
    private var lastBoardW = 0f
    private var lastBoardH = 0f
    private var lastTrayX = 0f
    private var lastTrayY = 0f
    private var lastTrayW = 0f
    private var lastTrayH = 0f

    fun resetToStart() {
        _uiState.value = _uiState.value.copy(
            phase = PuzzlePhase.START,
            pieces = emptyList(),
            isLoading = false,
            event = PuzzleEvent.None,
            lastSnapId = null,
            draggingId = null
        )
    }

    fun consumeEvent() {
        _uiState.value = _uiState.value.copy(event = PuzzleEvent.None, lastSnapId = null)
    }

    fun startGame(
        context: Context,
        boardWidth: Float,
        boardHeight: Float,
        trayX: Float,
        trayY: Float,
        trayW: Float,
        trayH: Float
    ) {
        lastBoardW = boardWidth
        lastBoardH = boardHeight
        lastTrayX = trayX
        lastTrayY = trayY
        lastTrayW = trayW
        lastTrayH = trayH

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, phase = PuzzlePhase.PLAYING, event = PuzzleEvent.None)
            val theme = PuzzleAssets.getRandomTheme()

            val pieces = cutBitmapUnified(
                context = context,
                resId = theme.resId,
                rows = 4,
                cols = 4,
                boardW = boardWidth,
                boardH = boardHeight,
                trayX = trayX,
                trayY = trayY,
                trayW = trayW,
                trayH = trayH
            )

            val cell = min(boardWidth / 4f, boardHeight / 4f)
            val magnet = cell * 0.24f

            _uiState.value = _uiState.value.copy(
                pieces = pieces,
                currentThemeResId = theme.resId,
                isLoading = false,
                phase = PuzzlePhase.PLAYING,
                magnetDistancePx = magnet
            )
        }
    }

    fun restartLast(context: Context) {
        if (lastBoardW <= 0f || lastBoardH <= 0f) {
            resetToStart()
            return
        }
        startGame(context, lastBoardW, lastBoardH, lastTrayX, lastTrayY, lastTrayW, lastTrayH)
    }

    fun onPieceDragStart(id: Int) {
        val cur = _uiState.value
        val newPieces = cur.pieces.map { p ->
            if (p.id == id && !p.isLocked) {
                zCounter += 1
                p.copy(z = zCounter)
            } else p
        }
        _uiState.value = cur.copy(pieces = newPieces, draggingId = id)
    }

    fun onPieceDrag(id: Int, dx: Float, dy: Float) {
        val cur = _uiState.value
        if (cur.phase != PuzzlePhase.PLAYING) return

        val snapDist = (min(lastBoardW / 4f, lastBoardH / 4f) * 0.16f).coerceAtLeast(10f)

        val newPieces = cur.pieces.map { p ->
            if (p.id != id || p.isLocked) return@map p

            var nx = p.currentX + dx
            var ny = p.currentY + dy

            // Soft magnet assist: if close, pull slightly towards target
            val d = hypot(nx - p.targetX, ny - p.targetY)
            if (d < cur.magnetDistancePx) {
                val t = 0.14f
                nx = nx + (p.targetX - nx) * t
                ny = ny + (p.targetY - ny) * t
            } else if (d < cur.magnetDistancePx + snapDist) {
                val t = 0.06f
                nx = nx + (p.targetX - nx) * t
                ny = ny + (p.targetY - ny) * t
            }

            p.copy(currentX = nx, currentY = ny)
        }

        _uiState.value = cur.copy(pieces = newPieces)
    }

    fun onPieceDrop(id: Int) {
        val cur = _uiState.value
        if (cur.phase != PuzzlePhase.PLAYING) return

        val cell = min(lastBoardW / 4f, lastBoardH / 4f)
        val snapDist = cell * 0.18f

        var snappedId: Int? = null
        val newPieces = cur.pieces.map { p ->
            if (p.id != id || p.isLocked) return@map p
            val d = hypot(p.currentX - p.targetX, p.currentY - p.targetY)
            if (d <= snapDist) {
                snappedId = p.id
                p.copy(currentX = p.targetX, currentY = p.targetY, isLocked = true)
            } else p
        }

        val allLocked = newPieces.isNotEmpty() && newPieces.all { it.isLocked }

        _uiState.value = cur.copy(
            pieces = newPieces,
            draggingId = null,
            event = when {
                allLocked -> PuzzleEvent.Complete
                snappedId != null -> PuzzleEvent.Snap
                else -> PuzzleEvent.None
            },
            lastSnapId = snappedId,
            phase = if (allLocked) PuzzlePhase.COMPLETE else PuzzlePhase.PLAYING
        )
    }

    // -------- Bitmap cutting (premium: masked bitmap + stroke + subtle shadow stroke) --------
    private suspend fun cutBitmapUnified(
        context: Context,
        resId: Int,
        rows: Int,
        cols: Int,
        boardW: Float,
        boardH: Float,
        trayX: Float,
        trayY: Float,
        trayW: Float,
        trayH: Float
    ): List<PuzzlePiece> = withContext(Dispatchers.IO) {

        // 1) Load efficiently
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, bounds)

        var sample = 1
        while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val loaded = BitmapFactory.decodeResource(context.resources, resId, opts)
            ?: return@withContext emptyList()

        // 2) Scale to board size (exact)
        val boardWInt = boardW.toInt().coerceAtLeast(1)
        val boardHInt = boardH.toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(loaded, boardWInt, boardHInt, true)

        // 3) Build consistent piece configs (edges complement each other)
        val configs = Array(rows) { Array(cols) { PieceConfig() } }.also { grid ->
            val rnd = java.util.Random()
            fun r(): Int = if (rnd.nextBoolean()) 1 else -1

            for (rIdx in 0 until rows) {
                for (cIdx in 0 until cols) {
                    var top = 0
                    var left = 0
                    if (rIdx > 0) {
                        // complement bottom of piece above
                        val above = grid[rIdx - 1][cIdx]
                        top = -above.bottom
                    }
                    if (cIdx > 0) {
                        val leftN = grid[rIdx][cIdx - 1]
                        left = -leftN.right
                    }

                    val right = if (cIdx == cols - 1) 0 else r()
                    val bottom = if (rIdx == rows - 1) 0 else r()

                    grid[rIdx][cIdx] = PieceConfig(top = top, right = right, bottom = bottom, left = left)
                }
            }
        }

        val baseW = boardW / cols.toFloat()
        val baseH = boardH / rows.toFloat()
        val paddingX = baseW / 3f
        val paddingY = baseH / 3f

        val fullW = (baseW + 2f * paddingX).toInt()
        val fullH = (baseH + 2f * paddingY).toInt()

        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val dstPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val clipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (min(fullW, fullH) * 0.018f).coerceAtLeast(2f)
            color = 0xCCFFFFFF.toInt()
        }

        val shadowStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokePaint.strokeWidth * 1.6f
            color = 0x33000000
        }

        val pieces = ArrayList<PuzzlePiece>(rows * cols)
        var id = 0

        for (rIdx in 0 until rows) {
            for (cIdx in 0 until cols) {

                val cfg = configs[rIdx][cIdx]

                // Source rect includes padding, clamped to bitmap
                val srcL = (cIdx * baseW - paddingX).toInt()
                val srcT = (rIdx * baseH - paddingY).toInt()
                val srcR = (srcL + fullW)
                val srcB = (srcT + fullH)

                val clampedL = srcL.coerceAtLeast(0)
                val clampedT = srcT.coerceAtLeast(0)
                val clampedR = srcR.coerceAtMost(scaled.width)
                val clampedB = srcB.coerceAtMost(scaled.height)

                val srcRect = Rect(clampedL, clampedT, clampedR, clampedB)

                // Destination rect shifts when clamped (so piece stays aligned)
                val dstL = (clampedL - srcL)
                val dstT = (clampedT - srcT)
                val dstRect = Rect(dstL, dstT, dstL + srcRect.width(), dstT + srcRect.height())

                val out = Bitmap.createBitmap(fullW, fullH, Bitmap.Config.ARGB_8888)
                val c = Canvas(out)

                // draw image region
                c.drawBitmap(scaled, srcRect, dstRect, dstPaint)

                // build mask
                val mask = Bitmap.createBitmap(fullW, fullH, Bitmap.Config.ARGB_8888)
                val mc = Canvas(mask)
                val path = createAndroidPath(cfg, fullW.toFloat(), fullH.toFloat())
                mc.drawPath(path, maskPaint)

                // apply mask
                c.drawBitmap(mask, 0f, 0f, clipPaint)

                // shadow stroke (slight offset)
                c.save()
                c.translate(1.2f, 1.2f)
                c.drawPath(path, shadowStrokePaint)
                c.restore()

                // bright stroke
                c.drawPath(path, strokePaint)

                // target position on board (board-local)
                val targetX = cIdx * baseW - paddingX
                val targetY = rIdx * baseH - paddingY

                // start in tray (board-local coordinates)
                val jitterX = (Math.random() * 24.0).toFloat()
                val jitterY = (Math.random() * 24.0).toFloat()

                val startX = trayX + 12f + jitterX + (Math.random() * (trayW - fullW - 24f).coerceAtLeast(8f)).toFloat()
                val startY = trayY + 40f + jitterY + (Math.random() * (trayH - fullH - 56f).coerceAtLeast(8f)).toFloat()

                pieces.add(
                    PuzzlePiece(
                        id = id++,
                        bitmap = out.asImageBitmap(),
                        currentX = startX,
                        currentY = startY,
                        targetX = targetX,
                        targetY = targetY,
                        isLocked = false,
                        width = fullW,
                        height = fullH,
                        config = cfg,
                        z = 0
                    )
                )
            }
        }

        // shuffle pieces in tray for variety
        pieces.shuffle()
        pieces
    }

    /**
     * Android Path version of PuzzleShape (same geometry assumptions as PuzzleShape.kt):
     * - Total size = Padding (1/5) + Body (3/5) + Padding (1/5)
     */
    private fun createAndroidPath(config: PieceConfig, w: Float, h: Float): AndroidPath {
        val baseW = w * (3f / 5f)
        val baseH = h * (3f / 5f)
        val offX = w * (1f / 5f)
        val offY = h * (1f / 5f)
        val bump = baseW / 3.5f

        val p = AndroidPath()
        p.moveTo(offX, offY)

        // TOP
        if (config.top == 0) {
            p.lineTo(offX + baseW, offY)
        } else {
            val dir = if (config.top == 1) -1f else 1f
            horizontalBump(p, offX, offY, baseW, bump, dir)
        }

        // RIGHT
        if (config.right == 0) {
            p.lineTo(offX + baseW, offY + baseH)
        } else {
            val dir = if (config.right == 1) 1f else -1f
            verticalBump(p, offX + baseW, offY, baseH, bump, dir)
        }

        // BOTTOM
        if (config.bottom == 0) {
            p.lineTo(offX, offY + baseH)
        } else {
            val dir = if (config.bottom == 1) 1f else -1f
            horizontalBump(p, offX + baseW, offY + baseH, -baseW, bump, dir)
        }

        // LEFT
        if (config.left == 0) {
            p.close()
        } else {
            val dir = if (config.left == 1) -1f else 1f
            verticalBump(p, offX, offY + baseH, -baseH, bump, dir)
            p.close()
        }
        return p
    }

    private fun horizontalBump(path: AndroidPath, startX: Float, startY: Float, length: Float, size: Float, dir: Float) {
        val portion = length / 3f
        val endX = startX + length
        path.lineTo(startX + portion, startY)
        path.cubicTo(
            startX + portion, startY + size * dir,
            startX + length - portion, startY + size * dir,
            startX + length - portion, startY
        )
        path.lineTo(endX, startY)
    }

    private fun verticalBump(path: AndroidPath, startX: Float, startY: Float, length: Float, size: Float, dir: Float) {
        val portion = length / 3f
        val endY = startY + length
        path.lineTo(startX, startY + portion)
        path.cubicTo(
            startX + size * dir, startY + portion,
            startX + size * dir, startY + length - portion,
            startX, startY + length - portion
        )
        path.lineTo(startX, endY)
    }
}