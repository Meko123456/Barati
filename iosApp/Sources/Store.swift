import Foundation
import Shared

/// SwiftUI-facing wrapper around the shared Kotlin `DeckRepository`.
///
/// The domain logic (SM-2 scheduling, due-card selection) and now the deck/card
/// editing + persistence live once in the KMP `shared` module; this type only
/// adapts it to `ObservableObject` so the native SwiftUI views refresh after a
/// mutation. Decks and reviews persist through the same native `UserDefaults`.
@MainActor
final class Store: ObservableObject {
    private let defaults = UserDefaultsKeyValueStore(defaults: .standard)
    private lazy var repo = DeckRepository(
        initial: SampleDecks.shared.all,
        store: ReviewStore(kv: defaults),
        deckStore: DeckStore(kv: defaults)
    )

    /// Bumped on every change so observing views recompute their queues/counts.
    @Published private(set) var version = 0

    private var today: Int64 { Int64(Date().timeIntervalSince1970 / 86_400) }

    func deckSummaries() -> [(deck: Deck, due: Int)] {
        repo.decks().map { deck in
            (deck, Int(repo.dueCount(deckId: deck.id, today: today)))
        }
    }

    func deck(_ deckId: String) -> Deck? { repo.deck(id: deckId) }

    func dueQueue(deckId: String) -> [FlashCard] {
        repo.dueCards(deckId: deckId, today: today)
    }

    func grade(cardId: String, grade: Grade) {
        repo.grade(cardId: cardId, grade: grade, today: today)
        version += 1
    }

    // --- Editing (each bumps version so observing views recompute) -------------

    func createDeck(name: String) { _ = repo.createDeck(name: name); version += 1 }
    func renameDeck(_ deckId: String, name: String) { repo.renameDeck(deckId: deckId, name: name); version += 1 }
    func deleteDeck(_ deckId: String) { repo.deleteDeck(deckId: deckId); version += 1 }
    func addCard(_ deckId: String, front: String, back: String) { _ = repo.addCard(deckId: deckId, front: front, back: back); version += 1 }
    func updateCard(_ deckId: String, cardId: String, front: String, back: String) { repo.updateCard(deckId: deckId, cardId: cardId, front: front, back: back); version += 1 }
    func deleteCard(_ deckId: String, cardId: String) { repo.deleteCard(deckId: deckId, cardId: cardId); version += 1 }
}
