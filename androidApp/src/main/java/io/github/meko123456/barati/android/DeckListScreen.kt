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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckListScreen(viewModel: BaratiViewModel, onOpenDeck: (String) -> Unit) {
    val version = viewModel.version // snapshot read → recompose after grading
    val summaries = remember(version) { viewModel.deckSummaries() }

    Scaffold(topBar = { TopAppBar(title = { Text("Barati 🗂️") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Your decks", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
            }
            items(summaries, key = { it.first.id }) { (deck, due) ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenDeck(deck.id) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(deck.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${deck.cards.size} cards",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            if (due > 0) "$due due" else "✓",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (due > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
