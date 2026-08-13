import SwiftUI

@main
struct BaratiApp: App {
    @StateObject private var store = Store()

    var body: some Scene {
        WindowGroup {
            DeckListView()
                .environmentObject(store)
        }
    }
}
