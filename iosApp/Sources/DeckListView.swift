import SwiftUI
import Shared

/// Where a tap on the deck list can go: study the deck, or edit its cards.
enum DeckRoute: Hashable {
    case study(String)
    case edit(String)
}

struct DeckListView: View {
    @EnvironmentObject var store: Store
    @Environment(\.dynamicTypeSize) private var typeSize
    @State private var path = NavigationPath()
    @State private var addingDeck = false
    @State private var newDeckName = ""

    var body: some View {
        NavigationStack(path: $path) {
            List(store.deckSummaries(), id: \.deck.id) { item in
                let cards = "\(item.deck.cards.count) \(item.deck.cards.count == 1 ? "card" : "cards")"
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
                            Text("\(item.due) due")
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
                .accessibilityValue("\(cards), \(item.due > 0 ? "\(item.due) due" : "nothing due")")
                .swipeActions(edge: .trailing) {
                    Button(role: .destructive) { store.deleteDeck(item.deck.id) } label: {
                        Label("Delete", systemImage: "trash")
                    }
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
            .alert("New deck", isPresented: $addingDeck) {
                TextField("Deck name", text: $newDeckName)
                Button("Create") {
                    let name = newDeckName.trimmingCharacters(in: .whitespaces)
                    if !name.isEmpty { store.createDeck(name: name) }
                }
                Button("Cancel", role: .cancel) {}
            }
        }
    }
}
