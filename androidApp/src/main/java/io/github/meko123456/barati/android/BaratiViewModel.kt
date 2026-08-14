package io.github.meko123456.barati.android

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import io.github.meko123456.barati.shared.data.DeckRepository
import io.github.meko123456.barati.shared.data.DeckStore
import io.github.meko123456.barati.shared.data.PrefsKeyValueStore
import io.github.meko123456.barati.shared.data.ReviewStore
import io.github.meko123456.barati.shared.domain.Deck
import io.github.meko123456.barati.shared.domain.FlashCard
import io.github.meko123456.barati.shared.domain.Grade
import java.time.LocalDate

/** Thin Android wrapper over the shared [DeckRepository]. */
class BaratiViewModel(app: Application) : AndroidViewModel(app) {

    private val kv = PrefsKeyValueStore(app)
    private val repo = DeckRepository(store = ReviewStore(kv), deckStore = DeckStore(kv))

    /** Bumped after mutations so Compose recomputes derived values. */
    var version by mutableIntStateOf(0)
        private set

    private val today: Long get() = LocalDate.now().toEpochDay()

    fun deckSummaries(): List<Pair<Deck, Int>> =
        repo.decks().map { it to repo.dueCount(it.id, today) }

    fun deck(deckId: String): Deck? = repo.deck(deckId)

    fun dueQueue(deckId: String): List<FlashCard> = repo.dueCards(deckId, today)

    fun grade(cardId: String, grade: Grade) {
        repo.grade(cardId, grade, today)
        version++
    }

    // --- Editing (each bumps version so the UI recomposes) ---------------------

    fun createDeck(name: String) { repo.createDeck(name); version++ }

    fun renameDeck(deckId: String, name: String) { repo.renameDeck(deckId, name); version++ }

    fun deleteDeck(deckId: String) { repo.deleteDeck(deckId); version++ }

    fun addCard(deckId: String, front: String, back: String) { repo.addCard(deckId, front, back); version++ }

    fun updateCard(deckId: String, cardId: String, front: String, back: String) {
        repo.updateCard(deckId, cardId, front, back); version++
    }

    fun deleteCard(deckId: String, cardId: String) { repo.deleteCard(deckId, cardId); version++ }
}
