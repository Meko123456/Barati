import SwiftUI
import Shared

/// A card added from the Home Screen: the deck it goes into, since nothing in Barati was open to say
/// which, then its front and back. With no decks yet it names a new one. Android's add-card dialog
/// for the launcher shortcut asks the same things.
struct NewCardView: View {
    @EnvironmentObject var store: Store
    @Environment(\.dismiss) private var dismiss
    /// The deck being studied or edited when the action came, if any: where the card goes unless
    /// another is picked.
    let openDeckId: String?

    @State private var deckId: String?
    @State private var newDeckName = ""
    @State private var front = ""
    @State private var back = ""
    @FocusState private var focus: Field?

    private enum Field { case newDeck, front, back }

    private var decks: [Deck] { store.deckSummaries().map(\.deck) }

    private var canAdd: Bool {
        let named = deckId != nil || (decks.isEmpty && !newDeckName.trimmingCharacters(in: .whitespaces).isEmpty)
        return named &&
            !front.trimmingCharacters(in: .whitespaces).isEmpty &&
            !back.trimmingCharacters(in: .whitespaces).isEmpty
    }

    var body: some View {
        NavigationStack {
            Form {
                if decks.isEmpty {
                    Section {
                        TextField("New deck", text: $newDeckName)
                            .focused($focus, equals: .newDeck)
                            .submitLabel(.next)
                            .onSubmit { focus = .front }
                            .accessibilityIdentifier("newCardDeckName")
                    } footer: {
                        Text("There are no decks yet. This one is made with the card.")
                    }
                } else {
                    Section {
                        Picker("Deck", selection: $deckId) {
                            ForEach(decks, id: \.id) { deck in
                                Text(deck.name).tag(Optional(deck.id))
                            }
                        }
                        .accessibilityIdentifier("newCardDeck")
                    }
                }
                Section("Card") {
                    TextField("Front (prompt)", text: $front)
                        .focused($focus, equals: .front)
                        .submitLabel(.next)
                        .onSubmit { focus = .back }
                        .accessibilityIdentifier("newCardFront")
                    // Return adds the card, as Add does, and nothing while Add can't.
                    TextField("Back (answer)", text: $back)
                        .focused($focus, equals: .back)
                        .submitLabel(.done)
                        .onSubmit { if canAdd { add() } }
                        .accessibilityIdentifier("newCardBack")
                }
            }
            .navigationTitle("New card")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") { add() }
                        .disabled(!canAdd)
                        .accessibilityIdentifier("addNewCard")
                }
            }
        }
        .onAppear {
            // The open deck, else the one the last card from here went into, else the first.
            let ids = decks.map(\.id)
            deckId = [openDeckId, store.lastDeckForNewCard].compactMap { $0 }.first { ids.contains($0) } ?? ids.first
        }
        // Ready to type, as the Android dialog is.
        .task { focus = decks.isEmpty ? .newDeck : .front }
    }

    private func add() {
        let target = deckId ?? store.createDeck(name: newDeckName.trimmingCharacters(in: .whitespaces))
        store.addCard(target, front: front, back: back)
        store.lastDeckForNewCard = target
        dismiss()
    }
}
