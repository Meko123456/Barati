package io.github.meko123456.barati.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckListScreen(
    viewModel: BaratiViewModel,
    onOpenDeck: (String) -> Unit,
    onEditDeck: (String) -> Unit,
) {
    val version = viewModel.version // snapshot read → recompose after edits/grading
    val summaries = remember(version) { viewModel.deckSummaries() }
    var addingDeck by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Barati 🗂️") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { addingDeck = true }) {
                Icon(Icons.Default.Add, contentDescription = "New deck")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Your decks", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
            }
            items(summaries, key = { it.first.id }) { (deck, due) ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onOpenDeck(deck.id) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(deck.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${deck.cards.size} ${if (deck.cards.size == 1) "card" else "cards"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            if (due > 0) "$due due" else "✓",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (due > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        IconButton(onClick = { onEditDeck(deck.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit ${deck.name}")
                        }
                    }
                }
            }
        }
    }

    if (addingDeck) {
        TextFieldDialog(
            title = "New deck",
            label = "Deck name",
            confirmLabel = "Create",
            onConfirm = { name -> viewModel.createDeck(name); addingDeck = false },
            onDismiss = { addingDeck = false },
        )
    }
}
