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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.abs
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
    val config: PieceConfig
)

data class PuzzleUiState(
    val pieces: List<PuzzlePiece> = emptyList(),
    val currentThemeResId: Int = 0,
    val isLoading: Boolean = true,
    val isComplete: Boolean = false
)

@HiltViewModel
class PuzzleViewModel @Inject constructor() : ViewModel() {

    companion object {
        // Cerință: jocul rulează strict pe 4x4.
        private const val ROWS = 4
        private const val COLS = 4

        // „Magnet” (ajutor) - folosit doar în apropierea poziției corecte.
        private const val MAGNET_STRENGTH = 0.22f
    }

    private val _uiState = MutableStateFlow(PuzzleUiState())
    val uiState: StateFlow<PuzzleUiState> = _uiState

    // Used to keep pieces inside reasonable bounds (so they cannot be "lost" off-screen).
    private var boardW: Float = 0f
    private var boardH: Float = 0f

    fun startGame(context: Context, boardWidth: Float, boardHeight: Float) {
        boardW = boardWidth
        boardH = boardHeight

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isComplete = false)

            val theme = PuzzleAssets.getRandomTheme()
            val pieces = cutBitmapPremium(
                context = context,
                resId = theme.resId,
                rows = ROWS,
                cols = COLS,
                boardWidth = boardWidth,
                boardHeight = boardHeight
            )

