package io.github.meko123456.barati.shared.data

import io.github.meko123456.barati.shared.domain.ReviewInfo
import kotlinx.serialization.json.Json

/**
 * Persists the per-card SM-2 review state as JSON in a [KeyValueStore]. The
 * (de)serialization lives here in shared Kotlin, so Android and iOS persist
 * and reload identical data — only the underlying storage is native.
 */
class ReviewStore(private val kv: KeyValueStore) {

    /**
     * The saved review state, or an empty map when there is none or it can't be read.
     *
     * An unreadable blob is copied aside first, as [DeckStore.read] does for decks. It used to read
     * as "no reviews" and nothing more, so the next grade saved the one card it graded straight over
     * it: a truncated write, or a field this version doesn't understand, and every card lost its
     * schedule at the first tap.
     */
    fun load(): Map<String, ReviewInfo> {
        val raw = kv.getString(KEY) ?: return emptyMap()
        runCatching { json.decodeFromString<Map<String, ReviewInfo>>(raw) }
            .getOrNull()
            ?.let { return it }

        // Only the first time: a later failure must not overwrite the copy that was the user's.
        if (kv.getString(BACKUP_KEY) == null) kv.putString(BACKUP_KEY, raw)
        return emptyMap()
    }

    /** The unreadable blob kept aside by [load], if there is one. */
    fun backup(): String? = kv.getString(BACKUP_KEY)

    fun save(reviews: Map<String, ReviewInfo>) {
        kv.putString(KEY, json.encodeToString(reviews))
    }

    private companion object {
        const val KEY = "barati.reviews.v1"
        const val BACKUP_KEY = "barati.reviews.v1.unreadable"
        val json = Json { ignoreUnknownKeys = true }
    }
}
