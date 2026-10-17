import SwiftUI
import Shared

/// Native SwiftUI editor for one deck: add, edit and delete cards, and rename or
/// delete the deck itself. Mirrors the Android DeckEditScreen; all mutations go
/// through the shared `DeckRepository` so both platforms behave identically.
struct DeckEditView: View {
    @EnvironmentObject var store: Store
    @Environment(\.dismiss) private var dismiss
    let deckId: String

    @State private var showingAddCard = false
    @State private var showingRename = false
    @State private var editingCardId: String?
    @State private var front = ""
    @State private var back = ""
    @State private var deckName = ""
    @State private var confirmingDelete = false
    /// The card a swipe just deleted, while it can still be put back.
    @State private var lastDeleted: DeletedCard?

    var body: some View {
        let deck = store.deck(deckId)

        Group {
            if let deck, !deck.cards.isEmpty {
                List(deck.cards, id: \.id) { card in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(card.front).font(.headline)
                        Text(card.back).font(.subheadline).foregroundStyle(.secondary)
                    }
                    .swipeActions(edge: .trailing) {
                        // Deleted at once, and offered back below for a few seconds, as on Android.
                        // The swipe used to be the end of the card, schedule and all.
                        Button(role: .destructive) {
                            let index = deck.cards.firstIndex { $0.id == card.id } ?? 0
                            store.deleteCard(deckId, cardId: card.id)
                            withAnimation { lastDeleted = DeletedCard(card: card, index: index) }
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                        Button { startEditing(card) } label: { Label("Edit", systemImage: "pencil") }
                            .tint(.blue)
                    }
                }
            } else {
                ContentUnavailableCompat(
                    title: "No cards yet",
                    message: "Tap + to add your first card to this deck."
                )
            }
        }
        .overlay(alignment: .bottom) {
            if let deleted = lastDeleted {
                HStack {
                    Text("Card deleted")
                    Spacer()
                    Button("Undo") {
                        store.restoreCard(deckId, card: deleted.card, index: deleted.index)
                        withAnimation { lastDeleted = nil }
                    }
                    .bold()
                    .accessibilityIdentifier("undoCardDelete")
                }
                .padding()
                .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 12))
                .padding()
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .task(id: deleted.card.id) {
                    try? await Task.sleep(for: .seconds(6))
                    if lastDeleted?.card.id == deleted.card.id { withAnimation { lastDeleted = nil } }
                }
            }
        }
        .navigationTitle(deck?.name ?? "Deck")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItemGroup(placement: .navigationBarTrailing) {
                // Three glyphs in a row. The identifiers keep a UI test from picking the wrong one, and the
                // labels tell VoiceOver what each does: a bare glyph says nothing about the deck.
                Button { deckName = deck?.name ?? ""; showingRename = true } label: { Image(systemName: "pencil") }
                    .accessibilityIdentifier("renameDeck")
                    .accessibilityLabel("Rename deck")
                Button(role: .destructive) { confirmingDelete = true } label: { Image(systemName: "trash") }
                    .accessibilityIdentifier("deleteDeck")
                    .accessibilityLabel("Delete deck")
                Button { front = ""; back = ""; showingAddCard = true } label: { Image(systemName: "plus") }
                    .accessibilityIdentifier("addCard")
                    .accessibilityLabel("Add card")
            }
        }
        // Asks first. The whole deck, every card in it, went at one tap of a glyph next to Rename. An
        // alert, like this view's other prompts: as a confirmationDialog off the toolbar button, its
        // Cancel never became tappable in the UI test on iOS 26.
        .alert("Delete “\(deck?.name ?? "this deck")”?", isPresented: $confirmingDelete) {
            Button("Delete", role: .destructive) { store.deleteDeck(deckId); dismiss() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text(deckDeletionMessage(cards: deck?.cards.count ?? 0))
        }
        .alert("Add card", isPresented: $showingAddCard) {
            TextField("Front (prompt)", text: $front)
            TextField("Back (answer)", text: $back)
            Button("Add") { if canSaveCard { store.addCard(deckId, front: front, back: back) } }
            Button("Cancel", role: .cancel) {}
        }
        .alert("Edit card", isPresented: Binding(get: { editingCardId != nil }, set: { if !$0 { editingCardId = nil } })) {
            TextField("Front (prompt)", text: $front)
            TextField("Back (answer)", text: $back)
            Button("Save") {
                if let id = editingCardId, canSaveCard { store.updateCard(deckId, cardId: id, front: front, back: back) }
                editingCardId = nil
            }
            Button("Cancel", role: .cancel) { editingCardId = nil }
        }
        .alert("Rename deck", isPresented: $showingRename) {
            TextField("Deck name", text: $deckName)
            Button("Save") {
                let name = deckName.trimmingCharacters(in: .whitespaces)
                if !name.isEmpty { store.renameDeck(deckId, name: name) }
            }
            Button("Cancel", role: .cancel) {}
        }
    }

    private var canSaveCard: Bool {
        !front.trimmingCharacters(in: .whitespaces).isEmpty &&
            !back.trimmingCharacters(in: .whitespaces).isEmpty
    }

    private func startEditing(_ card: FlashCard) {
        front = card.front
        back = card.back
        editingCardId = card.id
    }
}

/// Small empty-state view (avoids depending on iOS 17's ContentUnavailableView).
private struct ContentUnavailableCompat: View {
    let title: String
    let message: String

    var body: some View {
        VStack(spacing: 8) {
            Text(title).font(.headline)
            Text(message).font(.subheadline).foregroundStyle(.secondary).multilineTextAlignment(.center)
        }
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

/// A card a swipe deleted, and where it stood, so Undo can put it back in its place.
private struct DeletedCard {
    let card: FlashCard
    let index: Int
}

/// What goes with a deck, said under both delete questions: the editor's and the list's swipe.
func deckDeletionMessage(cards: Int) -> String {
    cards == 0 ? "It has no cards yet." : cards == 1 ? "Its 1 card goes with it." : "Its \(cards) cards go with it."
}
