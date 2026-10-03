import SwiftUI

@main
struct ReaxKeyApp: App {
    @StateObject private var library = StickerLibrary()

    var body: some Scene {
        WindowGroup {
            TabView {
                NavigationStack { LibraryView() }
                    .tabItem { Label("Stickers", systemImage: "square.grid.2x2") }
                NavigationStack { SettingsView() }
                    .tabItem { Label("Settings", systemImage: "gearshape") }
            }
            .environmentObject(library)
        }
    }
}
