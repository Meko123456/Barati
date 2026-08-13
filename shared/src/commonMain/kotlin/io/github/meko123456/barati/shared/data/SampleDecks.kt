package io.github.meko123456.barati.shared.data

import io.github.meko123456.barati.shared.domain.Deck
import io.github.meko123456.barati.shared.domain.FlashCard

/** Bundled starter decks so the app is useful on first launch. */
object SampleDecks {

    val all: List<Deck> = listOf(
        Deck(
            id = "kotlin",
            name = "Kotlin basics",
            cards = listOf(
                FlashCard("k1", "val vs var?", "val is read-only (assigned once); var is reassignable."),
                FlashCard("k2", "What does the ?. operator do?", "Safe call — returns null instead of throwing if the receiver is null."),
                FlashCard("k3", "What is a data class?", "A class that auto-generates equals, hashCode, toString, and copy from its properties."),
                FlashCard("k4", "What does 'suspend' mean?", "The function can pause and resume without blocking the thread."),
                FlashCard("k5", "sealed class?", "Restricts subclasses to the same module, so when-expressions can be exhaustive."),
            ),
        ),
        Deck(
            id = "geo",
            name = "Georgian phrases",
            cards = listOf(
                FlashCard("g1", "Hello", "გამარჯობა (gamarjoba)"),
                FlashCard("g2", "Thank you", "მადლობა (madloba)"),
                FlashCard("g3", "Yes / No", "კი / არა (ki / ara)"),
                FlashCard("g4", "Please", "თუ შეიძლება (tu sheidzleba)"),
                FlashCard("g5", "Goodbye", "ნახვამდის (nakhvamdis)"),
            ),
        ),
    )
}
