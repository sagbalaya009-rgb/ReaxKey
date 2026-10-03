import Foundation
import CryptoKit
import Combine
import ImageIO
import UniformTypeIdentifiers
import UIKit

struct StickerRecord: Identifiable, Codable, Hashable {
    let id: UUID
    let fileName: String
    var displayName: String
    let typeIdentifier: String
    var label: String
    var favorite: Bool
    var useCount: Int
    var lastUsed: Date?
    var collectionIDs: [UUID]
    let added: Date
}

struct StickerCollection: Identifiable, Codable, Hashable {
    let id: UUID
    var name: String
}

struct LibrarySnapshot: Codable {
    var stickers: [StickerRecord] = []
    var collections: [StickerCollection] = []
}

enum LibraryError: LocalizedError {
    case sharedContainerUnavailable
    case unsupportedSticker
    case tooLarge

    var errorDescription: String? {
        switch self {
        case .sharedContainerUnavailable: "ReaxKey's shared library is unavailable. Check the App Group setting."
        case .unsupportedSticker: "Choose a PNG, GIF or WebP sticker up to 1024 × 1024 pixels."
        case .tooLarge: "This sticker is larger than the 50 MB import limit."
        }
    }
}

@MainActor
final class StickerLibrary: ObservableObject {
    static let groupIdentifier = "group.com.reaxkey.ios"
    private static let maximumBytes = 50 * 1024 * 1024
    @Published private(set) var snapshot = LibrarySnapshot()
    let sharedContainerAvailable: Bool
    private let root: URL?
    private let thumbnails = NSCache<NSString, UIImage>()

