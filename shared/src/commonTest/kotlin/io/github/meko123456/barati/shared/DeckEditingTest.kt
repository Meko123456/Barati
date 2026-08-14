package io.github.meko123456.barati.shared

import io.github.meko123456.barati.shared.data.DeckRepository
import io.github.meko123456.barati.shared.data.DeckStore
import io.github.meko123456.barati.shared.data.InMemoryKeyValueStore
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
