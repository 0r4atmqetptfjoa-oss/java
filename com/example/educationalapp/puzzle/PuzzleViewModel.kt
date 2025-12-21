package com.example.educationalapp.puzzle

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
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
import kotlin.math.max
import kotlin.math.min

data class PuzzlePiece(
    val id: Int,
    val bitmap: ImageBitmap,
    val currentX: Float,
    val currentY: Float,
    val targetX: Float,
    val targetY: Float,
    val isLocked: Boolean = false,
    val width: Int,
    val height: Int,
    val config: PieceConfig,
    val z: Int = 0
)

sealed class PuzzleEvent {
    data class Snap(val pieceId: Int) : PuzzleEvent()
    object Completed : PuzzleEvent()
}

data class PuzzleUiState(
    val pieces: List<PuzzlePiece> = emptyList(),
    val currentThemeResId: Int = 0,
    val isLoading: Boolean = true,
    val isComplete: Boolean = false,
    val snapDistancePx: Float = 0f,
    val magnetDistancePx: Float = 0f,
    val event: PuzzleEvent? = null
)

@HiltViewModel
class PuzzleViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(PuzzleUiState())
    val uiState: StateFlow<PuzzleUiState> = _uiState.asStateFlow()

    // Board size in px (board box only, not the HUD area)
    private var boardW: Float = 0f
    private var boardH: Float = 0f

    private var zCounter: Int = 0

    fun consumeEvent() {
        _uiState.value = _uiState.value.copy(event = null)
    }

    fun startGame(context: Context, boardWidth: Float, boardHeight: Float) {
        boardW = boardWidth
        boardH = boardHeight

        val rows = 4
        val cols = 4

        // Adaptive snap/magnet distances based on cell size
        val cellW = boardWidth / cols.toFloat()
        val cellH = boardHeight / rows.toFloat()
        val snap = min(cellW, cellH) * 0.22f
        val magnet = snap * 2.2f

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                isComplete = false,
                event = null,
                snapDistancePx = snap,
                magnetDistancePx = magnet
            )

            val theme = PuzzleAssets.getRandomTheme()
            val pieces = cutBitmapUnified(
                context = context,
                resId = theme.resId,
                rows = rows,
                cols = cols,
                reqW = boardWidth,
                reqH = boardHeight
            )

            _uiState.value = _uiState.value.copy(
                pieces = pieces,
                currentThemeResId = theme.resId,
                isLoading = false
            )
        }
    }

    fun onPieceDragStart(pieceId: Int) {
        val currentList = _uiState.value.pieces.toMutableList()
        val idx = currentList.indexOfFirst { it.id == pieceId }
        if (idx == -1) return
        val piece = currentList[idx]
        if (piece.isLocked) return

        zCounter += 1
        currentList[idx] = piece.copy(z = zCounter)
        _uiState.value = _uiState.value.copy(pieces = currentList)
    }

    fun onPieceDrag(pieceId: Int, dragAmountX: Float, dragAmountY: Float) {
        val state = _uiState.value
        val currentList = state.pieces.toMutableList()
        val idx = currentList.indexOfFirst { it.id == pieceId }
        if (idx == -1) return
        val piece = currentList[idx]
        if (piece.isLocked) return

        // Apply movement
        var nx = piece.currentX + dragAmountX
        var ny = piece.currentY + dragAmountY

        // Soft clamp so pieces don't get lost
        val minX = -piece.width * 0.35f
        val maxX = boardW + piece.width * 0.85f
        val minY = -piece.height * 0.15f
        val maxY = boardH - piece.height * 0.10f
        nx = nx.coerceIn(minX, maxX)
        ny = ny.coerceIn(minY, maxY)

        // Magnet assist if close to target
        val magnetDist = state.magnetDistancePx
        if (magnetDist > 0f) {
            val dx = piece.targetX - nx
            val dy = piece.targetY - ny
            val d = hypot(dx, dy)
            if (d < magnetDist) {
                val t = ((magnetDist - d) / magnetDist).coerceIn(0f, 1f)
                val strength = 0.35f * t * t // up to 35% pull
                nx = nx + dx * strength
                ny = ny + dy * strength
            }
        }

        currentList[idx] = piece.copy(currentX = nx, currentY = ny)
        _uiState.value = state.copy(pieces = currentList)
    }

    fun onPieceDrop(pieceId: Int) {
        val state = _uiState.value
        val currentList = state.pieces.toMutableList()
        val idx = currentList.indexOfFirst { it.id == pieceId }
        if (idx == -1) return
        val piece = currentList[idx]
        if (piece.isLocked) return

        val snapDist = state.snapDistancePx
        val dx = piece.targetX - piece.currentX
        val dy = piece.targetY - piece.currentY
        val dist = hypot(dx, dy)

        if (snapDist > 0f && dist <= snapDist) {
            currentList[idx] = piece.copy(
                currentX = piece.targetX,
                currentY = piece.targetY,
                isLocked = true
            )

            var newState = state.copy(pieces = currentList, event = PuzzleEvent.Snap(pieceId))

            if (currentList.all { it.isLocked }) {
                newState = newState.copy(isComplete = true, event = PuzzleEvent.Completed)
            }

            _uiState.value = newState
        } else {
            _uiState.value = state.copy(pieces = currentList)
        }
    }

    // --- UNIFIED CUT + PREMIUM MASK (4x4) ---
    private suspend fun cutBitmapUnified(
        context: Context,
        resId: Int,
        rows: Int,
        cols: Int,
        reqW: Float,
        reqH: Float
    ): List<PuzzlePiece> = withContext(Dispatchers.IO) {

        // 1) Load (downsample for memory)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, bounds)
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > 1400) sampleSize *= 2

        val loadOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val loadedBitmap = BitmapFactory.decodeResource(context.resources, resId, loadOptions)
            ?: return@withContext emptyList()

        val scaledBitmap = Bitmap.createScaledBitmap(
            loadedBitmap,
            reqW.toInt().coerceAtLeast(1),
            reqH.toInt().coerceAtLeast(1),
            true
        )
        if (loadedBitmap !== scaledBitmap) {
            loadedBitmap.recycle()
        }

        val pieces = mutableListOf<PuzzlePiece>()

        val baseW = reqW / cols
        val baseH = reqH / rows

        val paddingX = baseW / 3f
        val paddingY = baseH / 3f

        val fullPieceW = (baseW + 2f * paddingX).toInt().coerceAtLeast(1)
        val fullPieceH = (baseH + 2f * paddingY).toInt().coerceAtLeast(1)

        // Random edges
        val horizontalEdges = Array(rows - 1) { IntArray(cols) }
        val verticalEdges = Array(rows) { IntArray(cols - 1) }

        for (r in 0 until rows - 1) for (c in 0 until cols) horizontalEdges[r][c] = if (Math.random() > 0.5) 1 else -1
        for (r in 0 until rows) for (c in 0 until cols - 1) verticalEdges[r][c] = if (Math.random() > 0.5) 1 else -1

        // Place initial pieces slightly to the right of the board (within same coordinate system)
        val trayStartX = reqW + max(24f, reqW * 0.08f)

        // Paint for bitmap draw
        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
        }

        // Mask paint (DST_IN)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }

        // Stroke paint (border)
        val strokeW = max(2f, min(fullPieceW, fullPieceH) * 0.012f)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            color = 0x66FFFFFF
        }

        var idCounter = 0
        zCounter = 0

        for (row in 0 until rows) {
            for (col in 0 until cols) {

                val top = if (row == 0) 0 else -horizontalEdges[row - 1][col]
                val bottom = if (row == rows - 1) 0 else horizontalEdges[row][col]
                val left = if (col == 0) 0 else -verticalEdges[row][col - 1]
                val right = if (col == cols - 1) 0 else verticalEdges[row][col]
                val config = PieceConfig(top, right, bottom, left)

                val pieceBitmap = Bitmap.createBitmap(fullPieceW, fullPieceH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(pieceBitmap)

                val logicalX = col * baseW
                val logicalY = row * baseH

                // source rect in scaled image
                val srcLeft = (logicalX - paddingX).toInt()
                val srcTop = (logicalY - paddingY).toInt()
                val srcRight = (logicalX + baseW + paddingX).toInt()
                val srcBottom = (logicalY + baseH + paddingY).toInt()

                var finalSrcLeft = srcLeft
                var finalSrcTop = srcTop
                var finalSrcRight = srcRight
                var finalSrcBottom = srcBottom

                var dstLeft = 0
                var dstTop = 0
                var dstRight = fullPieceW
                var dstBottom = fullPieceH

                if (srcLeft < 0) {
                    dstLeft += -srcLeft
                    finalSrcLeft = 0
                }
                if (srcTop < 0) {
                    dstTop += -srcTop
                    finalSrcTop = 0
                }
                if (srcRight > scaledBitmap.width) {
                    dstRight -= (srcRight - scaledBitmap.width)
                    finalSrcRight = scaledBitmap.width
                }
                if (srcBottom > scaledBitmap.height) {
                    dstBottom -= (srcBottom - scaledBitmap.height)
                    finalSrcBottom = scaledBitmap.height
                }

                val srcRect = Rect(finalSrcLeft, finalSrcTop, finalSrcRight, finalSrcBottom)
                val dstRect = Rect(dstLeft, dstTop, dstRight, dstBottom)

                canvas.drawBitmap(scaledBitmap, srcRect, dstRect, bitmapPaint)

                // Premium: apply mask directly in bitmap + outline
                val maskPath = PuzzleShape.createAndroidPath(config, fullPieceW.toFloat(), fullPieceH.toFloat())
                canvas.drawPath(maskPath, maskPaint)
                maskPaint.xfermode = null
                canvas.drawPath(maskPath, strokePaint)
                maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)

                // Target in board coords (includes padding offset)
                val targetX = logicalX - paddingX
                val targetY = logicalY - paddingY

                val startX = trayStartX + (Math.random().toFloat() * max(20f, reqW * 0.08f))
                val startY = (Math.random().toFloat() * max(1f, reqH - fullPieceH.toFloat()))

                zCounter += 1
                pieces.add(
                    PuzzlePiece(
                        id = idCounter++,
                        bitmap = pieceBitmap.asImageBitmap(),
                        currentX = startX,
                        currentY = startY,
                        targetX = targetX,
                        targetY = targetY,
                        isLocked = false,
                        width = fullPieceW,
                        height = fullPieceH,
                        config = config,
                        z = zCounter
                    )
                )
            }
        }

        scaledBitmap.recycle()
        return@withContext pieces.shuffled()
    }
}
