import SwiftUI
import UniformTypeIdentifiers

private enum LibraryFilter: Hashable {
    case all, favorites, recent, collection(UUID)
}

struct LibraryView: View {
    @EnvironmentObject private var library: StickerLibrary
    @State private var search = ""
    @State private var filter: LibraryFilter = .all
    @State private var importing = false
    @State private var selected: StickerRecord?
    @State private var showingNewCollection = false
    @State private var newCollectionName = ""
    @State private var notice: String?

    private var visible: [StickerRecord] {
        var items = library.stickers
        switch filter {
        case .all: break
        case .favorites: items = items.filter(\.favorite)
        case .recent: items = items.filter { $0.lastUsed != nil }
        case .collection(let id): items = items.filter { $0.collectionIDs.contains(id) }
        }
        if !search.isEmpty {
            items = items.filter {
                $0.displayName.localizedCaseInsensitiveContains(search) ||
                $0.label.localizedCaseInsensitiveContains(search)
            }
        }
        if filter == .recent { items.sort { ($0.lastUsed ?? .distantPast) > ($1.lastUsed ?? .distantPast) } }
        return items
    }

    var body: some View {
        VStack(spacing: 0) {
            if !library.sharedContainerAvailable {
                ContentUnavailableView("Shared library unavailable",
                    systemImage: "externaldrive.badge.exclamationmark",
                    description: Text("The App Group must be enabled for both ReaxKey targets before device testing."))
            } else {
                filterBar
                if visible.isEmpty {
                    ContentUnavailableView(library.stickers.isEmpty ? "No stickers yet" : "No matches",
                        systemImage: "face.smiling",
                        description: Text(library.stickers.isEmpty
                            ? "Import your own PNG, GIF or WebP stickers with the Files picker."
                            : "Try another search or filter."))
                } else {
                    ScrollView {
                        LazyVGrid(columns: [GridItem(.adaptive(minimum: 84), spacing: 10)], spacing: 10) {
                            ForEach(visible) { sticker in
                                Button { selected = sticker } label: {
                                    StickerTile(sticker: sticker)
                                }
                                .buttonStyle(.plain)
                                .accessibilityLabel(sticker.label.isEmpty ? sticker.displayName : sticker.label)
                            }
                        }
                        .padding()
                    }
                }
            }
        }
        .navigationTitle("ReaxKey")
        .searchable(text: $search, prompt: "Search your stickers")
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Menu {
                    Button("New collection", systemImage: "folder.badge.plus") { showingNewCollection = true }
                    ForEach(library.collections) { collection in
                        Button(collection.name) { filter = .collection(collection.id) }
                    }
                } label: { Image(systemName: "folder") }
                .accessibilityLabel("Collections")
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button("Import stickers", systemImage: "plus") { importing = true }
                    .disabled(!library.sharedContainerAvailable)
            }
        }
        .fileImporter(isPresented: $importing, allowedContentTypes: [.png, .gif, .webP],
            allowsMultipleSelection: true) { result in
            switch result {
            case .success(let urls):
                do {
                    var added = 0
                    for url in urls { if try library.importSticker(from: url) { added += 1 } }
                    notice = "Added \(added) stickers. Duplicates were skipped."
                } catch { notice = error.localizedDescription }
            case .failure(let error): notice = error.localizedDescription
            }
        }
        .sheet(item: $selected) { sticker in StickerDetailView(stickerID: sticker.id) }
        .alert("New collection", isPresented: $showingNewCollection) {
            TextField("Collection name", text: $newCollectionName)
            Button("Create") {
                do { try library.addCollection(newCollectionName); newCollectionName = "" }
                catch { notice = error.localizedDescription }
            }
            Button("Cancel", role: .cancel) { newCollectionName = "" }
        }
        .alert("ReaxKey", isPresented: Binding(
            get: { notice != nil }, set: { if !$0 { notice = nil } })) {
            Button("OK") { notice = nil }
        } message: { Text(notice ?? "") }
    }

    private var filterBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack {
                filterButton("All", .all)
                filterButton("Favorites", .favorites)
                filterButton("Recent", .recent)
                ForEach(library.collections) { collection in
                    filterButton(collection.name, .collection(collection.id))
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 8)
        }
    }

    private func filterButton(_ title: String, _ value: LibraryFilter) -> some View {
        Button(title) { filter = value }
            .buttonStyle(.bordered)
            .tint(filter == value ? .indigo : .secondary)
            .accessibilityAddTraits(filter == value ? .isSelected : [])
    }
}

struct StickerTile: View {
    @EnvironmentObject private var library: StickerLibrary
    let sticker: StickerRecord

    var body: some View {
        ZStack(alignment: .topTrailing) {
            RoundedRectangle(cornerRadius: 14).fill(.thinMaterial)
            if let thumbnail = library.thumbnail(for: sticker) {
                Image(uiImage: thumbnail).resizable().scaledToFit().padding(6)
            } else {
                Image(systemName: "photo").foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            if sticker.favorite {
                Image(systemName: "heart.fill")
                    .font(.caption).foregroundStyle(.pink).padding(6)
            }
        }
        .frame(height: 94)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}
