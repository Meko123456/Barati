package io.github.meko123456.barati.shared.data

import io.github.meko123456.barati.shared.domain.Deck
import kotlinx.serialization.json.Json

/**
 * Persists the user's decks (and their cards) as JSON in a [KeyValueStore], the
 * same pattern as [ReviewStore]. Serialization lives here in shared Kotlin so
 * Android and iOS read and write identical data — only the storage is native.
 */
class DeckStore(private val kv: KeyValueStore) {

    /**
     * What was found in storage.
     *
     * Three states, not two, and that is the whole point of this type. [load] used to return `null`
     * for both "nothing saved yet" and "saved but it would not decode", and the repository read that
     * single `null` as first launch — so a blob it could not parse was answered by seeding the sample
     * decks and **saving them straight over it**, in the constructor, before the UI drew a frame.
     * A truncated write or a field this version does not understand would have taken the lot.
     */
    sealed interface Stored {
        /** Nothing has ever been saved. Safe to seed. */
        data object Missing : Stored

        /** Decoded cleanly. */
        data class Decks(val decks: List<Deck>) : Stored

        /** Something is there and this version cannot read it. Never write over it. */
        data object Unreadable : Stored
    }

    /**
     * Reads storage, and on finding something unreadable takes a copy of it first.
     *
     * The copy is the difference between a bad release losing a user's decks and merely failing to
     * show them: whatever is under [KEY] is the only record of them, and every write path in this
     * app starts by reading and ends by replacing.
     */
    fun read(): Stored {
        val raw = kv.getString(KEY) ?: return Stored.Missing
        runCatching { json.decodeFromString<List<Deck>>(raw) }
            .getOrNull()
            ?.let { return Stored.Decks(it) }

        // Only the first time: a later successful save followed by another failure must not
        // overwrite the copy of the data that was actually the user's.
        if (kv.getString(BACKUP_KEY) == null) kv.putString(BACKUP_KEY, raw)
        return Stored.Unreadable
    }

    /** The unreadable blob kept aside by [read], if there is one. */
    fun backup(): String? = kv.getString(BACKUP_KEY)

    fun save(decks: List<Deck>) {
        kv.putString(KEY, json.encodeToString(decks))
    }

    private companion object {
        const val KEY = "barati.decks.v1"
        const val BACKUP_KEY = "barati.decks.v1.unreadable"
        val json = Json { ignoreUnknownKeys = true }
    }
}
