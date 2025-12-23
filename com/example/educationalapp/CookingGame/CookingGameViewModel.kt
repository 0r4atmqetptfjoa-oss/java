package com.example.educationalapp.features.games

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.educationalapp.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class CookingStage {
    ROLLING,    // Întindem aluatul
    SAUCE,      // Punem sosul
    TOPPING,    // Punem ingrediente
    BAKING,     // Coacem
    EATING      // Mâncăm (Final)
}

data class PlacedTopping(
    val id: String = UUID.randomUUID().toString(),
    val imageRes: Int,
    val positionFromCenter: Offset, // Poziție relativă față de centrul pizzei (px)
    val scale: Float = 1f,
    val rotation: Float = 0f
)

data class IngredientOption(
    val id: String,
    val imageRes: Int
)

data class RecipeRequirement(
    val ingredientId: String,
    val imageRes: Int,
    val requiredCount: Int
)

data class BiteMark(
    val offsetFromCenter: Offset, // px
    val radiusPx: Float
)

data class CookingUiState(
    val stage: CookingStage = CookingStage.ROLLING,

    // Rolling/Sauce "mini-game" progress (0..1)
    val rollProgress: Float = 0f,
    val sauceProgress: Float = 0f,

    // Toppings
    val recipe: List<RecipeRequirement> = emptyList(),
    val placedToppings: List<PlacedTopping> = emptyList(),
    val isRecipeComplete: Boolean = false,

    // Baking
    val bakeProgress: Float = 0f,

    // Eating
    val bitesTaken: Int = 0,
    val biteMarks: List<BiteMark> = emptyList(),
    val isPizzaFinished: Boolean = false,

    // Lightweight FX triggers
    val lastDropFxId: Long = 0L,
    val lastDropFxPosFromCenter: Offset = Offset.Zero
)

