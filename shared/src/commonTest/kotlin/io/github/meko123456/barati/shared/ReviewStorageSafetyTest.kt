package io.github.meko123456.barati.shared

import io.github.meko123456.barati.shared.data.DeckRepository
import io.github.meko123456.barati.shared.data.InMemoryKeyValueStore
import io.github.meko123456.barati.shared.data.ReviewStore
import io.github.meko123456.barati.shared.domain.Grade
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What happens when the saved review state cannot be read: the decks' safety (DeckStorageSafetyTest)
 * for the other half of the user's data, every card's spaced-repetition schedule.
 *
 * It used to read as "nothing reviewed yet", and the first grade afterwards saved a map holding
 * only that card over the stored one.
 */
class ReviewStorageSafetyTest {

    private val corrupt = """{"k1":{"state":{"repetitions":3,"inter"""

    @Test
    fun anUnreadableBlobIsCopiedAsideBeforeAGradeReplacesIt() {
        val kv = InMemoryKeyValueStore()
        kv.putString("barati.reviews.v1", corrupt)

        val repo = DeckRepository(store = ReviewStore(kv))
        repo.grade("k2", Grade.GOOD, today = 20_000)

        // The grade is saved, and the history it replaced is still there to be recovered.
        assertTrue(kv.getString("barati.reviews.v1")!!.contains("k2"))
        assertEquals(corrupt, ReviewStore(kv).backup(), "the only copy of the schedules must be kept")
    }

    @Test
    fun theCopyIsTakenOnceAndNeverOverwritten() {
        val kv = InMemoryKeyValueStore()
        kv.putString("barati.reviews.v1", corrupt)
        val store = ReviewStore(kv)
        store.load()

        // The user carries on, grades something, and that in turn goes bad later.
        DeckRepository(store = store).grade("k1", Grade.HARD, today = 20_001)
        kv.putString("barati.reviews.v1", "also broken")
        store.load()

        assertEquals(corrupt, store.backup(), "the first copy is the one that was really the user's")
    }

    @Test
    fun readableReviewsAreNotCopied() {
        val kv = InMemoryKeyValueStore()
        val store = ReviewStore(kv)
        DeckRepository(store = store).grade("k1", Grade.GOOD, today = 20_000)

        assertEquals(setOf("k1"), store.load().keys)
        assertNull(store.backup())
    }
}
