import SwiftUI
import Shared

struct StudyView: View {
    @EnvironmentObject var store: Store
    let deckId: String

    /// The shared sitting: a card graded Again comes round again before it ends.
    @State private var session: StudySession?
    /// Bumped after every grade. The session is a Kotlin object SwiftUI cannot watch, so the
    /// body reads this to know it has changed.
    @State private var step = 0
    @State private var revealed = false

    var body: some View {
        content
            .navigationTitle("Study")
            .navigationBarTitleDisplayMode(.inline)
            .onAppear {
                if session == nil {
                    session = StudySession(cards: store.dueQueue(deckId: deckId))
                }
            }
    }

    @ViewBuilder
    private var content: some View {
        let _ = step
        if let session {
            if session.total == 0 {
                ContentUnavailableCompat(
                    title: "Nothing due",
                    systemImage: "checkmark.circle",
                    message: "Come back later 🎉"
                )
            } else if let current = session.current {
                card(current, in: session)
            } else {
                VStack(spacing: 12) {
                    Text("Session complete").font(.title2.bold())
                    Text("\(session.total) cards reviewed").foregroundStyle(.secondary)
                }
            }
        } else {
            // Until onAppear builds the sitting, which needs the store from the environment. Not
            // an empty view: an empty view never appears, so onAppear would never run.
            Color.clear
        }
    }

    private func card(_ card: FlashCard, in session: StudySession) -> some View {
        let position = min(Int(session.reviewed) + 1, Int(session.total))
        return VStack(spacing: 20) {
            ProgressView(value: Double(position), total: Double(session.total))
            Text(session.isRepeat ? "Once more · \(session.remaining) left" : "Card \(position) of \(session.total)")
                .font(.caption)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityIdentifier("studyProgress")

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
                        Button(g.label) { answer(card: card, grade: g.grade, in: session) }
                            .buttonStyle(.borderedProminent)
                            .tint(g.tint)
                            .frame(maxWidth: .infinity)
                            .accessibilityIdentifier("grade-\(g.label)")
                    }
                }
            } else {
                Button("Show answer") { revealed = true }
                    .buttonStyle(.borderedProminent)
                    .frame(maxWidth: .infinity)
                    .accessibilityIdentifier("showAnswer")
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

    private func answer(card: FlashCard, grade: Grade, in session: StudySession) {
        // Only a card's first grade in the sitting is scheduled; a repeat is practice.
        if session.grade(grade: grade) {
            store.grade(cardId: card.id, grade: grade)
        }
        step += 1
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
