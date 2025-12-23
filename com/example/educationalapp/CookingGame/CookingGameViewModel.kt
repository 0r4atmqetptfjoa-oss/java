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
    val positionFromCenter: Offset, // Poziție relativă exactă
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val timestamp: Long = System.currentTimeMillis() // Pentru animația de "landing"
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
    val offsetFromCenter: Offset,
    val radiusPx: Float
)

data class CookingUiState(
    val stage: CookingStage = CookingStage.ROLLING,
    
    // Progres
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
    val isPizzaFinished: Boolean = false
)

@HiltViewModel
class CookingGameViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CookingUiState())
    val uiState: StateFlow<CookingUiState> = _uiState

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

    private val rollTarget = 1.0f
    private val sauceTarget = 1.0f
    private val maxToppings = 20 // Mai multe ingrediente pentru distracție
    private val bitesToFinish = 6

    init {
        resetGame()
    }

    fun resetGame() {
        _uiState.value = CookingUiState(
            stage = CookingStage.ROLLING,
            recipe = randomRecipe()
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
    fun addTopping(imageRes: Int, exactPosition: Offset) {
        val s = _uiState.value
        if (s.stage != CookingStage.TOPPING) return
        if (s.placedToppings.size >= maxToppings) return

        // Adăugăm ingredientul exact la poziția unde a dat drumul copilul,
        // dar adăugăm o mică variație random la rotație pentru naturalețe.
        val topping = PlacedTopping(
            imageRes = imageRes,
            positionFromCenter = exactPosition,
            rotation = Random.nextInt(0, 360).toFloat(),
            scale = 0.9f + Random.nextFloat() * 0.2f // Variație mică de mărime
        )

        val newList = s.placedToppings + topping
        val complete = computeRecipeComplete(s.recipe, newList)

        _uiState.value = s.copy(
            placedToppings = newList,
            isRecipeComplete = complete
        )
    }

    fun startBaking() {
        val s = _uiState.value
        if (s.stage != CookingStage.TOPPING) return
        // Poți coace chiar dacă rețeta nu e completă, e mai distractiv pentru copii
        
        viewModelScope.launch {
            _uiState.value = s.copy(stage = CookingStage.BAKING, bakeProgress = 0f)

            val totalSteps = 50
            for (i in 1..totalSteps) {
                delay(60)
                _uiState.value = _uiState.value.copy(bakeProgress = i / totalSteps.toFloat())
            }
            _uiState.value = _uiState.value.copy(stage = CookingStage.EATING)
        }
    }

    // ===== EATING =====
    fun takeBite(pizzaRadiusPx: Float, touchPos: Offset) {
        val s = _uiState.value
        if (s.stage != CookingStage.EATING) return
        if (s.isPizzaFinished) return

        val currentBites = s.bitesTaken + 1
        
        // Mușcătura apare exact unde a apăsat copilul (sau aproape)
        val biteRadius = pizzaRadiusPx * 0.25f
        val newMark = BiteMark(offsetFromCenter = touchPos, radiusPx = biteRadius)

        val newMarks = s.biteMarks + newMark
        _uiState.value = s.copy(bitesTaken = currentBites, biteMarks = newMarks)

        if (currentBites >= bitesToFinish) {
            viewModelScope.launch {
                delay(300)
                _uiState.value = _uiState.value.copy(isPizzaFinished = true)
            }
        }
    }

    // ===== Helpers =====
    private fun randomRecipe(): List<RecipeRequirement> {
        val pool = availableIngredients.shuffled()
        val pick = pool.take(3)
        // Numere mici pentru copii (1-2 bucăți)
        return pick.map { ing ->
            RecipeRequirement(ing.id, ing.imageRes, Random.nextInt(1, 3))
        }
    }

    private fun computeRecipeComplete(recipe: List<RecipeRequirement>, toppings: List<PlacedTopping>): Boolean {
        if (recipe.isEmpty()) return true
        val counts = toppings.groupingBy { it.imageRes }.eachCount()
        return recipe.all { req -> (counts[req.imageRes] ?: 0) >= req.requiredCount }
    }
}