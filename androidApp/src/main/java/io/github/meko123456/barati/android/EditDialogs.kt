package io.github.meko123456.barati.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.meko123456.barati.shared.domain.Deck

/** Single-field prompt used for creating and renaming decks. */
@Composable
fun TextFieldDialog(
    title: String,
    label: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    initial: String = "",
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
                // Done does what the button does, and nothing while the button is disabled.
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onConfirm(text) }),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text) }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Two-field prompt used for adding and editing a card (front + back). */
@Composable
fun CardDialog(
    title: String,
    confirmLabel: String,
    onConfirm: (front: String, back: String) -> Unit,
    onDismiss: () -> Unit,
    initialFront: String = "",
    initialBack: String = "",
) {
    var front by rememberSaveable { mutableStateOf(initialFront) }
    var back by rememberSaveable { mutableStateOf(initialBack) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text("Front (prompt)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text("Back (answer)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = front.isNotBlank() && back.isNotBlank(),
                onClick = { onConfirm(front, back) },
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * The add-card dialog for text picked out or shared in another app, or the New card shortcut: the
 * front filled in and the back ready to type (or both empty), plus the deck the card goes into,
 * since nothing in Barati was open to say which. With no decks yet it names a new one. [onAdd]
 * gets a deck id, or null and the new deck's name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardFromTextDialog(
    front: String,
    decks: List<Deck>,
    initialDeckId: String?,
    onAdd: (deckId: String?, newDeckName: String, front: String, back: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var frontText by rememberSaveable { mutableStateOf(front) }
    var back by rememberSaveable { mutableStateOf("") }
    var deckId by rememberSaveable {
        mutableStateOf(initialDeckId?.takeIf { id -> decks.any { it.id == id } } ?: decks.firstOrNull()?.id)
    }
    var newDeck by rememberSaveable { mutableStateOf("") }
    var choosing by remember { mutableStateOf(false) }
    val chosen = decks.firstOrNull { it.id == deckId }
    val canAdd = frontText.isNotBlank() && back.isNotBlank() &&
        (chosen != null || (decks.isEmpty() && newDeck.isNotBlank()))
    val frontFocus = remember { FocusRequester() }
    val backFocus = remember { FocusRequester() }
    val add = { onAdd(chosen?.id, newDeck, frontText, back) }

    AlertDialog(
        onDismissRequest = onDismiss,
        // Edge to edge, inside the system bars and above the keyboard. Its three fields made it too
        // tall to pan into view whole: the window slid up to the focused field and left Add under
        // the keyboard. Now the fields scroll inside it and Add stays in view.
        properties = DialogProperties(decorFitsSystemWindows = false),
        modifier = Modifier.safeDrawingPadding(),
        title = { Text("Add card") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                if (decks.isEmpty()) {
                    OutlinedTextField(
                        value = newDeck,
                        onValueChange = { newDeck = it },
                        label = { Text("New deck") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    ExposedDropdownMenuBox(expanded = choosing, onExpandedChange = { choosing = it }) {
                        OutlinedTextField(
                            value = chosen?.name.orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = { Text("Deck") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = choosing) },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(expanded = choosing, onDismissRequest = { choosing = false }) {
                            decks.forEach { deck ->
                                DropdownMenuItem(
                                    text = { Text(deck.name) },
                                    onClick = { deckId = deck.id; choosing = false },
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = frontText,
                    onValueChange = { frontText = it },
                    label = { Text("Front (prompt)") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { backFocus.requestFocus() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(frontFocus),
                )
                // The keyboard's action key adds the card, as Add does.
                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text("Back (answer)") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (canAdd) add() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(backFocus),
                )
            }
            // The front came with the text, so what's left to type is the answer; from the New card
            // shortcut there is no text yet, and the front comes first.
            LaunchedEffect(Unit) { (if (front.isBlank()) frontFocus else backFocus).requestFocus() }
        },
        confirmButton = {
            TextButton(enabled = canAdd, onClick = add) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
