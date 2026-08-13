package io.github.meko123456.barati.shared.data

import io.github.meko123456.barati.shared.domain.Deck
import io.github.meko123456.barati.shared.domain.FlashCard
import io.github.meko123456.barati.shared.domain.Grade
import io.github.meko123456.barati.shared.domain.ReviewInfo
import io.github.meko123456.barati.shared.domain.Scheduler
import io.github.meko123456.barati.shared.domain.Sm2

/**
 * The single source of truth both apps talk to: decks plus per-card SM-2 state.
 * In-memory for now (persistence is a later issue). Pure Kotlin in commonMain,
 * so the Android (Compose) and iOS (SwiftUI) UIs drive identical behaviour.
 */
class DeckRepository(initial: List<Deck> = SampleDecks.all) {

    private val decks: MutableList<Deck> = initial.toMutableList()
    private val reviews: MutableMap<String, ReviewInfo> = mutableMapOf()

    fun decks(): List<Deck> = decks.toList()

    fun deck(id: String): Deck? = decks.firstOrNull { it.id == id }

    fun reviewInfo(cardId: String): ReviewInfo = reviews[cardId] ?: ReviewInfo()

    /** Cards due to study in [deckId] today, ordered most-overdue first. */
    fun dueCards(deckId: String, today: Long): List<FlashCard> =
        deck(deckId)?.let { Scheduler.due(it.cards, reviews, today) } ?: emptyList()

    fun dueCount(deckId: String, today: Long): Int = dueCards(deckId, today).size

    /** Records a grade, advancing the card's SM-2 schedule from [today]. */
    fun grade(cardId: String, grade: Grade, today: Long) {
        val current = reviewInfo(cardId)
        reviews[cardId] = ReviewInfo(Sm2.schedule(current.state, grade), today)
    }

    fun addDeck(deck: Deck) {
        decks += deck
    }

    fun addCard(deckId: String, card: FlashCard) {
        val i = decks.indexOfFirst { it.id == deckId }
        if (i >= 0) decks[i] = decks[i].copy(cards = decks[i].cards + card)
    }
}
