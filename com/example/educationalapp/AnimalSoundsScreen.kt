package com.example.educationalapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlin.random.Random

@Composable
fun AnimalSoundsScreen(navController: NavController, starState: MutableState<Int>) {
    val animals = remember {
        listOf(
            Animal("Câine", "🐶"),
            Animal("Pisică", "🐱"),
            Animal("Vacă", "🐮"),
            Animal("Cal", "🐴"),
            Animal("Leu", "🦁"),
            Animal("Broască", "🐸")
        )
    }
    var currentAnimal by remember { mutableStateOf(animals[0]) }
    var options by remember { mutableStateOf(listOf<Animal>()) }
    var feedback by remember { mutableStateOf("") }
    var score by remember { mutableStateOf(0) }

    fun newRound() {
        feedback = ""
        currentAnimal = animals.random()
        val set = mutableSetOf<Animal>()
        val optionList = mutableListOf<Animal>()
        val correctIndex = Random.nextInt(3)
        for (i in 0 until 3) {
            if (i == correctIndex) {
                optionList.add(currentAnimal)
            } else {
                var animal: Animal
                do {
                    animal = animals.random()
                } while (animal == currentAnimal || animal in set)
                set.add(animal)
                optionList.add(animal)
            }
        }
        options = optionList
    }

    LaunchedEffect(Unit) { newRound() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Sunete Animale", modifier = Modifier.padding(bottom = 16.dp))
        Text(text = "Ce animal este acesta? ${currentAnimal.emoji}", modifier = Modifier.padding(bottom = 16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                Button(onClick = {
                    if (option == currentAnimal) {
                        feedback = "Corect!";
                        score += 10; starState.value += 1
                    } else {
                        feedback = "Greșit!";
                        score = (score - 5).coerceAtLeast(0)
                    }
                    newRound()
                }, modifier = Modifier.weight(1f)) {
                    Text(text = option.name)
                }
            }
        }
        Text(text = "Scor: $score", modifier = Modifier.padding(top = 16.dp))
        Text(text = feedback, modifier = Modifier.padding(top = 8.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate(Screen.MainMenu.route) }) {
            Text(text = "Înapoi la Meniu")
        }
    }
}
