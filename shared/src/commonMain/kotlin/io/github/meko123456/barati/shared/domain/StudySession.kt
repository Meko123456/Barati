package io.github.meko123456.barati.shared.domain

/**
 * One sitting with a deck's due cards, in order, where a forgotten card comes round again.
 *
 * A card graded Again goes to the back of the line and is shown again, until it is recalled.
 * Classic SM-2 does the same: the day's failed items are repeated before the day is done. Before
 * this, Again only scheduled the card for tomorrow and the sitting moved on without it. It is one
 * class here so the Android and iOS apps run the same sitting.
 *
 * Only a card's first grade in a sitting is a review; the repeats are practice. [grade] says
 * which, and only a review should reach the scheduler, so a card forgotten once is due again
 * tomorrow however the repeat goes.
 */
class StudySession(cards: List<FlashCard>) {

    private val line = ArrayDeque(cards)
    private val reviewedIds = mutableSetOf<String>()

    /** How many cards the sitting started with. */
    val total: Int = cards.size

    /** Cards graded at least once so far. */
    val reviewed: Int get() = reviewedIds.size

    /** The card to show now, or null once the sitting is over. */
    val current: FlashCard? get() = line.firstOrNull()

    /** Whether [current] was already graded in this sitting, so this showing is a repeat. */
    val isRepeat: Boolean get() = current?.let { it.id in reviewedIds } ?: false

    /** Cards still to show, repeats included. */
    val remaining: Int get() = line.size

    /**
     * Grades [current] and moves on. Returns true when this was the card's first grade in the
     * sitting: the one to pass to the scheduler.
     */
    fun grade(grade: Grade): Boolean {
        val card = line.removeFirstOrNull() ?: return false
        if (grade == Grade.AGAIN) line.addLast(card)
        return reviewedIds.add(card.id)
    }
}
