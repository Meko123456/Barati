package io.github.meko123456.barati.shared

import io.github.meko123456.barati.shared.data.DeckRepository
import io.github.meko123456.barati.shared.data.DeckStore
import io.github.meko123456.barati.shared.data.InMemoryKeyValueStore
import io.github.meko123456.barati.shared.data.SampleDecks
import io.github.meko123456.barati.shared.domain.Deck
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What happens when the saved decks cannot be read.
 *
 * This used to be indistinguishable from first launch: the store returned null for both, and the
 * repository answered null by seeding the sample decks and saving them over whatever was there —
 * in its constructor, before anything rendered. A truncated write, or one field this version does
 * not understand, and the user's decks were gone with no way back.
 */
class DeckStorageSafetyTest {

    private val corrupt = """[{"id":"d1","name":"Kotlin","car"""

    @Test
    fun nothingSavedYetIsFirstLaunch() {
        val kv = InMemoryKeyValueStore()
        assertEquals(DeckStore.Stored.Missing, DeckStore(kv).read())

        val repo = DeckRepository(deckStore = DeckStore(kv))
        assertEquals(SampleDecks.all.size, repo.decks().size, "first launch should seed the samples")
    }

    @Test
    fun savedDecksAreReadBack() {
        val kv = InMemoryKeyValueStore()
        val store = DeckStore(kv)
        store.save(listOf(Deck(id = "d1", name = "Georgian", cards = emptyList())))

        val stored = store.read()
        assertTrue(stored is DeckStore.Stored.Decks)
        assertEquals("Georgian", stored.decks.single().name)
    }

    @Test
    fun anUnreadableBlobIsNotMistakenForFirstLaunch() {
        val kv = InMemoryKeyValueStore()
        kv.putString("barati.decks.v1", corrupt)

        assertEquals(DeckStore.Stored.Unreadable, DeckStore(kv).read())
    }

    @Test
    fun anUnreadableBlobIsCopiedAsideBeforeAnythingElseHappens() {
        val kv = InMemoryKeyValueStore()
        kv.putString("barati.decks.v1", corrupt)
        val store = DeckStore(kv)

        store.read()

        assertEquals(corrupt, store.backup(), "the only copy of the user's decks must be kept")
    }

    @Test
    fun theRepositoryDoesNotSeedOverAnUnreadableBlob() {
        val kv = InMemoryKeyValueStore()
        kv.putString("barati.decks.v1", corrupt)

        val repo = DeckRepository(deckStore = DeckStore(kv))

        // The old behaviour: sample decks seeded and written straight over the corrupt value.
        assertTrue(repo.decks().isEmpty(), "nothing should be invented on top of unreadable data")
        assertEquals(corrupt, kv.getString("barati.decks.v1"), "the stored value must be untouched")
        assertNotNull(DeckStore(kv).backup())
    }

    @Test
    fun theCopyIsTakenOnceAndNeverOverwritten() {
        val kv = InMemoryKeyValueStore()
        kv.putString("barati.decks.v1", corrupt)
        val store = DeckStore(kv)
        store.read()

        // The user carries on, saves something, and that in turn goes bad later.
        store.save(listOf(Deck(id = "d2", name = "Later", cards = emptyList())))
        kv.putString("barati.decks.v1", "also broken")
        store.read()

        assertEquals(corrupt, store.backup(), "the first copy is the one that was really the user's")
    }

    @Test
    fun thereIsNoCopyWhenNothingWentWrong() {
        val kv = InMemoryKeyValueStore()
        val store = DeckStore(kv)
        store.save(listOf(Deck(id = "d1", name = "Fine", cards = emptyList())))
        store.read()

        assertNull(store.backup())
    }
}
