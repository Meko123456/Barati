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
                        }
                        Spacer()
                        if item.due > 0 {
                            Text("\(item.due) due").font(.headline).foregroundStyle(.tint)
                        } else {
                            Image(systemName: "checkmark").foregroundStyle(.secondary)
                        }
                    }
                    .padding(.vertical, 4)
                }
                .swipeActions(edge: .trailing) {
                    Button(role: .destructive) { store.deleteDeck(item.deck.id) } label: {
                        Label("Delete", systemImage: "trash")
                    }
                    Button { path.append(DeckRoute.edit(item.deck.id)) } label: {
                        Label("Edit", systemImage: "pencil")
                    }
                    .tint(.blue)
                }
            }
            .navigationTitle("Barati")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button { newDeckName = ""; addingDeck = true } label: { Image(systemName: "plus") }
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
