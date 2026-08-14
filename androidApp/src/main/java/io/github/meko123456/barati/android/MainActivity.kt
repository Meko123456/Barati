package io.github.meko123456.barati.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
                var studyDeck by remember { mutableStateOf<String?>(null) }
                var editDeck by remember { mutableStateOf<String?>(null) }

                BackHandler(enabled = studyDeck != null || editDeck != null) {
                    studyDeck = null
                    editDeck = null
                }

                when {
                    studyDeck != null ->
                        StudyScreen(viewModel = vm, deckId = studyDeck!!, onBack = { studyDeck = null })
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
