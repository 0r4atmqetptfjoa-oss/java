package com.example.educationalapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/**
 * Represents a simple recipe with a name and an ordered list of ingredients.  In
 * this mini‑game the player must select ingredients in the correct sequence
 * to complete the recipe.  Completing a recipe awards points and a star.
 */
data class Recipe(val name: String, val ingredients: List<String>)

/**
 * A cooking game that challenges players to remember the order of ingredients
 * for various recipes.  The player is prompted to add each ingredient in
 * sequence; selecting the wrong ingredient resets the round.  Upon completing
 * a recipe correctly the player earns points and a star and a new recipe
 * begins.
 */
@Composable
fun CookingGameScreen(navController: NavController, starState: MutableState<Int>) {
    val recipes = listOf(
        Recipe("Salată", listOf("Roșie", "Castravete", "Morcov")),
        Recipe("Sandviș", listOf("Pâine", "Șuncă", "Brânză")),
        Recipe("Pizza", listOf("Aluat", "Sos", "Cașcaval"))
    )
    // Flattened list of all ingredients for distractor generation
    val allIngredients = recipes.flatMap { it.ingredients }.distinct()
    var currentRecipe by remember { mutableStateOf(recipes[0]) }
    var stepIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var feedback by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(listOf<String>()) }

    fun updateOptions() {
        val correct = currentRecipe.ingredients[stepIndex]
        val decoys = mutableSetOf<String>()
        while (decoys.size < 2) {
            val candidate = allIngredients.random()
            if (candidate != correct) decoys.add(candidate)
        }
        options = (decoys + correct).shuffled()
    }

    fun newRecipe() {
        currentRecipe = recipes.random()
        stepIndex = 0
        feedback = ""
        updateOptions()
    }

    LaunchedEffect(Unit) { newRecipe() }

    fun onIngredientSelected(choice: String) {
        val expected = currentRecipe.ingredients[stepIndex]
        if (choice == expected) {
            // Correct ingredient
            if (stepIndex + 1 == currentRecipe.ingredients.size) {
                // Recipe completed
                feedback = "Preparat complet!"
                score += 10
                starState.value += 1
                newRecipe()
            } else {
                stepIndex++
                feedback = "Corect!"
                updateOptions()
            }
        } else {
            // Wrong ingredient
            feedback = "Greșit! Se începe o nouă rețetă."
            score = (score - 5).coerceAtLeast(0)
            newRecipe()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Gătit", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Prepară: ${currentRecipe.name}", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Ingredientul ${stepIndex + 1} din ${currentRecipe.ingredients.size}", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        // Ingredient options
        options.forEach { option ->
            Button(
                onClick = { onIngredientSelected(option) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(text = option)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Scor: $score")
        Text(text = feedback)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate(Screen.MainMenu.route) }) {
            Text("Înapoi la Meniu")
        }
    }
}