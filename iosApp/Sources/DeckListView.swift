import SwiftUI
import Shared

/// Where a tap on the deck list can go: study the deck, or edit its cards.
enum DeckRoute: Hashable {
    case study(String)
    case edit(String)
}

struct DeckListView: View {
    @EnvironmentObject var store: Store
    @EnvironmentObject var quickActions: QuickActions
    @Environment(\.dynamicTypeSize) private var typeSize
    /// Typed rather than a NavigationPath, so a card from the Home Screen can start in the deck
    /// that is open.
    @State private var path: [DeckRoute] = []
    /// The Home Screen's Add a new card, while its sheet is up.
    @State private var newCard: NewCardRequest?
    @State private var addingDeck = false
    @State private var newDeckName = ""
    /// The deck a swipe asked to delete, held while the question is up.
    @State private var pendingDelete: PendingDelete?

    var body: some View {
        NavigationStack(path: $path) {
            List(store.deckSummaries(), id: \.deck.id) { item in
                // formatted() groups the digits as the phone's language does: "1,234 cards", not "1234".
                let cards = "\(item.deck.cards.count.formatted()) \(item.deck.cards.count == 1 ? "card" : "cards")"
                NavigationLink(value: DeckRoute.study(item.deck.id)) {
                    // At the accessibility text sizes the due count goes under the name. Beside it, it
                    // left the name a third of the row, and "Georgian phrases" broke as "Geor- gian
                    // phras es".
                    let stacked = typeSize.isAccessibilitySize
                    let layout = stacked
                        ? AnyLayout(VStackLayout(alignment: .leading, spacing: 4))
                        : AnyLayout(HStackLayout())
                    layout {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(item.deck.name).font(.headline)
                            Text(cards)
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                        if !stacked { Spacer() }
                        if item.due > 0 {
                            Text("\(item.due.formatted()) due")
                                .font(.headline)
                                .foregroundStyle(.tint)
                        } else {
                            Image(systemName: "checkmark")
                                .foregroundStyle(.secondary)
                        }
                    }
                    .padding(.vertical, 4)
                }
                .accessibilityIdentifier("deck-\(item.deck.name)")
                // One VoiceOver stop per deck, read as a button: "Kotlin basics, 5 cards, 3 due". The
                // row used to be a nameless button around three loose texts, and the checkmark shown
                // when nothing is due read "Selected". The counts are the value, so a test reads them
                // whole.
                .accessibilityLabel(item.deck.name)
                .accessibilityValue("\(cards), \(item.due > 0 ? "\(item.due.formatted()) due" : "nothing due")")
                .swipeActions(edge: .trailing) {
                    // Asks first, as the editor's trash button does: the swipe deleted the deck and
                    // every card in it at once, and a full swipe needs no tap at all. Tinted rather
                    // than given the destructive role, which takes the row out of the list before
                    // the answer is in.
                    Button {
                        pendingDelete = PendingDelete(id: item.deck.id, name: item.deck.name, cards: item.deck.cards.count)
                    } label: {
                        Label("Delete", systemImage: "trash")
                    }
                    .tint(.red)
                    .accessibilityIdentifier("deleteDeck-\(item.deck.name)")
                    Button { path.append(DeckRoute.edit(item.deck.id)) } label: {
                        Label("Edit", systemImage: "pencil")
                    }
                    .tint(.blue)
                    .accessibilityIdentifier("editDeck-\(item.deck.name)")
                }
            }
            .navigationTitle("Barati")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { newDeckName = ""; addingDeck = true } label: { Image(systemName: "plus") }
                        .accessibilityIdentifier("newDeck")
                        .accessibilityLabel("New deck")
                }
            }
            .navigationDestination(for: DeckRoute.self) { route in
                switch route {
                case .study(let id): StudyView(deckId: id)
                case .edit(let id): DeckEditView(deckId: id)
                }
            }
            .alert(
                "Delete \u{201C}\(pendingDelete?.name ?? "this deck")\u{201D}?",
                isPresented: Binding(get: { pendingDelete != nil }, set: { if !$0 { pendingDelete = nil } }),
                presenting: pendingDelete
            ) { pending in
                Button("Delete", role: .destructive) { store.deleteDeck(pending.id) }
                Button("Cancel", role: .cancel) {}
            } message: { pending in
                Text(deckDeletionMessage(cards: pending.cards))
            }
            .alert("New deck", isPresented: $addingDeck) {
                TextField("Deck name", text: $newDeckName)
                Button("Create") {
                    let name = newDeckName.trimmingCharacters(in: .whitespaces)
                    if !name.isEmpty { store.createDeck(name: name) }
                }
                Button("Cancel", role: .cancel) {}
            }
        }
        // On the stack, not the list, so it opens over a deck being studied or edited too.
        .sheet(item: $newCard) { request in
            NewCardView(openDeckId: request.openDeckId).environmentObject(store)
        }
        // The Home Screen's Add a new card: on launch, or later while the app is running.
        .onAppear(perform: takeQuickAction)
        .onChange(of: quickActions.pending) { _ in takeQuickAction() }
    }

    private func takeQuickAction() {
        guard quickActions.pending == .newCard else { return }
        quickActions.pending = nil
        let openDeck: String?
        switch path.last {
        case .study(let id), .edit(let id): openDeck = id
        case nil: openDeck = nil
        }
        newCard = NewCardRequest(openDeckId: openDeck)
    }
}

/// One opening of the New card sheet. Each has its own identity, so the action used while the
/// sheet is up starts a blank card.
private struct NewCardRequest: Identifiable {
    let id = UUID()
    let openDeckId: String?
}

/// A deck waiting on the delete question: enough of it to ask about it by name.
private struct PendingDelete {
    let id: String
    let name: String
    let cards: Int
}
