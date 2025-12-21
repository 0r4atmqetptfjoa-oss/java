package com.example.educationalapp.shapes

import com.example.educationalapp.R

// Definim tipurile de forme posibile
enum class ShapeType {
    CIRCLE, SQUARE, TRIANGLE, RECTANGLE, STAR, HEART
}

data class ShapeItem(
    val id: String,
    val name: String,
    val type: ShapeType,
    val imageRes: Int // Imaginea cu obiectul real (ex: Pizza)
)

object ShapesAssets {

    val allItems = listOf(
        // --- CERC (Circle) ---
        ShapeItem("circle_donut", "Gogoașă", ShapeType.CIRCLE, R.drawable.shape_circle_donut),
        ShapeItem("circle_button", "Nasture", ShapeType.CIRCLE, R.drawable.shape_circle_button),
        ShapeItem("circle_clock", "Ceas", ShapeType.CIRCLE, R.drawable.shape_circle_clock),

        // --- PĂTRAT (Square) ---
        ShapeItem("square_gift", "Cadou", ShapeType.SQUARE, R.drawable.shape_square_gift),
        ShapeItem("square_dice", "Zar", ShapeType.SQUARE, R.drawable.shape_square_dice),
        ShapeItem("square_cracker", "Biscuit", ShapeType.SQUARE, R.drawable.shape_square_cracker),

        // --- TRIUNGHI (Triangle) ---
        ShapeItem("triangle_pizza", "Pizza", ShapeType.TRIANGLE, R.drawable.shape_triangle_pizza),
        ShapeItem("triangle_hat", "Coif", ShapeType.TRIANGLE, R.drawable.shape_triangle_hat),
        ShapeItem("triangle_inst", "Triunghi", ShapeType.TRIANGLE, R.drawable.shape_triangle_instrument),

        // --- DREPTUNGHI (Rectangle) ---
        ShapeItem("rect_phone", "Telefon", ShapeType.RECTANGLE, R.drawable.shape_rect_phone),
        ShapeItem("rect_book", "Carte", ShapeType.RECTANGLE, R.drawable.shape_rect_book),
        ShapeItem("rect_choc", "Ciocolată", ShapeType.RECTANGLE, R.drawable.shape_rect_chocolate),

        // --- STEA (Star) ---
        ShapeItem("star_fish", "Stea de Mare", ShapeType.STAR, R.drawable.shape_star_fish),
        ShapeItem("star_wand", "Baghetă", ShapeType.STAR, R.drawable.shape_star_wand),
        ShapeItem("star_ornament", "Ornament", ShapeType.STAR, R.drawable.shape_star_ornament),

        // --- INIMĂ (Heart) ---
        ShapeItem("heart_pillow", "Pernă", ShapeType.HEART, R.drawable.shape_heart_pillow),
        ShapeItem("heart_balloon", "Balon", ShapeType.HEART, R.drawable.shape_heart_balloon),
        ShapeItem("heart_box", "Cutie", ShapeType.HEART, R.drawable.shape_heart_box)
    )

    // Funcție pentru a lua un item random dintr-o anumită formă (pt răspunsul corect)
    fun getRandomItemForShape(type: ShapeType): ShapeItem {
        return allItems.filter { it.type == type }.random()
    }
    
    // Funcție pentru a lua un item care NU e de tipul dat (pt răspunsuri greșite)
    fun getRandomDistractor(excludeType: ShapeType): ShapeItem {
        return allItems.filter { it.type != excludeType }.random()
    }
}