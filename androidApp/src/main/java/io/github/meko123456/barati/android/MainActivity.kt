package io.github.meko123456.barati.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.meko123456.barati.android.ui.theme.BaratiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BaratiTheme {
                val vm: BaratiViewModel = viewModel()
                // Saveable, so turning the phone keeps the screen it was on. Plain remember sent a
                // study session or a deck being edited back to the deck list on every rotation.
                var studyDeck by rememberSaveable { mutableStateOf<String?>(null) }
                var editDeck by rememberSaveable { mutableStateOf<String?>(null) }
                val closeStudy = {
                    vm.endStudy()
                    studyDeck = null
                }

                BackHandler(enabled = studyDeck != null || editDeck != null) {
                    if (studyDeck != null) closeStudy()
                    editDeck = null
                }

                when {
                    studyDeck != null ->
                        StudyScreen(viewModel = vm, deckId = studyDeck!!, onBack = closeStudy)
                    editDeck != null ->
                        DeckEditScreen(viewModel = vm, deckId = editDeck!!, onBack = { editDeck = null })
                    else ->
                        DeckListScreen(
                            viewModel = vm,
                            onOpenDeck = { studyDeck = it },
                            onEditDeck = { editDeck = it },
                        )
                }
            }
        }
    }
}
