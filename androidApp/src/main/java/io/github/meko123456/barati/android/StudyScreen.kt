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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.meko123456.barati.shared.domain.Grade

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(viewModel: BaratiViewModel, deckId: String, onBack: () -> Unit) {
    // From the ViewModel, which outlives the Activity: a sitting rebuilt here after a rotation
    // lost its repeats, because a card graded Again is no longer due.
    val session = remember(deckId) { viewModel.studySession(deckId) }
    // The session is shared Kotlin, not Compose state. The card on show is, and every grade sets
    // it, even to the same card: a lone card graded Again comes straight back as a repeat.
    var card by remember { mutableStateOf(session.current, neverEqualPolicy()) }
    var revealed by rememberSaveable { mutableStateOf(false) }

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
        val shown = card
        when {
            session.total == 0 -> Column(content, Arrangement.Center, Alignment.CenterHorizontally) {
                Text("Nothing due — come back later 🎉", textAlign = TextAlign.Center)
            }
            shown == null -> Column(content, Arrangement.Center, Alignment.CenterHorizontally) {
                Text("Session complete", style = MaterialTheme.typography.titleMedium)
                Text("${session.total} cards reviewed", style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) { Text("Done") }
            }
            else -> {
                val position = minOf(session.reviewed + 1, session.total)
                Column(content, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LinearProgressIndicator(
                        progress = { position.toFloat() / session.total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        if (session.isRepeat) "Once more · ${session.remaining} left" else "Card $position of ${session.total}",
                        style = MaterialTheme.typography.labelMedium,
                    )

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(24.dp)) {
                            Text(shown.front, style = MaterialTheme.typography.titleLarge)
                            if (revealed) {
                                Text(
                                    shown.back,
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
                                        // Only a card's first grade in the sitting is scheduled.
                                        if (session.grade(grade)) viewModel.grade(shown.id, grade)
                                        card = session.current
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
