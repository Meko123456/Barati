package io.github.meko123456.barati.android

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.meko123456.barati.android.ui.theme.BaratiTheme
import io.github.meko123456.barati.shared.domain.CardText

class MainActivity : ComponentActivity() {
    /** Text from another app for a new card, until the card dialog takes it. */
    private var incoming by mutableStateOf<Incoming?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // A fresh start only: recreated after a turn, the activity is handed the same intent again,
        // and the dialog already holds whatever was typed since.
        if (savedInstanceState == null) take(intent, openedForIt = true)
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

                // A card for text from another app. When Barati was opened just for it, adding or
                // cancelling goes back to that app, where the reading was.
                var cardFront by rememberSaveable { mutableStateOf<String?>(null) }
                var openings by rememberSaveable { mutableIntStateOf(0) }
                var backAfterCard by rememberSaveable { mutableStateOf(false) }
                val arriving = incoming
                LaunchedEffect(arriving) {
                    if (arriving != null) {
                        cardFront = CardText.fromSelection(arriving.text)
                        openings++
                        backAfterCard = arriving.openedForIt
                        incoming = null
                    }
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

                cardFront?.let { front ->
                    val decks = remember(vm.version) { vm.deckSummaries().map { it.first } }
                    key(openings) {
                        CardFromTextDialog(
                            front = front,
                            decks = decks,
                            initialDeckId = editDeck ?: studyDeck ?: vm.lastDeckForText,
                            onAdd = { deckId, newDeckName, text, back ->
                                val target = deckId ?: vm.createDeck(newDeckName)
                                vm.addCard(target, text, back)
                                vm.lastDeckForText = target
                                cardFront = null
                                if (backAfterCard) {
                                    val name = vm.deck(target)?.name.orEmpty()
                                    Toast.makeText(this@MainActivity, "Added to “$name”", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                            },
                            onDismiss = {
                                cardFront = null
                                if (backAfterCard) finish()
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        take(intent, openedForIt = false)
    }

    private fun take(intent: Intent?, openedForIt: Boolean) {
        val text = when (intent?.action) {
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            Intent.ACTION_SEND -> intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
            else -> null
        }?.toString()?.takeIf { it.isNotBlank() } ?: return
        incoming = Incoming(text, openedForIt)
    }

    /** Text handed over by another app, and whether this activity was started just for it. */
    private data class Incoming(val text: String, val openedForIt: Boolean)
}