@HiltViewModel
class CookingGameViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CookingUiState())
    val uiState: StateFlow<CookingUiState> = _uiState

    // LISTA DE INGREDIENTE DISPONIBILE
    val availableIngredients = listOf(
        IngredientOption("salami", R.drawable.top_salami),
        IngredientOption("mushroom", R.drawable.top_mushroom),
        IngredientOption("olive", R.drawable.top_olive),
        IngredientOption("pepper", R.drawable.top_pepper_green),
        IngredientOption("onion", R.drawable.top_onion),
        IngredientOption("bacon", R.drawable.top_bacon),
        IngredientOption("cheese", R.drawable.top_cheese),
        IngredientOption("basil", R.drawable.top_basil),
        IngredientOption("tomato", R.drawable.top_tomato)
    )

    // Tuning
    private val rollTarget = 1.0f
    private val sauceTarget = 1.0f
    private val maxToppings = 60
    private val bitesToFinish = 6

    init {
        resetGame()
    }

    fun resetGame() {
        _uiState.value = CookingUiState(
            stage = CookingStage.ROLLING,
            rollProgress = 0f,
            sauceProgress = 0f,
            recipe = randomRecipe(),
            placedToppings = emptyList(),
            isRecipeComplete = false,
            bakeProgress = 0f,
            bitesTaken = 0,
            biteMarks = emptyList(),
            isPizzaFinished = false,
            lastDropFxId = 0L,
            lastDropFxPosFromCenter = Offset.Zero
        )
    }

    // ===== ROLLING =====
    fun addRollingProgress(delta: Float) {
        val s = _uiState.value
        if (s.stage != CookingStage.ROLLING) return
        val next = min(rollTarget, max(0f, s.rollProgress + delta))
        val reached = next >= rollTarget
        _uiState.value = s.copy(
            rollProgress = next,
            stage = if (reached) CookingStage.SAUCE else s.stage
        )
    }

    // ===== SAUCE =====
    fun addSauceProgress(delta: Float) {
        val s = _uiState.value
        if (s.stage != CookingStage.SAUCE) return
        val next = min(sauceTarget, max(0f, s.sauceProgress + delta))
        val reached = next >= sauceTarget
        _uiState.value = s.copy(
            sauceProgress = next,
            stage = if (reached) CookingStage.TOPPING else s.stage
        )
    }

    // ===== TOPPING =====
    fun addTopping(imageRes: Int, positionFromCenter: Offset) {
        val s = _uiState.value
        if (s.stage != CookingStage.TOPPING) return
        if (s.placedToppings.size >= maxToppings) return

        val topping = PlacedTopping(
            imageRes = imageRes,
            positionFromCenter = positionFromCenter,
            rotation = Random.nextInt(0, 360).toFloat(),
            scale = 0.8f + Random.nextFloat() * 0.45f
        )

        val newList = s.placedToppings + topping
        val complete = computeRecipeComplete(s.recipe, newList)

        _uiState.value = s.copy(
            placedToppings = newList,
            isRecipeComplete = complete,
            lastDropFxId = System.currentTimeMillis(),
            lastDropFxPosFromCenter = positionFromCenter
        )
    }

    fun startBaking() {
        val s = _uiState.value
        if (s.stage != CookingStage.TOPPING) return
        if (!s.isRecipeComplete) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(stage = CookingStage.BAKING, bakeProgress = 0f)

            val totalSteps = 36
            for (i in 1..totalSteps) {
                delay(90)
                _uiState.value = _uiState.value.copy(bakeProgress = i / totalSteps.toFloat())
            }

            // Enter eating + generate bite marks gradually on taps
            _uiState.value = _uiState.value.copy(stage = CookingStage.EATING)
        }
    }

    // ===== EATING =====
    fun takeBite(pizzaRadiusPx: Float) {
        val s = _uiState.value
        if (s.stage != CookingStage.EATING) return
        if (s.isPizzaFinished) return

        val currentBites = s.bitesTaken + 1
        val newMark = generateBiteMark(pizzaRadiusPx, seed = currentBites * 997)

        val newMarks = if (currentBites <= bitesToFinish) s.biteMarks + newMark else s.biteMarks
        _uiState.value = s.copy(bitesTaken = currentBites, biteMarks = newMarks)

        if (currentBites >= bitesToFinish) {
            viewModelScope.launch {
                delay(450)
                _uiState.value = _uiState.value.copy(isPizzaFinished = true)
            }
        }
    }

    // ===== Helpers =====
    private fun randomRecipe(): List<RecipeRequirement> {
        // Pick 3 distinct ingredients, child-friendly counts
        val pool = availableIngredients.shuffled()
        val pick = pool.take(3)
        val counts = listOf(2, 2, 1).shuffled() // small targets
        return pick.mapIndexed { idx, ing ->
            RecipeRequirement(
                ingredientId = ing.id,
                imageRes = ing.imageRes,
                requiredCount = counts[idx]
            )
        }
    }

    private fun computeRecipeComplete(recipe: List<RecipeRequirement>, toppings: List<PlacedTopping>): Boolean {
        if (recipe.isEmpty()) return true
        // count by imageRes (stable)
        val counts = toppings.groupingBy { it.imageRes }.eachCount()
        return recipe.all { req -> (counts[req.imageRes] ?: 0) >= req.requiredCount }
    }

    private fun generateBiteMark(pizzaRadiusPx: Float, seed: Int): BiteMark {
        // Place bites on the rim area (upper-right-ish) with some variance.
        val rnd = Random(seed)
        val angle = (rnd.nextFloat() * 0.8f + 0.15f) * (Math.PI.toFloat()) // 0.15π .. 0.95π
        val rim = pizzaRadiusPx * (0.72f + rnd.nextFloat() * 0.12f)
        val x = (kotlin.math.cos(angle.toDouble()) * rim).toFloat()
        val y = (kotlin.math.sin(angle.toDouble()) * rim).toFloat()
        val r = pizzaRadiusPx * (0.15f + rnd.nextFloat() * 0.03f)
        return BiteMark(offsetFromCenter = Offset(x, -y), radiusPx = r) // negative y => upward bias
    }
}
