import SwiftUI

struct StickerDetailView: View {
    @EnvironmentObject private var library: StickerLibrary
    @Environment(\.dismiss) private var dismiss
    let stickerID: UUID
    @State private var editedLabel = ""
    @State private var errorMessage: String?

    private var sticker: StickerRecord? { library.stickers.first { $0.id == stickerID } }

    var body: some View {
        NavigationStack {
            Group {
                if let sticker {
                    Form {
                        Section {
                            if let image = library.thumbnail(for: sticker, maxPixels: 640) {
                                Image(uiImage: image)
                                    .resizable().scaledToFit()
                                    .frame(maxWidth: .infinity, minHeight: 180, maxHeight: 280)
                                    .accessibilityLabel(sticker.label.isEmpty ? sticker.displayName : sticker.label)
                            }
                            TextField("Search label", text: $editedLabel)
                                .onSubmit { saveLabel() }
                            Button(sticker.favorite ? "Remove favorite" : "Add favorite",
                                systemImage: sticker.favorite ? "heart.slash" : "heart") {
                                do { try library.setFavorite(stickerID, !sticker.favorite) }
                                catch { errorMessage = error.localizedDescription }
                            }
                        }
                        Section("Collections") {
                            if library.collections.isEmpty {
                                Text("Create collections from the library screen.")
                                    .foregroundStyle(.secondary)
                            }
                            ForEach(library.collections) { collection in
                                Toggle(collection.name, isOn: Binding(
                                    get: { sticker.collectionIDs.contains(collection.id) },
                                    set: { value in
                                        do { try library.setMembership(stickerID, collectionID: collection.id, included: value) }
                                        catch { errorMessage = error.localizedDescription }
                                    }))
                            }
                        }
                        Section {
                            if let url = library.fileURL(for: sticker) {
                                ShareLink(item: url, subject: Text("Sticker from ReaxKey")) {
                                    Label("Share sticker", systemImage: "square.and.arrow.up")
                                }
                                .simultaneousGesture(TapGesture().onEnded {
                                    try? library.recordUse(stickerID)
                                })
                            }
                            Button("Delete imported sticker", role: .destructive) {
                                do { try library.remove(stickerID); dismiss() }
                                catch { errorMessage = error.localizedDescription }
                            }
                        }
                    }
                } else {
                    ContentUnavailableView("Sticker unavailable", systemImage: "photo")
                }
            }
            .navigationTitle("Sticker")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { saveLabel(); dismiss() }
                }
            }
        }
        .onAppear { editedLabel = sticker?.label ?? "" }
        .alert("ReaxKey", isPresented: Binding(
            get: { errorMessage != nil }, set: { if !$0 { errorMessage = nil } })) {
            Button("OK") { errorMessage = nil }
        } message: { Text(errorMessage ?? "") }
    }

    private func saveLabel() {
        do { try library.setLabel(stickerID, editedLabel) }
        catch { errorMessage = error.localizedDescription }
    }
}
