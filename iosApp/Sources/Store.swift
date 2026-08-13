import Foundation
import Shared

/// SwiftUI-facing wrapper around the shared Kotlin `DeckRepository`.
///
/// The domain logic (SM-2 scheduling, due-card selection) lives once in the
/// KMP `shared` module; this type only adapts it to `ObservableObject` so the
/// native SwiftUI views refresh after a card is graded.
@MainActor
final class Store: ObservableObject {
    private let repo = DeckRepository(initial: SampleDecks.shared.all)

    /// Bumped on every grade so observing views recompute their queues/counts.
    @Published private(set) var version = 0

    private var today: Int64 { Int64(Date().timeIntervalSince1970 / 86_400) }

    func deckSummaries() -> [(deck: Deck, due: Int)] {
        repo.decks().map { deck in
            (deck, Int(repo.dueCount(deckId: deck.id, today: today)))
        }
    }

    func dueQueue(deckId: String) -> [FlashCard] {
        repo.dueCards(deckId: deckId, today: today)
    }

    func grade(cardId: String, grade: Grade) {
        repo.grade(cardId: cardId, grade: grade, today: today)
        version += 1
    }
}
