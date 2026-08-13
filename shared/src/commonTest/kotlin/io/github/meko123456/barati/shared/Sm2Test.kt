package io.github.meko123456.barati.shared

import io.github.meko123456.barati.shared.domain.Grade
import io.github.meko123456.barati.shared.domain.ReviewState
import io.github.meko123456.barati.shared.domain.Sm2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Sm2Test {

    private val fresh = ReviewState()

    @Test
    fun firstGoodReviewSchedulesOneDay() {
        val s = Sm2.schedule(fresh, Grade.GOOD)
        assertEquals(1, s.repetitions)
        assertEquals(1L, s.intervalDays)
    }

    @Test
    fun secondPassJumpsToSixDays() {
        val s = Sm2.schedule(Sm2.schedule(fresh, Grade.GOOD), Grade.GOOD)
        assertEquals(6L, s.intervalDays)
    }

    @Test
    fun thirdPassMultipliesByEase() {
        var s = Sm2.schedule(fresh, Grade.GOOD)
        s = Sm2.schedule(s, Grade.GOOD)
        s = Sm2.schedule(s, Grade.GOOD)
        assertEquals(15L, s.intervalDays)
    }

    @Test
    fun againResetsToRelearn() {
        var s = Sm2.schedule(Sm2.schedule(fresh, Grade.GOOD), Grade.GOOD)
        s = Sm2.schedule(s, Grade.AGAIN)
        assertEquals(0, s.repetitions)
        assertEquals(1L, s.intervalDays)
    }

    @Test
    fun easeMovesWithDifficulty() {
        assertTrue(Sm2.schedule(fresh, Grade.EASY).easeFactor > ReviewState.INITIAL_EASE)
        assertTrue(Sm2.schedule(fresh, Grade.HARD).easeFactor < ReviewState.INITIAL_EASE)
    }

    @Test
    fun easeNeverBelowFloor() {
        var s = fresh
        repeat(20) { s = Sm2.schedule(s, Grade.HARD) }
        assertTrue(s.easeFactor >= ReviewState.MIN_EASE)
    }

    @Test
    fun dueLogic() {
        assertTrue(Sm2.isDue(fresh, lastReviewedEpochDay = 100, todayEpochDay = 100))
        val s = Sm2.schedule(Sm2.schedule(fresh, Grade.GOOD), Grade.GOOD) // interval 6
        assertFalse(Sm2.isDue(s, lastReviewedEpochDay = 100, todayEpochDay = 105))
        assertTrue(Sm2.isDue(s, lastReviewedEpochDay = 100, todayEpochDay = 106))
    }
}
