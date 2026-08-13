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

                BackHandler(enabled = studyDeck != null) { studyDeck = null }

                val deck = studyDeck
                if (deck == null) {
                    DeckListScreen(viewModel = vm, onOpenDeck = { studyDeck = it })
                } else {
                    StudyScreen(viewModel = vm, deckId = deck, onBack = { studyDeck = null })
                }
            }
        }
    }
}
