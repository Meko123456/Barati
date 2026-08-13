package io.github.meko123456.barati.shared.data

import io.github.meko123456.barati.shared.domain.ReviewInfo
import kotlinx.serialization.json.Json

/**
 * Persists the per-card SM-2 review state as JSON in a [KeyValueStore]. The
 * (de)serialization lives here in shared Kotlin, so Android and iOS persist
 * and reload identical data — only the underlying storage is native.
 */
class ReviewStore(private val kv: KeyValueStore) {

    fun load(): Map<String, ReviewInfo> {
        val raw = kv.getString(KEY) ?: return emptyMap()
        return runCatching { json.decodeFromString<Map<String, ReviewInfo>>(raw) }
            .getOrDefault(emptyMap())
    }

    fun save(reviews: Map<String, ReviewInfo>) {
        kv.putString(KEY, json.encodeToString(reviews))
    }

    private companion object {
        const val KEY = "barati.reviews.v1"
        val json = Json { ignoreUnknownKeys = true }
    }
}
