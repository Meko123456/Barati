package io.github.meko123456.barati.shared.domain

import kotlinx.serialization.Serializable

/** A single flashcard: a prompt ([front]) and its answer ([back]). */
@Serializable
data class FlashCard(
    val id: String,
    val front: String,
    val back: String,
)

/** A named collection of cards. */
@Serializable
data class Deck(
    val id: String,
    val name: String,
    val cards: List<FlashCard>,
)
