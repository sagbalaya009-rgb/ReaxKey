import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var library: StickerLibrary

    var body: some View {
        Form {
            Section("Set up keyboard") {
                Label("Open iPhone Settings → General → Keyboard → Keyboards → Add New Keyboard → ReaxKey.",
                    systemImage: "keyboard")
                Text("Normal typing works without Full Access. For the sticker copy action, iOS asks you to enable Allow Full Access for ReaxKey in Keyboard settings.")
                Text("ReaxKey does not send typed text to a server. Full Access is used only for the local sticker library and user-initiated copy action.")
            }
            Section("Stickers on iPhone") {
                Text("Import PNG, GIF or WebP stickers through Files in the Stickers tab. ReaxKey cannot read another app's private sticker library.")
                Text("When a receiving app supports image paste, choose a sticker in the ReaxKey keyboard, then paste it in that app. You can also share a sticker from its detail page.")
                Text("iOS does not give third-party keyboards Android's direct image commit API. Sending is app-dependent.")
            }
            Section("Your data") {
                LabeledContent("Imported stickers", value: "\(library.stickers.count)")
                LabeledContent("Collections", value: "\(library.collections.count)")
                Text("Stickers, labels, favorites and recents remain in ReaxKey's local App Group storage. Removing the app deletes its local library under normal iOS storage behavior.")
            }
            Section("About") {
                LabeledContent("Version", value: "1.0.0")
                Text("Your reactions. Everywhere.")
            }
        }
        .navigationTitle("Settings")
    }
}
