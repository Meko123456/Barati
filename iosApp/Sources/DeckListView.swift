import SwiftUI
import Shared

struct DeckListView: View {
    @EnvironmentObject var store: Store

    var body: some View {
        NavigationStack {
            List(store.deckSummaries(), id: \.deck.id) { item in
                NavigationLink(value: item.deck.id) {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(item.deck.name)
                                .font(.headline)
                            Text("\(item.deck.cards.count) cards")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
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
            }
            .navigationTitle("Barati")
            .navigationDestination(for: String.self) { deckId in
                StudyView(deckId: deckId)
            }
        }
    }
}
