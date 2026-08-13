package io.github.meko123456.barati.shared.domain

/** A single flashcard: a prompt ([front]) and its answer ([back]). */
data class FlashCard(
    val id: String,
    val front: String,
    val back: String,
)

/** A named collection of cards. */
data class Deck(
    val id: String,
    val name: String,
    val cards: List<FlashCard>,
)
