import SwiftUI
import Shared

struct StudyView: View {
    @EnvironmentObject var store: Store
    let deckId: String

    @State private var queue: [FlashCard] = []
    @State private var index = 0
    @State private var revealed = false

    var body: some View {
        content
            .navigationTitle("Study")
            .navigationBarTitleDisplayMode(.inline)
            .onAppear {
                if queue.isEmpty {
                    queue = store.dueQueue(deckId: deckId)
                }
            }
    }

    @ViewBuilder
    private var content: some View {
        if queue.isEmpty {
            ContentUnavailableCompat(
                title: "Nothing due",
                systemImage: "checkmark.circle",
                message: "Come back later 🎉"
            )
        } else if index >= queue.count {
            VStack(spacing: 12) {
                Text("Session complete").font(.title2.bold())
                Text("\(queue.count) cards reviewed").foregroundStyle(.secondary)
            }
        } else {
            card(queue[index])
        }
    }

    private func card(_ card: FlashCard) -> some View {
        VStack(spacing: 20) {
            ProgressView(value: Double(index + 1), total: Double(queue.count))
            Text("Card \(index + 1) of \(queue.count)")
                .font(.caption)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)

            VStack(alignment: .leading, spacing: 16) {
                Text(card.front).font(.title2)
                if revealed {
                    Divider()
                    Text(card.back).font(.body)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
            .background(RoundedRectangle(cornerRadius: 16).fill(Color(.secondarySystemBackground)))

            Spacer()

            if revealed {
                HStack(spacing: 8) {
                    ForEach(Self.grades, id: \.label) { g in
                        Button(g.label) { answer(card: card, grade: g.grade) }
                            .buttonStyle(.borderedProminent)
                            .tint(g.tint)
                            .frame(maxWidth: .infinity)
                    }
                }
            } else {
                Button("Show answer") { revealed = true }
                    .buttonStyle(.borderedProminent)
                    .frame(maxWidth: .infinity)
            }
        }
        .padding()
    }

    private static let grades: [(label: String, grade: Grade, tint: Color)] = [
        ("Again", .again, .red),
        ("Hard", .hard, .orange),
        ("Good", .good, .blue),
        ("Easy", .easy, .green),
    ]

    private func answer(card: FlashCard, grade: Grade) {
        store.grade(cardId: card.id, grade: grade)
        index += 1
        revealed = false
    }
}

/// `ContentUnavailableView` needs iOS 17; this keeps the deployment target at 16.
private struct ContentUnavailableCompat: View {
    let title: String
    let systemImage: String
    let message: String

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: systemImage)
                .font(.largeTitle)
                .foregroundStyle(.secondary)
            Text(title).font(.title3.bold())
            Text(message).foregroundStyle(.secondary)
        }
    }
}
