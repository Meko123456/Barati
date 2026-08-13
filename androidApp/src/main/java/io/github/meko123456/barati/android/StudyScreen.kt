package io.github.meko123456.barati.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.meko123456.barati.shared.domain.Grade

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(viewModel: BaratiViewModel, deckId: String, onBack: () -> Unit) {
    val queue = remember { viewModel.dueQueue(deckId) }
    var index by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val content = Modifier.fillMaxSize().padding(padding).padding(20.dp)
        when {
            queue.isEmpty() -> Column(content, Arrangement.Center, Alignment.CenterHorizontally) {
                Text("Nothing due — come back later 🎉", textAlign = TextAlign.Center)
            }
            index >= queue.size -> Column(content, Arrangement.Center, Alignment.CenterHorizontally) {
                Text("Session complete", style = MaterialTheme.typography.titleMedium)
                Text("${queue.size} cards reviewed", style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) { Text("Done") }
            }
            else -> {
                val card = queue[index]
                Column(content, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LinearProgressIndicator(
                        progress = { (index + 1f) / queue.size },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Card ${index + 1} of ${queue.size}", style = MaterialTheme.typography.labelMedium)

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(24.dp)) {
                            Text(card.front, style = MaterialTheme.typography.titleLarge)
                            if (revealed) {
                                Text(
                                    card.back,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(top = 16.dp),
                                )
                            }
                        }
                    }

                    if (!revealed) {
                        Button(onClick = { revealed = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Show answer")
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Grade.entries.forEach { grade ->
                                FilledTonalButton(
                                    onClick = {
                                        viewModel.grade(card.id, grade)
                                        index++
                                        revealed = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        grade.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
