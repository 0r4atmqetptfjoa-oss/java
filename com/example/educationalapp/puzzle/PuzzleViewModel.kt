package com.example.educationalapp.puzzle

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.sqrt
import kotlin.random.Random

data class PuzzlePieceState(
    val id: Int,
    val bitmap: Bitmap,
    val correctRow: Int,
    val correctCol: Int,
    val currentPos: Offset,
    val isLocked: Boolean = false
)

@HiltViewModel
class PuzzleViewModel @Inject constructor() : ViewModel() {

    var pieces = mutableStateListOf<PuzzlePieceState>()
        private set
    
    var isLoading by mutableStateOf(true)
    var isGameOver by mutableStateOf(false)

    private val rows = 2
    private val cols = 2

    fun loadGame(context: Context) {
        if (pieces.isNotEmpty()) return 

        viewModelScope.launch {
            isLoading = true
            
            val theme = PuzzleAssets.getRandomTheme()
            
            // --- FIX CRITIC: inScaled = false ---
            val newPieces = withContext(Dispatchers.IO) {
                val opts = BitmapFactory.Options().apply { inScaled = false }
                val fullBitmap = BitmapFactory.decodeResource(context.resources, theme.resId, opts)
                
                // Safety check
                if (fullBitmap == null) return@withContext emptyList<PuzzlePieceState>()

                splitBitmap(fullBitmap, rows, cols)
            }

            pieces.clear()
            pieces.addAll(newPieces)
            isLoading = false
            isGameOver = false
        }
    }

    private fun splitBitmap(source: Bitmap, rows: Int, cols: Int): List<PuzzlePieceState> {
        val chunkW = source.width / cols
        val chunkH = source.height / rows
        val list = mutableListOf<PuzzlePieceState>()
        var idCounter = 0

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val pieceBmp = Bitmap.createBitmap(source, c * chunkW, r * chunkH, chunkW, chunkH)
                
                // Imprastiere
                val randX = Random.nextFloat() * 400f + 50f
                val randY = Random.nextFloat() * 200f + 500f

                list.add(
                    PuzzlePieceState(
                        id = idCounter++,
                        bitmap = pieceBmp,
                        correctRow = r,
                        correctCol = c,
                        currentPos = Offset(randX, randY),
                        isLocked = false
                    )
                )
            }
        }
        return list.shuffled()
    }

    fun onPieceDrag(id: Int, delta: Offset) {
        val index = pieces.indexOfFirst { it.id == id }
        if (index != -1 && !pieces[index].isLocked) {
            val p = pieces[index]
            pieces[index] = p.copy(currentPos = p.currentPos + delta)
        }
    }

    fun onPieceRelease(id: Int, slotSizePx: Float, slotsStartPos: Offset) {
        val index = pieces.indexOfFirst { it.id == id }
        if (index == -1) return

        val p = pieces[index]
        
        val targetX = slotsStartPos.x + (p.correctCol * slotSizePx)
        val targetY = slotsStartPos.y + (p.correctRow * slotSizePx)
        
        // Distanta
        val dx = p.currentPos.x - targetX
        val dy = p.currentPos.y - targetY
        val dist = sqrt(dx*dx + dy*dy)

        // Snap distance 100px
        if (dist < 100f) {
            pieces[index] = p.copy(currentPos = Offset(targetX, targetY), isLocked = true)
            checkWin()
        }
    }

    private fun checkWin() {
        if (pieces.isNotEmpty() && pieces.all { it.isLocked }) {
            isGameOver = true
        }
    }
}