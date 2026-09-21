import SwiftUI
import Shared

/// Where a tap on the deck list can go: study the deck, or edit its cards.
enum DeckRoute: Hashable {
    case study(String)
    case edit(String)
}

struct DeckListView: View {
    @EnvironmentObject var store: Store
    @State private var path = NavigationPath()
    @State private var addingDeck = false
    @State private var newDeckName = ""

    var body: some View {
        NavigationStack(path: $path) {
            List(store.deckSummaries(), id: \.deck.id) { item in
                NavigationLink(value: DeckRoute.study(item.deck.id)) {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(item.deck.name).font(.headline)
                            Text("\(item.deck.cards.count) \(item.deck.cards.count == 1 ? "card" : "cards")")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                                .accessibilityIdentifier("cards-\(item.deck.name)")
                        }
                        Spacer()
                        if item.due > 0 {
                            Text("\(item.due) due")
                                .font(.headline)
                                .foregroundStyle(.tint)
                                .accessibilityIdentifier("due-\(item.deck.name)")
                        } else {
                            Image(systemName: "checkmark")
                                .foregroundStyle(.secondary)
                                .accessibilityIdentifier("nothingDue-\(item.deck.name)")
                        }
                    }
                    .padding(.vertical, 4)
                    // Keeps the counts addressable one by one. A NavigationLink is a button, and a
                    // button merges its children into a single element whose label is everything
                    // inside it concatenated — so without this the due count can only be asserted
                    // as a substring of the whole row.
                    .accessibilityElement(children: .contain)
                }
                .accessibilityIdentifier("deck-\(item.deck.name)")
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
