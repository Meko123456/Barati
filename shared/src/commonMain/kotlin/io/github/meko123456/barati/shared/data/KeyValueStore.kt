package io.github.meko123456.barati.shared.data

/**
 * Minimal persistent string map. The one thing that genuinely differs per
 * platform, so each app supplies its own native implementation:
 * SharedPreferences on Android, NSUserDefaults on iOS. Everything above this
 * (serialization, scheduling) stays in shared Kotlin.
 */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}

/** An in-memory store — the default when no platform store is supplied (tests). */
class InMemoryKeyValueStore : KeyValueStore {
    private val map = mutableMapOf<String, String>()
    override fun getString(key: String): String? = map[key]
    override fun putString(key: String, value: String) {
        map[key] = value
    }
}
