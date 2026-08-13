package io.github.meko123456.barati.android

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.github.meko123456.barati.shared.data.DeckRepository
import io.github.meko123456.barati.shared.domain.Deck
import io.github.meko123456.barati.shared.domain.FlashCard
import io.github.meko123456.barati.shared.domain.Grade
import java.time.LocalDate

/** Thin Android wrapper over the shared [DeckRepository]. */
class BaratiViewModel : ViewModel() {

    private val repo = DeckRepository()

    /** Bumped after mutations so Compose recomputes derived values. */
    var version by mutableIntStateOf(0)
        private set

    private val today: Long get() = LocalDate.now().toEpochDay()

    fun deckSummaries(): List<Pair<Deck, Int>> =
        repo.decks().map { it to repo.dueCount(it.id, today) }

    fun dueQueue(deckId: String): List<FlashCard> = repo.dueCards(deckId, today)

    fun grade(cardId: String, grade: Grade) {
        repo.grade(cardId, grade, today)
        version++
    }
}