    init() {
        root = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: Self.groupIdentifier)
        sharedContainerAvailable = root != nil
        if let dataURL = root?.appendingPathComponent("library.json"),
           let data = try? Data(contentsOf: dataURL),
           let decoded = try? JSONDecoder().decode(LibrarySnapshot.self, from: data) {
            snapshot = decoded
        }
        thumbnails.countLimit = 180
    }

    var stickers: [StickerRecord] { snapshot.stickers }
    var collections: [StickerCollection] { snapshot.collections }

    func fileURL(for sticker: StickerRecord) -> URL? {
        root?.appendingPathComponent("Stickers", isDirectory: true).appendingPathComponent(sticker.fileName)
    }

    func data(for sticker: StickerRecord) -> Data? {
        guard let url = fileURL(for: sticker) else { return nil }
        return try? Data(contentsOf: url)
    }

    func thumbnail(for sticker: StickerRecord, maxPixels: Int = 240) -> UIImage? {
        let key = "\(sticker.fileName)-\(maxPixels)" as NSString
        if let cached = thumbnails.object(forKey: key) { return cached }
        guard let url = fileURL(for: sticker),
              let source = CGImageSourceCreateWithURL(url as CFURL, nil) else { return nil }
        let options: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceThumbnailMaxPixelSize: maxPixels
        ]
        guard let image = CGImageSourceCreateThumbnailAtIndex(source, 0, options as CFDictionary) else { return nil }
        let thumbnail = UIImage(cgImage: image)
        thumbnails.setObject(thumbnail, forKey: key)
        return thumbnail
    }

    @discardableResult
    func importSticker(from externalURL: URL) throws -> Bool {
        guard let root else { throw LibraryError.sharedContainerUnavailable }
        let scoped = externalURL.startAccessingSecurityScopedResource()
        defer { if scoped { externalURL.stopAccessingSecurityScopedResource() } }
        let values = try externalURL.resourceValues(forKeys: [.fileSizeKey, .nameKey])
        guard (values.fileSize ?? 0) <= Self.maximumBytes else { throw LibraryError.tooLarge }
        let bytes = try Data(contentsOf: externalURL, options: .mappedIfSafe)
        guard bytes.count <= Self.maximumBytes else { throw LibraryError.tooLarge }
        guard let imageSource = CGImageSourceCreateWithData(bytes as CFData, nil),
              let type = CGImageSourceGetType(imageSource) as String?,
              let fileType = UTType(type),
              [UTType.png, .gif, .webP].contains(fileType),
              let properties = CGImageSourceCopyPropertiesAtIndex(imageSource, 0, nil) as? [CFString: Any],
              let width = properties[kCGImagePropertyPixelWidth] as? Int,
              let height = properties[kCGImagePropertyPixelHeight] as? Int,
              (1...1024).contains(width), (1...1024).contains(height),
              CGImageSourceCreateImageAtIndex(imageSource, 0, nil) != nil
        else { throw LibraryError.unsupportedSticker }

        let digest = SHA256.hash(data: bytes).map { String(format: "%02x", $0) }.joined()
        if snapshot.stickers.contains(where: { $0.fileName.hasPrefix(digest + ".") }) { return false }
        let extensionName = fileType == .png ? "png" : fileType == .gif ? "gif" : "webp"
        let directory = root.appendingPathComponent("Stickers", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true, attributes: nil)
        let name = digest + "." + extensionName
        let destination = directory.appendingPathComponent(name)
        try bytes.write(to: destination, options: .atomic)
        let title = (values.name ?? externalURL.lastPathComponent).trimmingCharacters(in: .whitespacesAndNewlines)
        snapshot.stickers.insert(StickerRecord(id: UUID(), fileName: name,
            displayName: String(title.prefix(120)), typeIdentifier: type, label: "",
            favorite: false, useCount: 0, lastUsed: nil, collectionIDs: [], added: Date()), at: 0)
        try persist()
        return true
    }

    func setFavorite(_ id: UUID, _ value: Bool) throws {
        guard let index = snapshot.stickers.firstIndex(where: { $0.id == id }) else { return }
        snapshot.stickers[index].favorite = value
        try persist()
    }

    func setLabel(_ id: UUID, _ label: String) throws {
        guard let index = snapshot.stickers.firstIndex(where: { $0.id == id }) else { return }
        snapshot.stickers[index].label = String(label.trimmingCharacters(in: .whitespacesAndNewlines).prefix(200))
        try persist()
    }

    func recordUse(_ id: UUID) throws {
        guard let index = snapshot.stickers.firstIndex(where: { $0.id == id }) else { return }
        snapshot.stickers[index].useCount += 1
        snapshot.stickers[index].lastUsed = Date()
        try persist()
    }

    func addCollection(_ name: String) throws {
        let trimmed = String(name.trimmingCharacters(in: .whitespacesAndNewlines).prefix(100))
        guard !trimmed.isEmpty else { return }
        snapshot.collections.append(StickerCollection(id: UUID(), name: trimmed))
        try persist()
    }

    func setMembership(_ stickerID: UUID, collectionID: UUID, included: Bool) throws {
        guard let index = snapshot.stickers.firstIndex(where: { $0.id == stickerID }),
              snapshot.collections.contains(where: { $0.id == collectionID }) else { return }
        snapshot.stickers[index].collectionIDs.removeAll { $0 == collectionID }
        if included { snapshot.stickers[index].collectionIDs.append(collectionID) }
        try persist()
    }

    func remove(_ id: UUID) throws {
        guard let index = snapshot.stickers.firstIndex(where: { $0.id == id }) else { return }
        let sticker = snapshot.stickers.remove(at: index)
        try persist()
        if let url = fileURL(for: sticker) { try? FileManager.default.removeItem(at: url) }
        thumbnails.removeAllObjects()
    }

    func reload() {
        guard let root,
              let data = try? Data(contentsOf: root.appendingPathComponent("library.json")),
              let decoded = try? JSONDecoder().decode(LibrarySnapshot.self, from: data) else { return }
        snapshot = decoded
    }

    private func persist() throws {
        guard let root else { throw LibraryError.sharedContainerUnavailable }
        let data = try JSONEncoder().encode(snapshot)
        try data.write(to: root.appendingPathComponent("library.json"), options: .atomic)
    }
}
