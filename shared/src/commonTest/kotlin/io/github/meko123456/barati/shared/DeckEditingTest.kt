package io.github.meko123456.barati.shared

import io.github.meko123456.barati.shared.data.DeckRepository
import io.github.meko123456.barati.shared.data.DeckStore
import io.github.meko123456.barati.shared.data.InMemoryKeyValueStore
import io.github.meko123456.barati.shared.domain.Grade
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeckEditingTest {

    @Test
    fun createAddEditDeleteRoundTrip() {
        val repo = DeckRepository(deckStore = DeckStore(InMemoryKeyValueStore()))

        val deck = repo.createDeck("  My deck  ")
        assertEquals("My deck", deck.name) // trimmed
        assertTrue(repo.decks().any { it.id == deck.id })

        val card = repo.addCard(deck.id, " front ", " back ")!!
        assertEquals("front", card.front)
        assertEquals(1, repo.deck(deck.id)!!.cards.size)

        repo.updateCard(deck.id, card.id, "front2", "back2")
        assertEquals("front2", repo.deck(deck.id)!!.cards.single().front)

        repo.renameDeck(deck.id, "Renamed")
        assertEquals("Renamed", repo.deck(deck.id)!!.name)

        repo.deleteCard(deck.id, card.id)
        assertTrue(repo.deck(deck.id)!!.cards.isEmpty())

        repo.deleteDeck(deck.id)
        assertNull(repo.deck(deck.id))
    }

    @Test
    fun aDeletedCardComesBackInItsPlaceWithItsSchedule() {
        val repo = DeckRepository(deckStore = DeckStore(InMemoryKeyValueStore()))
        val deck = repo.createDeck("Deck")
        val a = repo.addCard(deck.id, "a", "1")!!
        val b = repo.addCard(deck.id, "b", "2")!!
        val c = repo.addCard(deck.id, "c", "3")!!
        repo.grade(b.id, Grade.GOOD, today = 100)
        val schedule = repo.reviewInfo(b.id)

        repo.deleteCard(deck.id, b.id)
        repo.restoreCard(deck.id, b, index = 1)

        assertEquals(listOf(a.id, b.id, c.id), repo.deck(deck.id)!!.cards.map { it.id })
        assertEquals(schedule, repo.reviewInfo(b.id), "the card's schedule must come back with it")
        repo.restoreCard(deck.id, b, index = 1) // a second undo
        assertEquals(3, repo.deck(deck.id)!!.cards.size, "a second undo must not add a copy")
    }

    @Test
    fun editsSurviveRestartAndSampleDecksDoNotResurrect() {
        val kv = InMemoryKeyValueStore()

        // First session: delete a bundled deck and add a custom one.
        val first = DeckRepository(deckStore = DeckStore(kv))
        val sampleCount = first.decks().size
        first.deleteDeck("kotlin")
        val custom = first.createDeck("Custom")
        first.addCard(custom.id, "q", "a")

        // Restart over the same storage.
        val restarted = DeckRepository(deckStore = DeckStore(kv))

        assertNull(restarted.deck("kotlin")) // deleted deck stays gone (no re-seed)
        assertEquals(sampleCount, restarted.decks().size) // -1 sample +1 custom
        assertEquals(1, restarted.deck(custom.id)!!.cards.size)
    }

    @Test
    fun addCardToMissingDeckReturnsNull() {
        val repo = DeckRepository(deckStore = DeckStore(InMemoryKeyValueStore()))
        assertNull(repo.addCard("nope", "f", "b"))
    }
}
