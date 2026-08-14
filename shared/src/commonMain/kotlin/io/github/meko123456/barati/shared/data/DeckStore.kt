package io.github.meko123456.barati.shared.data

import io.github.meko123456.barati.shared.domain.Deck
import kotlinx.serialization.json.Json

/**
 * Persists the user's decks (and their cards) as JSON in a [KeyValueStore], the
 * same pattern as [ReviewStore]. Serialization lives here in shared Kotlin so
 * Android and iOS read and write identical data — only the storage is native.
 *
 * [load] returns null when nothing has been saved yet, so the repository knows
 * to seed the bundled sample decks on first launch (and never re-seed after the
 * user has edited or deleted them).
 */
class DeckStore(private val kv: KeyValueStore) {

    fun load(): List<Deck>? {
        val raw = kv.getString(KEY) ?: return null
        return runCatching { json.decodeFromString<List<Deck>>(raw) }.getOrNull()
    }

    fun save(decks: List<Deck>) {
        kv.putString(KEY, json.encodeToString(decks))
    }

    private companion object {
        const val KEY = "barati.decks.v1"
        val json = Json { ignoreUnknownKeys = true }
    }
}
