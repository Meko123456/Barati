package io.github.meko123456.barati.shared

import io.github.meko123456.barati.shared.data.DeckRepository
import io.github.meko123456.barati.shared.data.InMemoryKeyValueStore
import io.github.meko123456.barati.shared.data.ReviewStore
import io.github.meko123456.barati.shared.domain.Grade
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReviewStoreTest {

    @Test
    fun gradedProgressSurvivesReconstruction() {
        val kv = InMemoryKeyValueStore()
        val today = 20_000L

        // First "session": grade one Kotlin card as GOOD.
        val first = DeckRepository(store = ReviewStore(kv))
        val card = first.dueCards("kotlin", today).first()
        val dueBefore = first.dueCount("kotlin", today)
        first.grade(card.id, Grade.GOOD, today)

        // New repository over the SAME storage = an app restart.
        val restarted = DeckRepository(store = ReviewStore(kv))

        // The graded card is no longer due today; the count dropped by one.
        assertEquals(dueBefore - 1, restarted.dueCount("kotlin", today))
        assertTrue(restarted.dueCards("kotlin", today).none { it.id == card.id })
    }

    @Test
    fun emptyStoreLoadsCleanly() {
        val repo = DeckRepository(store = ReviewStore(InMemoryKeyValueStore()))
        assertEquals(5, repo.dueCount("kotlin", 20_000L))
    }
}
