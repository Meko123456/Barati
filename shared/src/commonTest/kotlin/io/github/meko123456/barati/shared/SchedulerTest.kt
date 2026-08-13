package io.github.meko123456.barati.shared

import io.github.meko123456.barati.shared.data.DeckRepository
import io.github.meko123456.barati.shared.domain.FlashCard
import io.github.meko123456.barati.shared.domain.Grade
import io.github.meko123456.barati.shared.domain.ReviewInfo
import io.github.meko123456.barati.shared.domain.ReviewState
import io.github.meko123456.barati.shared.domain.Scheduler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SchedulerTest {

    private val cards = (1..3).map { FlashCard("c$it", "front$it", "back$it") }

    @Test
    fun newCardsAreAllDue() {
        val due = Scheduler.due(cards, reviews = emptyMap(), today = 100)
        assertEquals(3, due.size)
    }

    @Test
    fun notDueCardsAreExcluded() {
        // c1 reviewed today with a 6-day interval -> not due at today
        val reviews = mapOf(
            "c1" to ReviewInfo(ReviewState(repetitions = 2, intervalDays = 6), lastReviewedEpochDay = 100),
        )
        val due = Scheduler.due(cards, reviews, today = 103)
        assertTrue(due.none { it.id == "c1" })
        assertEquals(2, due.size)
    }

    @Test
    fun repositoryGradeMakesACardNotDue() {
        val repo = DeckRepository(
            initial = listOf(io.github.meko123456.barati.shared.domain.Deck("d", "D", cards)),
        )
        assertEquals(3, repo.dueCount("d", today = 100))
        repo.grade("c1", Grade.GOOD, today = 100) // interval 1 -> due tomorrow
        assertEquals(2, repo.dueCount("d", today = 100))
        assertEquals(3, repo.dueCount("d", today = 101)) // due again next day
    }
}
