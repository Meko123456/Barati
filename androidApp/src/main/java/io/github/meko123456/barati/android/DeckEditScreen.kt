package io.github.meko123456.barati.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.meko123456.barati.shared.domain.FlashCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckEditScreen(viewModel: BaratiViewModel, deckId: String, onBack: () -> Unit) {
    val version = viewModel.version
    val deck = remember(version) { viewModel.deck(deckId) }

    // The deck was deleted from under us — leave the screen.
    if (deck == null) {
        onBack()
        return
    }

    var addingCard by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<FlashCard?>(null) }
    var renamingDeck by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(deck.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { renamingDeck = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename deck")
                    }
                    IconButton(onClick = { viewModel.deleteDeck(deckId); onBack() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete deck")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { addingCard = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add card")
            }
        },
    ) { padding ->
        if (deck.cards.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No cards yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Tap + to add your first card to this deck.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(deck.cards, key = { it.id }) { card ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(card.front, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    card.back,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { editingCard = card }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit card")
                            }
                            IconButton(onClick = { viewModel.deleteCard(deckId, card.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete card")
                            }
                        }
                    }
                }
            }
        }
    }

    if (addingCard) {
        CardDialog(
            title = "Add card",
            confirmLabel = "Add",
            onConfirm = { front, back -> viewModel.addCard(deckId, front, back); addingCard = false },
            onDismiss = { addingCard = false },
        )
    }

    editingCard?.let { card ->
        CardDialog(
            title = "Edit card",
            confirmLabel = "Save",
            initialFront = card.front,
            initialBack = card.back,
            onConfirm = { front, back -> viewModel.updateCard(deckId, card.id, front, back); editingCard = null },
            onDismiss = { editingCard = null },
        )
    }

    if (renamingDeck) {
        TextFieldDialog(
            title = "Rename deck",
            label = "Deck name",
            confirmLabel = "Save",
            initial = deck.name,
            onConfirm = { name -> viewModel.renameDeck(deckId, name); renamingDeck = false },
            onDismiss = { renamingDeck = false },
        )
    }
}
