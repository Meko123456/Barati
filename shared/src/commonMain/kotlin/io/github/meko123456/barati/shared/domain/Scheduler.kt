package io.github.meko123456.barati.shared.domain

/** Persisted review state for a card (defaults = a brand-new, never-seen card). */
data class ReviewInfo(
    val state: ReviewState = ReviewState(),
    val lastReviewedEpochDay: Long = 0,
)

/**
 * Pure due-card selection: which cards should be studied today and in what
 * order. Shared by both apps so scheduling behaves identically everywhere.
 */
object Scheduler {

    /**
     * Cards that are due on [today] — never-seen cards are always due — ordered
     * most-overdue first (new cards lead).
     */
    fun due(cards: List<FlashCard>, reviews: Map<String, ReviewInfo>, today: Long): List<FlashCard> =
        cards
            .filter { card ->
                val r = reviews[card.id]
                r == null || Sm2.isDue(r.state, r.lastReviewedEpochDay, today)
            }
            .sortedBy { card ->
                val r = reviews[card.id] ?: return@sortedBy Long.MIN_VALUE
                Sm2.dueEpochDay(r.state, r.lastReviewedEpochDay)
            }
}
