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

    private val _uiState = MutableStateFlow(PuzzleUiState())
    val uiState: StateFlow<PuzzleUiState> = _uiState

    fun startGame(context: Context, boardWidth: Float, boardHeight: Float) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isComplete = false)
            val theme = PuzzleAssets.getRandomTheme()
            val pieces = cutBitmapUnified(context, theme.resId, 3, 3, boardWidth, boardHeight)
            
            _uiState.value = _uiState.value.copy(
                pieces = pieces,
                currentThemeResId = theme.resId,
                isLoading = false
            )
        }
    }

    fun onPieceDrag(pieceId: Int, dragAmountX: Float, dragAmountY: Float) {
        val currentList = _uiState.value.pieces.toMutableList()
        val index = currentList.indexOfFirst { it.id == pieceId }
        if (index != -1 && !currentList[index].isLocked) {
            val piece = currentList[index]
            currentList[index] = piece.copy(
                currentX = piece.currentX + dragAmountX,
                currentY = piece.currentY + dragAmountY
            )
            _uiState.value = _uiState.value.copy(pieces = currentList)
        }
    }

    fun onPieceDrop(pieceId: Int) {
        val currentList = _uiState.value.pieces.toMutableList()
        val index = currentList.indexOfFirst { it.id == pieceId }
        if (index == -1) return
        val piece = currentList[index]
        if (piece.isLocked) return

        val snapDistance = 80f 
        val dx = abs(piece.currentX - piece.targetX)
        val dy = abs(piece.currentY - piece.targetY)

        if (dx < snapDistance && dy < snapDistance) {
            currentList[index] = piece.copy(
                currentX = piece.targetX,
                currentY = piece.targetY,
                isLocked = true
            )
            checkVictory(currentList)
        }
        _uiState.value = _uiState.value.copy(pieces = currentList)
    }

    private fun checkVictory(pieces: List<PuzzlePiece>) {
        if (pieces.all { it.isLocked }) {
            _uiState.value = _uiState.value.copy(isComplete = true)
        }
    }
    
    // --- NOUA FUNCȚIE DE TĂIERE UNIFICATĂ ---
    private suspend fun cutBitmapUnified(
        context: Context, resId: Int, rows: Int, cols: Int, reqW: Float, reqH: Float
    ): List<PuzzlePiece> = withContext(Dispatchers.IO) {
        
        // 1. Încărcare
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, options)
        var sampleSize = 1
        while (options.outWidth / sampleSize > 1200) sampleSize *= 2
        val loadOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val loadedBitmap = BitmapFactory.decodeResource(context.resources, resId, loadOptions) ?: return@withContext emptyList()
        val scaledBitmap = Bitmap.createScaledBitmap(loadedBitmap, reqW.toInt(), reqH.toInt(), true)

        val pieces = mutableListOf<PuzzlePiece>()
        
        // Dimensiunea logică a unei piese
        val baseW = reqW / cols
        val baseH = reqH / rows
        
        // Padding standard (1/3 din mărime) - identic cu cel din PuzzleShape
        val paddingX = baseW / 3f
        val paddingY = baseH / 3f

        // Mărimea UNIFICATĂ a tuturor bitmap-urilor (Padding + Body + Padding)
        val fullPieceW = (baseW + 2 * paddingX).toInt()
        val fullPieceH = (baseH + 2 * paddingY).toInt()

        // Configurație muchii
        val verticalEdges = Array(rows) { IntArray(cols - 1) }
        val horizontalEdges = Array(rows - 1) { IntArray(cols) }
        for (r in 0 until rows) for (c in 0 until cols - 1) verticalEdges[r][c] = if (Math.random() > 0.5) 1 else -1
        for (r in 0 until rows - 1) for (c in 0 until cols) horizontalEdges[r][c] = if (Math.random() > 0.5) 1 else -1

        val trayStartX = reqW + 50f
        var idCounter = 0

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                // Config
                val top = if (row == 0) 0 else -horizontalEdges[row - 1][col]
                val bottom = if (row == rows - 1) 0 else horizontalEdges[row][col]
                val left = if (col == 0) 0 else -verticalEdges[row][col - 1]
                val right = if (col == cols - 1) 0 else verticalEdges[row][col]
                val config = PieceConfig(top, right, bottom, left)

                // 2. Creăm un Bitmap gol, transparent, de mărime FIXĂ
                val pieceBitmap = Bitmap.createBitmap(fullPieceW, fullPieceH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(pieceBitmap)
                
                // 3. Calculăm ce zonă din imaginea originală copiem
                // Coordonata logică
                val logicalX = col * baseW
                val logicalY = row * baseH
                
                // Zona de citire (Source Rect)
                val srcLeft = (logicalX - paddingX).toInt()
                val srcTop = (logicalY - paddingY).toInt()
                val srcRight = (logicalX + baseW + paddingX).toInt()
                val srcBottom = (logicalY + baseH + paddingY).toInt()
                
                // Zona de scriere (Dest Rect) - Default e tot bitmap-ul piesei
                var dstLeft = 0
                var dstTop = 0
                var dstRight = fullPieceW
                var dstBottom = fullPieceH

                // Ajustăm dacă ieșim din imagine (la margini)
                // Exemplu: Dacă suntem la col=0, srcLeft e negativ. 
                // Trebuie să tăiem din Destinație partea stângă (să rămână transparentă)
                
                var finalSrcLeft = srcLeft
                var finalSrcTop = srcTop
                var finalSrcRight = srcRight
                var finalSrcBottom = srcBottom

                if (srcLeft < 0) {
                    dstLeft = -srcLeft // Mutăm destinația mai la dreapta
                    finalSrcLeft = 0
                }
                if (srcTop < 0) {
                    dstTop = -srcTop // Mutăm destinația mai jos
                    finalSrcTop = 0
                }
                if (srcRight > scaledBitmap.width) {
                    dstRight -= (srcRight - scaledBitmap.width) // Micșorăm destinația din dreapta
                    finalSrcRight = scaledBitmap.width
                }
                if (srcBottom > scaledBitmap.height) {
                    dstBottom -= (srcBottom - scaledBitmap.height)
                    finalSrcBottom = scaledBitmap.height
                }

                // Desenăm bucata de imagine în bitmap-ul piesei
                val srcRect = Rect(finalSrcLeft, finalSrcTop, finalSrcRight, finalSrcBottom)
                val dstRect = Rect(dstLeft, dstTop, dstRight, dstBottom)
                
                canvas.drawBitmap(scaledBitmap, srcRect, dstRect, null)

                // 4. Calculăm Target-ul pe ecran
                // Deoarece bitmap-ul include padding în stânga/sus, target-ul trebuie să fie
                // decalat cu acel padding față de grid-ul logic.
                val targetX = logicalX - paddingX
                val targetY = logicalY - paddingY

                val startX = trayStartX + (Math.random() * 50).toFloat()
                val startY = (Math.random() * (reqH - baseH)).toFloat()

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
                        config = config
                    )
                )
            }
        }
        return@withContext pieces.shuffled()
    }
}