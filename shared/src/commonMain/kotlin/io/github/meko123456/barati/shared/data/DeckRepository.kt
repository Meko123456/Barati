package io.github.meko123456.barati.shared.data

import io.github.meko123456.barati.shared.domain.Deck
import io.github.meko123456.barati.shared.domain.FlashCard
import io.github.meko123456.barati.shared.domain.Grade
import io.github.meko123456.barati.shared.domain.ReviewInfo
import io.github.meko123456.barati.shared.domain.Scheduler
import io.github.meko123456.barati.shared.domain.Sm2
import kotlin.random.Random

/**
 * The single source of truth both apps talk to: decks plus per-card SM-2 state.
 * Pure Kotlin in commonMain, so the Android (Compose) and iOS (SwiftUI) UIs
 * drive identical behaviour. Decks are persisted through [DeckStore] and review
 * progress through [ReviewStore]; the defaults keep everything in memory (tests).
 *
 * The bundled [SampleDecks] seed only on first launch — once [deckStore] has
 * saved anything, the user's edits and deletions are authoritative.
 */
class DeckRepository(
    initial: List<Deck> = SampleDecks.all,
    private val store: ReviewStore = ReviewStore(InMemoryKeyValueStore()),
    private val deckStore: DeckStore = DeckStore(InMemoryKeyValueStore()),
) {

    private val decks: MutableList<Deck> = deckStore.load()?.toMutableList()
        ?: initial.toMutableList().also { deckStore.save(it) }
    private val reviews: MutableMap<String, ReviewInfo> = store.load().toMutableMap()

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
        store.save(reviews)
    }

    // --- Deck & card editing (all persisted) -----------------------------------

    /** Creates an empty deck with the given [name] and returns it. */
    fun createDeck(name: String): Deck {
        val deck = Deck(id = newId("deck"), name = name.trim(), cards = emptyList())
        decks += deck
        persist()
        return deck
    }

    fun renameDeck(deckId: String, name: String) {
        update(deckId) { it.copy(name = name.trim()) }
    }

    fun deleteDeck(deckId: String) {
        if (decks.removeAll { it.id == deckId }) persist()
    }

    /** Appends a new card to [deckId] and returns it (null if the deck is gone). */
    fun addCard(deckId: String, front: String, back: String): FlashCard? {
        val card = FlashCard(id = newId("card"), front = front.trim(), back = back.trim())
        val added = update(deckId) { it.copy(cards = it.cards + card) }
        return if (added) card else null
    }

    fun updateCard(deckId: String, cardId: String, front: String, back: String) {
        update(deckId) { deck ->
            deck.copy(cards = deck.cards.map { if (it.id == cardId) it.copy(front = front.trim(), back = back.trim()) else it })
        }
    }

    fun deleteCard(deckId: String, cardId: String) {
        update(deckId) { deck -> deck.copy(cards = deck.cards.filterNot { it.id == cardId }) }
    }

    /** Applies [transform] to the matching deck and persists; returns whether it matched. */
    private fun update(deckId: String, transform: (Deck) -> Deck): Boolean {
        val i = decks.indexOfFirst { it.id == deckId }
        if (i < 0) return false
        decks[i] = transform(decks[i])
        persist()
        return true
    }

    private fun persist() = deckStore.save(decks)

    private fun newId(prefix: String): String = "$prefix-${Random.nextLong().toULong().toString(16)}"
}
