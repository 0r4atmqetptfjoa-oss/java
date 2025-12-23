package com.example.educationalapp.puzzle

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
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
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class PuzzlePiece(
    val id: Int,
    val bitmap: ImageBitmap,
    val currentX: Float,
    val currentY: Float,
    val targetX: Float,
    val targetY: Float,
    val homeX: Float,
    val homeY: Float,
    val isLocked: Boolean = false,
    val width: Int,
    val height: Int,
    val config: PieceConfig
)

data class PuzzleUiState(
    val pieces: List<PuzzlePiece> = emptyList(),
    val currentThemeResId: Int = 0,
    val isLoading: Boolean = true,
    val isComplete: Boolean = false,

    // Layout (în coordonate "board-space": originea este colțul stânga-sus al tablei)
    val boardWidth: Float = 0f,
    val boardHeight: Float = 0f,
    val trayStartX: Float = 0f,
    val trayWidth: Float = 0f,
    val trayHeight: Float = 0f
)

@HiltViewModel
class PuzzleViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(PuzzleUiState())
    val uiState: StateFlow<PuzzleUiState> = _uiState

    private var themeQueue: MutableList<PuzzleTheme> = mutableListOf()

    private fun nextTheme(): PuzzleTheme {
        if (themeQueue.isEmpty()) {
            themeQueue = PuzzleAssets.allThemes().shuffled().toMutableList()
        }
        return themeQueue.removeAt(0)
    }

    /**
     * Compat: pornește jocul cu layout implicit (tray la dreapta).
     * Dacă ai deja varianta nouă de UI, folosește overload-ul complet.
     */
    fun startGame(
        context: Context,
        boardWidth: Float,
        boardHeight: Float
    ) {
        val trayW = boardWidth * 0.28f
        startGame(
            context = context,
            boardWidth = boardWidth,
            boardHeight = boardHeight,
            trayStartX = boardWidth + 50f,
            trayWidth = trayW,
            trayHeight = boardHeight
        )
    }

    fun startGame(
        context: Context,
        boardWidth: Float,
        boardHeight: Float,
        trayStartX: Float,
        trayWidth: Float,
        trayHeight: Float
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                isComplete = false,
                boardWidth = boardWidth,
                boardHeight = boardHeight,
                trayStartX = trayStartX,
                trayWidth = trayWidth,
                trayHeight = trayHeight
            )

            val theme = nextTheme()
            val pieces = cutBitmapUnified(
                context = context,
                resId = theme.resId,
                rows = 4,
                cols = 4,
                boardW = boardWidth,
                boardH = boardHeight,
                trayStartX = trayStartX,
                trayW = trayWidth,
                trayH = trayHeight
            )

            _uiState.value = _uiState.value.copy(
                pieces = pieces,
                currentThemeResId = theme.resId,
                isLoading = false,
                isComplete = false
            )
        }
    }

    fun shuffleTray() {
        val st = _uiState.value
        if (st.pieces.isEmpty()) return
        val updated = st.pieces.map { p ->
            if (!p.isLocked) p.copy(currentX = p.homeX, currentY = p.homeY) else p
        }
        _uiState.value = st.copy(pieces = updated)
    }

    fun onPiecePickUp(pieceId: Int) {
        val st = _uiState.value
        val idx = st.pieces.indexOfFirst { it.id == pieceId }
        if (idx < 0) return
        val p = st.pieces[idx]
        if (p.isLocked) return

        // Mutăm piesa la final ca să fie desenată deasupra (zIndex simplu)
        val newList = st.pieces.toMutableList()
        newList.removeAt(idx)
        newList.add(p)
        _uiState.value = st.copy(pieces = newList)
    }

    fun onPieceDrag(pieceId: Int, dragDx: Float, dragDy: Float) {
        val st = _uiState.value
        val currentList = st.pieces.toMutableList()
        val index = currentList.indexOfFirst { it.id == pieceId }
        if (index == -1) return
        val piece = currentList[index]
        if (piece.isLocked) return

        var newX = piece.currentX + dragDx
        var newY = piece.currentY + dragDy

        // Clamp în interiorul scenei (board + tray)
        val minX = -piece.width * 0.25f
        val maxX = st.trayStartX + st.trayWidth - piece.width * 0.75f
        val minY = -piece.height * 0.25f
        val maxY = st.boardHeight - piece.height * 0.75f
        newX = newX.coerceIn(minX, maxX)
        newY = newY.coerceIn(minY, maxY)

        // Magnetizare fină când ești aproape de target (nu snap, doar "atracție")
        val magnet = min(piece.width, piece.height) * 0.40f
        val dx = piece.targetX - newX
        val dy = piece.targetY - newY
        if (abs(dx) < magnet && abs(dy) < magnet) {
            newX += dx * 0.08f
            newY += dy * 0.08f
        }

        currentList[index] = piece.copy(currentX = newX, currentY = newY)
        _uiState.value = st.copy(pieces = currentList)
    }

    fun onPieceDrop(pieceId: Int) {
        val st = _uiState.value
        val currentList = st.pieces.toMutableList()
        val index = currentList.indexOfFirst { it.id == pieceId }
        if (index == -1) return
        val piece = currentList[index]
        if (piece.isLocked) return

        val snap = min(piece.width, piece.height) * 0.22f
        val dx = abs(piece.currentX - piece.targetX)
        val dy = abs(piece.currentY - piece.targetY)

        if (dx < snap && dy < snap) {
            currentList[index] = piece.copy(
                currentX = piece.targetX,
                currentY = piece.targetY,
                isLocked = true
            )
        }

        val complete = currentList.all { it.isLocked }
        _uiState.value = st.copy(
            pieces = currentList,
            isComplete = complete
        )
    }

    private suspend fun cutBitmapUnified(
        context: Context,
        resId: Int,
        rows: Int,
        cols: Int,
        boardW: Float,
        boardH: Float,
        trayStartX: Float,
        trayW: Float,
        trayH: Float
    ): List<PuzzlePiece> = withContext(Dispatchers.IO) {

        // Încărcare mai light (limităm dimensiunea pentru memorie)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, options)

        var sampleSize = 1
        while (options.outWidth / sampleSize > 1600) sampleSize *= 2
        val loadOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }

        val loaded = BitmapFactory.decodeResource(context.resources, resId, loadOptions)
            ?: return@withContext emptyList()

        val scaled = Bitmap.createScaledBitmap(
            loaded,
            max(1, boardW.toInt()),
            max(1, boardH.toInt()),
            true
        )

        val baseW = boardW / cols
        val baseH = boardH / rows

        // padding 1/3 - compatibil cu PuzzleShape
        val paddingX = baseW / 3f
        val paddingY = baseH / 3f

        val fullPieceW = (baseW + 2 * paddingX).toInt()
        val fullPieceH = (baseH + 2 * paddingY).toInt()

        val piecesRaw = mutableListOf<PuzzlePiece>()

        // Edges random dar consistente
        val verticalEdges = Array(rows) { IntArray(cols - 1) }
        val horizontalEdges = Array(rows - 1) { IntArray(cols) }
        for (r in 0 until rows) for (c in 0 until cols - 1) verticalEdges[r][c] = if (Random.nextBoolean()) 1 else -1
        for (r in 0 until rows - 1) for (c in 0 until cols) horizontalEdges[r][c] = if (Random.nextBoolean()) 1 else -1

        var idCounter = 0

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

                val srcLeft = (logicalX - paddingX).toInt()
                val srcTop = (logicalY - paddingY).toInt()
                val srcRight = (logicalX + baseW + paddingX).toInt()
                val srcBottom = (logicalY + baseH + paddingY).toInt()

                var dstLeft = 0
                var dstTop = 0
                var dstRight = fullPieceW
                var dstBottom = fullPieceH

                var finalSrcLeft = srcLeft
                var finalSrcTop = srcTop
                var finalSrcRight = srcRight
                var finalSrcBottom = srcBottom

                if (srcLeft < 0) {
                    dstLeft = -srcLeft
                    finalSrcLeft = 0
                }
                if (srcTop < 0) {
                    dstTop = -srcTop
                    finalSrcTop = 0
                }
                if (srcRight > scaled.width) {
                    dstRight -= (srcRight - scaled.width)
                    finalSrcRight = scaled.width
                }
                if (srcBottom > scaled.height) {
                    dstBottom -= (srcBottom - scaled.height)
                    finalSrcBottom = scaled.height
                }

                val srcRect = android.graphics.Rect(finalSrcLeft, finalSrcTop, finalSrcRight, finalSrcBottom)
                val dstRect = android.graphics.Rect(dstLeft, dstTop, dstRight, dstBottom)
                canvas.drawBitmap(scaled, srcRect, dstRect, null)

                // target (în board-space) este colțul grid-ului minus padding
                val targetX = logicalX - paddingX
                val targetY = logicalY - paddingY

                // homeX/homeY setate mai jos (după shuffle)
                piecesRaw.add(
                    PuzzlePiece(
                        id = idCounter++,
                        bitmap = pieceBitmap.asImageBitmap(),
                        currentX = 0f,
                        currentY = 0f,
                        targetX = targetX,
                        targetY = targetY,
                        homeX = 0f,
                        homeY = 0f,
                        isLocked = false,
                        width = fullPieceW,
                        height = fullPieceH,
                        config = config
                    )
                )
            }
        }

        // Piese în tray: grid premium 2 coloane, 8 rânduri
        val trayCols = 2
        val trayRows = ((rows * cols) + trayCols - 1) / trayCols
        val cellW = trayW / trayCols
        val cellH = trayH / trayRows
        val innerPad = min(cellW, cellH) * 0.08f + 6f

        val shuffled = piecesRaw.shuffled()
        val placed = shuffled.mapIndexed { idx, p ->
            val c = idx % trayCols
            val r = idx / trayCols

            val centerX = trayStartX + innerPad + c * cellW + cellW / 2f
            val centerY = innerPad + r * cellH + cellH / 2f

            val homeX = centerX - p.width / 2f
            val homeY = centerY - p.height / 2f

            p.copy(
                currentX = homeX,
                currentY = homeY,
                homeX = homeX,
                homeY = homeY
            )
        }

        return@withContext placed
    }
}