package com.example.educationalapp.puzzle

import android.graphics.Path as AndroidPath
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

data class PieceConfig(
    val top: Int = 0,    // 0=Flat, 1=Out, -1=In
    val right: Int = 0,
    val bottom: Int = 0,
    val left: Int = 0
)

class PuzzleShape(private val config: PieceConfig) : Shape {
    
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        
        // Matematica Fixă: Piesa e împărțită în 5 zone (Padding | Body | Padding)
        // Deoarece am adăugat padding 1/3 (33%) pe fiecare parte în ViewModel:
        // Lățimea totală = Padding + Body + Padding = 1/3 + 1/3 + 1/3
        // Deci Body = Width / 3. Dar hai să fim mai flexibili.
        
        // În ViewModel vom folosi padding = width / 3.
        // Asta înseamnă că Width Total = BaseWidth + 2 * (BaseWidth/3) = 5/3 BaseWidth.
        // Deci BaseWidth = TotalWidth * (3/5).
        
        val baseWidth = size.width * (3f / 5f)
        val baseHeight = size.height * (3f / 5f)

        val offsetX = size.width * (1f / 5f) // Padding stânga
        val offsetY = size.height * (1f / 5f) // Padding sus

        val bumpSize = baseWidth / 3.5f // Mărimea urechiușei

        // Pornim desenarea de la colțul "corpului" (după padding-ul transparent)
        path.moveTo(offsetX, offsetY)

        // --- SUS ---
        if (config.top == 0) {
            path.lineTo(offsetX + baseWidth, offsetY)
        } else {
            val dir = if (config.top == 1) -1f else 1f
            drawHorizontalBump(path, offsetX, offsetY, baseWidth, bumpSize, dir)
        }

        // --- DREAPTA ---
        if (config.right == 0) {
            path.lineTo(offsetX + baseWidth, offsetY + baseHeight)
        } else {
            val dir = if (config.right == 1) 1f else -1f
            drawVerticalBump(path, offsetX + baseWidth, offsetY, baseHeight, bumpSize, dir)
        }

        // --- JOS ---
        if (config.bottom == 0) {
            path.lineTo(offsetX, offsetY + baseHeight)
        } else {
            val dir = if (config.bottom == 1) 1f else -1f
            drawHorizontalBump(path, offsetX + baseWidth, offsetY + baseHeight, -baseWidth, bumpSize, dir)
        }

        // --- STÂNGA ---
        if (config.left == 0) {
            path.lineTo(offsetX, offsetY)
        } else {
            val dir = if (config.left == 1) -1f else 1f
            drawVerticalBump(path, offsetX, offsetY + baseHeight, -baseHeight, bumpSize, dir)
        }

        path.close()
        return Outline.Generic(path)
    }

    private fun drawHorizontalBump(path: Path, startX: Float, startY: Float, length: Float, size: Float, dir: Float) {
        val portion = length / 3f
        path.lineTo(startX + portion, startY)
        path.cubicTo(
            startX + portion, startY + size * dir,
            startX + length - portion, startY + size * dir,
            startX + length - portion, startY
        )
        path.lineTo(startX + length, startY)
    }

    private fun drawVerticalBump(path: Path, startX: Float, startY: Float, length: Float, size: Float, dir: Float) {
        val portion = length / 3f
        path.lineTo(startX, startY + portion)
        path.cubicTo(
            startX + size * dir, startY + portion,
            startX + size * dir, startY + length - portion,
            startX, startY + length - portion
        )
        path.lineTo(startX, startY + length)
    }

    companion object {
        /**
         * Android Path generator pentru a aplica mască direct în Bitmap (anti-aliased),
         * folosind aceeași matematică ca Shape-ul Compose.
         */
        fun createAndroidPath(config: PieceConfig, width: Float, height: Float): AndroidPath {
            val path = AndroidPath()

            val baseWidth = width * (3f / 5f)
            val baseHeight = height * (3f / 5f)
            val offsetX = width * (1f / 5f)
            val offsetY = height * (1f / 5f)
            val bumpSize = baseWidth / 3.5f

            path.moveTo(offsetX, offsetY)

            // --- SUS ---
            if (config.top == 0) {
                path.lineTo(offsetX + baseWidth, offsetY)
            } else {
                val dir = if (config.top == 1) -1f else 1f
                drawHorizontalBumpAndroid(path, offsetX, offsetY, baseWidth, bumpSize, dir)
            }

            // --- DREAPTA ---
            if (config.right == 0) {
                path.lineTo(offsetX + baseWidth, offsetY + baseHeight)
            } else {
                val dir = if (config.right == 1) 1f else -1f
                drawVerticalBumpAndroid(path, offsetX + baseWidth, offsetY, baseHeight, bumpSize, dir)
            }

            // --- JOS ---
            if (config.bottom == 0) {
                path.lineTo(offsetX, offsetY + baseHeight)
            } else {
                val dir = if (config.bottom == 1) 1f else -1f
                drawHorizontalBumpAndroid(path, offsetX + baseWidth, offsetY + baseHeight, -baseWidth, bumpSize, dir)
            }

            // --- STÂNGA ---
            if (config.left == 0) {
                path.lineTo(offsetX, offsetY)
            } else {
                val dir = if (config.left == 1) -1f else 1f
                drawVerticalBumpAndroid(path, offsetX, offsetY + baseHeight, -baseHeight, bumpSize, dir)
            }

            path.close()
            return path
        }

        private fun drawHorizontalBumpAndroid(
            path: AndroidPath,
            startX: Float,
            startY: Float,
            length: Float,
            size: Float,
            dir: Float
        ) {
            val portion = length / 3f
            path.lineTo(startX + portion, startY)
            path.cubicTo(
                startX + portion, startY + size * dir,
                startX + length - portion, startY + size * dir,
                startX + length - portion, startY
            )
            path.lineTo(startX + length, startY)
        }

        private fun drawVerticalBumpAndroid(
            path: AndroidPath,
            startX: Float,
            startY: Float,
            length: Float,
            size: Float,
            dir: Float
        ) {
            val portion = length / 3f
            path.lineTo(startX, startY + portion)
            path.cubicTo(
                startX + size * dir, startY + portion,
                startX + size * dir, startY + length - portion,
                startX, startY + length - portion
            )
            path.lineTo(startX, startY + length)
        }
    }
}