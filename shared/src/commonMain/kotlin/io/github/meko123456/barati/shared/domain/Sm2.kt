package io.github.meko123456.barati.shared.domain

import kotlin.math.roundToLong

/** How well a card was recalled. Quality maps to the SM-2 0–5 scale. */
enum class Grade(val quality: Int) {
    AGAIN(1),
    HARD(3),
    GOOD(4),
    EASY(5),
}

/** Per-card scheduling state. */
data class ReviewState(
    val repetitions: Int = 0,
    val intervalDays: Long = 0,
    val easeFactor: Double = INITIAL_EASE,
) {
    companion object {
        const val INITIAL_EASE = 2.5
        const val MIN_EASE = 1.3
    }
}

/**
 * The classic SM-2 spaced-repetition algorithm. Pure Kotlin in commonMain so
 * both the Android and iOS apps share exactly one, unit-tested implementation.
 */
object Sm2 {

    fun schedule(state: ReviewState, grade: Grade): ReviewState {
        val q = grade.quality
        val updatedEase = (state.easeFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)))
            .coerceAtLeast(ReviewState.MIN_EASE)

        if (q < 3) {
            return state.copy(repetitions = 0, intervalDays = 1, easeFactor = updatedEase)
        }

        val newReps = state.repetitions + 1
        val newInterval = when (newReps) {
            1 -> 1L
            2 -> 6L
            else -> (state.intervalDays * updatedEase).roundToLong()
        }
        return ReviewState(newReps, newInterval, updatedEase)
    }

    fun dueEpochDay(state: ReviewState, lastReviewedEpochDay: Long): Long =
        lastReviewedEpochDay + state.intervalDays

    fun isDue(state: ReviewState, lastReviewedEpochDay: Long, todayEpochDay: Long): Boolean =
        dueEpochDay(state, lastReviewedEpochDay) <= todayEpochDay
}
