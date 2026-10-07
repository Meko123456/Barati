package io.github.meko123456.barati.shared

import io.github.meko123456.barati.shared.domain.FlashCard
import io.github.meko123456.barati.shared.domain.Grade
import io.github.meko123456.barati.shared.domain.StudySession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StudySessionTest {

    private val cards = listOf("a", "b", "c").map { FlashCard(it, "front $it", "back $it") }

    /** Grades every card shown with [gradeFor] and returns the ids in the order they came. */
    private fun StudySession.run(gradeFor: (FlashCard, Int) -> Grade): List<String> {
        val shown = mutableListOf<String>()
        while (true) {
            val card = current ?: return shown
            shown += card.id
            grade(gradeFor(card, shown.count { it == card.id }))
        }
    }

    @Test
    fun cardsRecalledFirstTimeAreShownOnceEach() {
        val session = StudySession(cards)
        assertEquals(listOf("a", "b", "c"), session.run { _, _ -> Grade.GOOD })
        assertEquals(3, session.reviewed)
    }

    @Test
    fun aForgottenCardComesBackAfterTheOthersUntilItIsRecalled() {
        val session = StudySession(cards)
        // b is forgotten twice, then recalled on its third showing.
        val shown = session.run { card, time -> if (card.id == "b" && time < 3) Grade.AGAIN else Grade.GOOD }
        assertEquals(listOf("a", "b", "c", "b", "b"), shown)
        assertNull(session.current)
    }

    @Test
    fun onlyTheFirstGradeOfACardIsAReview() {
        val session = StudySession(cards.take(1))
        assertTrue(session.grade(Grade.AGAIN), "the first grade goes to the scheduler")
        assertTrue(session.isRepeat)
        assertFalse(session.grade(Grade.GOOD), "the repeat is practice")
        assertNull(session.current)
    }

    @Test
    fun hardAndEasyDoNotBringACardBack() {
        val session = StudySession(cards)
        assertEquals(listOf("a", "b", "c"), session.run { card, _ -> if (card.id == "a") Grade.HARD else Grade.EASY })
    }

    @Test
    fun progressCountsCardsNotShowings() {
        val session = StudySession(cards)
        assertEquals(3, session.total)
        session.grade(Grade.AGAIN)
        assertEquals(1, session.reviewed)
        assertEquals(3, session.remaining, "b, c, and a again")
        assertFalse(session.isRepeat)
    }

    @Test
    fun anEmptySittingHasNothingToShow() {
        val session = StudySession(emptyList())
        assertNull(session.current)
        assertFalse(session.grade(Grade.GOOD))
    }
}