            _uiState.value = PuzzleUiState(
                pieces = pieces,
                currentThemeResId = theme.resId,
                isLoading = false,
                isComplete = false
            )
        }
    }

    /**
     * Reorders the list so that the dragged piece is rendered last (on top).
     */
    fun bringToFront(pieceId: Int) {
        val list = _uiState.value.pieces
        val idx = list.indexOfFirst { it.id == pieceId }
        if (idx <= -1) return
        val piece = list[idx]
        if (piece.isLocked) return

        val newList = list.toMutableList().apply {
            removeAt(idx)
            add(piece)
        }
        _uiState.value = _uiState.value.copy(pieces = newList)
    }

    fun onPieceDrag(pieceId: Int, dragAmountX: Float, dragAmountY: Float) {
        val currentList = _uiState.value.pieces.toMutableList()
        val index = currentList.indexOfFirst { it.id == pieceId }
        if (index == -1) return

        val piece = currentList[index]
        if (piece.isLocked) return

        var nextX = piece.currentX + dragAmountX
        var nextY = piece.currentY + dragAmountY

        // Magnet assist: dacă ești aproape de target, trage ușor spre poziția corectă.
        val magnetDistance = (min(piece.width, piece.height) * 0.28f).coerceIn(38f, 180f)
        val dxToTarget = piece.targetX - nextX
        val dyToTarget = piece.targetY - nextY
        if (abs(dxToTarget) < magnetDistance && abs(dyToTarget) < magnetDistance) {
            nextX += dxToTarget * MAGNET_STRENGTH
            nextY += dyToTarget * MAGNET_STRENGTH
        }

        // Keep pieces inside a forgiving rectangle around the board (still allows moving to the tray).
        val minX = -piece.width * 0.65f
        val maxX = boardW + piece.width * 1.85f
        val minY = -piece.height * 0.70f
        val maxY = boardH + piece.height * 0.75f

        currentList[index] = piece.copy(
            currentX = nextX.coerceIn(minX, maxX),
            currentY = nextY.coerceIn(minY, maxY)
        )

        _uiState.value = _uiState.value.copy(pieces = currentList)
    }

    fun onPieceDrop(pieceId: Int) {
        val currentList = _uiState.value.pieces.toMutableList()
        val index = currentList.indexOfFirst { it.id == pieceId }
        if (index == -1) return

        val piece = currentList[index]
        if (piece.isLocked) return

        // Adaptive snap distance: scales with piece size + clamps to sane limits.
        val snapDistance = (min(piece.width, piece.height) * 0.16f).coerceIn(24f, 96f)

        val dx = abs(piece.currentX - piece.targetX)
        val dy = abs(piece.currentY - piece.targetY)

        if (dx < snapDistance && dy < snapDistance) {
            currentList[index] = piece.copy(
                currentX = piece.targetX,
                currentY = piece.targetY,
                isLocked = true
            )
            _uiState.value = _uiState.value.copy(pieces = currentList)
            checkVictory()
        } else {
            _uiState.value = _uiState.value.copy(pieces = currentList)
        }
    }

    private fun checkVictory() {
        val completed = _uiState.value.pieces.all { it.isLocked }
        if (completed) {
            _uiState.value = _uiState.value.copy(isComplete = true)
        }
    }

    /**
     * Premium cutter:
     * - 4x4 fixed
     * - Bitmap mask (anti-aliased) aplicat direct pe piesă
     * - Contur subtil (stroke) pentru look "puzzle real"
     */
    private suspend fun cutBitmapPremium(
        context: Context,
        resId: Int,
        rows: Int,
        cols: Int,
        boardWidth: Float,
        boardHeight: Float
    ): List<PuzzlePiece> = withContext(Dispatchers.Default) {

        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val loadedBitmap = BitmapFactory.decodeResource(context.resources, resId, options)
            ?: throw IllegalStateException("Failed to decode puzzle image resId=$resId")

        // Scale the image to match the board size (in px).
        val scaledBitmap = Bitmap.createScaledBitmap(
            loadedBitmap,
            boardWidth.toInt().coerceAtLeast(1),
            boardHeight.toInt().coerceAtLeast(1),
            true
        )

        // Free original bitmap to reduce peak memory.
        if (loadedBitmap != scaledBitmap && !loadedBitmap.isRecycled) {
            loadedBitmap.recycle()
        }

        val reqW = scaledBitmap.width
        val reqH = scaledBitmap.height

        val baseW = reqW / cols
        val baseH = reqH / rows

        // Padding is 1/3 of the base size. Matches PuzzleShape math (base = 3/5 of full).
        val paddingX = baseW / 3
        val paddingY = baseH / 3

        val fullPieceW = baseW + 2 * paddingX
        val fullPieceH = baseH + 2 * paddingY

        // Generate edge configs (matching knobs/holes between adjacent pieces).
        val pieceConfigs = Array(rows) { Array(cols) { PieceConfig(0, 0, 0, 0) } }
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val top = if (r == 0) 0 else -pieceConfigs[r - 1][c].bottom
                val left = if (c == 0) 0 else -pieceConfigs[r][c - 1].right
                val bottom = if (r == rows - 1) 0 else if (Math.random() < 0.5) 1 else -1
                val right = if (c == cols - 1) 0 else if (Math.random() < 0.5) 1 else -1
                pieceConfigs[r][c] = PieceConfig(top, right, bottom, left)
            }
        }

        // Paint for drawing the image region (filtered, anti-aliased).
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Mask + outline paints.
        val maskFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = 0xFFFFFFFF.toInt()
        }
        val maskXferPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }

        val outlineStroke = (min(fullPieceW, fullPieceH) * 0.012f).coerceIn(1.5f, 4.0f)
        val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = outlineStroke
            color = 0xAAFFFFFF.toInt()
        }
        val outlineShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = outlineStroke + 0.8f
            color = 0x55000000
        }

        val pieces = mutableListOf<PuzzlePiece>()
        var idCounter = 0

        // Tray area: start to the right of the board.
        val trayStartX = reqW + 55f
        val maxStartY = (reqH - fullPieceH).toFloat().coerceAtLeast(0f)

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x = c * baseW - paddingX
                val y = r * baseH - paddingY

                val srcRect = Rect(
                    x.coerceAtLeast(0),
                    y.coerceAtLeast(0),
                    (x + fullPieceW).coerceAtMost(reqW),
                    (y + fullPieceH).coerceAtMost(reqH)
                )

                val pieceBitmap = Bitmap.createBitmap(fullPieceW, fullPieceH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(pieceBitmap)

                val dstRect = Rect(
                    srcRect.left - x,
                    srcRect.top - y,
                    srcRect.left - x + srcRect.width(),
                    srcRect.top - y + srcRect.height()
                )

                // 1) Draw the image region.
                canvas.drawBitmap(scaledBitmap, srcRect, dstRect, imagePaint)

                // 2) Apply mask in bitmap (anti-aliased shape).
                val config = pieceConfigs[r][c]
                val maskPath = PuzzleShape.createAndroidPath(config, fullPieceW.toFloat(), fullPieceH.toFloat())

                val maskBitmap = Bitmap.createBitmap(fullPieceW, fullPieceH, Bitmap.Config.ARGB_8888)
                Canvas(maskBitmap).drawPath(maskPath, maskFillPaint)
                canvas.drawBitmap(maskBitmap, 0f, 0f, maskXferPaint)
                maskBitmap.recycle()

                // Clear Xfermode to avoid side effects.
                maskXferPaint.xfermode = null

                // 3) Outline for a more "real" puzzle look.
                canvas.drawPath(maskPath, outlineShadowPaint)
                canvas.drawPath(maskPath, outlinePaint)

                // Restore xfermode for next piece.
                maskXferPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)

                // Target (top-left of the full bitmap) is logical position minus padding.
                val logicalX = c * baseW
                val logicalY = r * baseH

                val randomOffsetX = (Math.random() * 220 - 110).toFloat()
                val randomOffsetY = (Math.random() * 220 - 110).toFloat()

                val startX = trayStartX + randomOffsetX
                val startY = (Math.random().toFloat() * maxStartY) + randomOffsetY

                pieces.add(
                    PuzzlePiece(
                        id = idCounter++,
                        bitmap = pieceBitmap.asImageBitmap(),
                        currentX = startX,
                        currentY = startY,
                        targetX = (logicalX - paddingX).toFloat(),
                        targetY = (logicalY - paddingY).toFloat(),
                        isLocked = false,
                        width = fullPieceW,
                        height = fullPieceH,
                        config = config
                    )
                )
            }
        }

        if (!scaledBitmap.isRecycled) {
            scaledBitmap.recycle()
        }

        pieces.shuffled()
    }
}
