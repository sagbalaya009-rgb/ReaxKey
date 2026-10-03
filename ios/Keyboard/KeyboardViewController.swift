import UIKit
import UniformTypeIdentifiers

final class KeyboardViewController: UIInputViewController, UITextFieldDelegate {
    private let library = StickerLibrary()
    private let root = UIStackView()
    private let message = UILabel()
    private var searchField: UITextField?
    private var mediaGrid: UIStackView?
    private var searchQuery = ""
    private var mediaMode = false
    private var symbols = false
    private var shift = true
    private var capsLocked = false
    private var lastShiftTap = Date.distantPast
    private var languageIndex = 0
    private let languages = ["EN", "FR", "ES", "YO"]

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor { trait in
            trait.userInterfaceStyle == .dark ? UIColor(red: 0.06, green: 0.07, blue: 0.09, alpha: 1) :
                UIColor(red: 0.91, green: 0.93, blue: 0.96, alpha: 1)
        }
        root.axis = .vertical
        root.spacing = 4
        root.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(root)
        NSLayoutConstraint.activate([
            root.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 4),
            root.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -4),
            root.topAnchor.constraint(equalTo: view.topAnchor, constant: 4),
            root.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -4),
            view.heightAnchor.constraint(greaterThanOrEqualToConstant: 268)
        ])
        render()
    }

    override func textDidChange(_ textInput: UITextInput?) {
        super.textDidChange(textInput)
        if !mediaMode && !symbols && !capsLocked {
            shift = textDocumentProxy.documentContextBeforeInput?.isEmpty ?? true
            render()
        }
    }

    private func render() {
        root.arrangedSubviews.forEach { $0.removeFromSuperview() }
        if mediaMode { renderMedia(); return }
        let toolbar = row()
        toolbar.addArrangedSubview(key("🌐", accessibility: "Next keyboard") { [weak self] in
            self?.advanceToNextInputMode()
        })
        toolbar.addArrangedSubview(key(languages[languageIndex], accessibility: "Change language") { [weak self] in
            guard let self else { return }
            self.languageIndex = (self.languageIndex + 1) % self.languages.count
            self.render()
        })
        toolbar.addArrangedSubview(key("Stickers", accessibility: "Stickers") { [weak self] in
            self?.mediaMode = true
            self?.library.reload()
            self?.render()
        })
        root.addArrangedSubview(toolbar)

        let lines = symbols
            ? ["1234567890", "@#$%&-+()/", "?!':;.,\""]
            : ["qwertyuiop", "asdfghjkl", "zxcvbnm"]
        for (index, line) in lines.enumerated() {
            let keys = row()
            if index == 2 && !symbols {
                keys.addArrangedSubview(key(capsLocked ? "⇪" : shift ? "⬆" : "⇧",
                    accessibility: "Shift") { [weak self] in self?.changeShift() })
            }
            for character in line {
                let value = String(character)
                keys.addArrangedSubview(key(!symbols && (shift || capsLocked) ? value.uppercased() : value) { [weak self] in
                    guard let self else { return }
                    self.textDocumentProxy.insertText(!self.symbols && (self.shift || self.capsLocked)
                        ? value.uppercased() : value)
                    if self.shift && !self.capsLocked { self.shift = false; self.render() }
                })
            }
            if index == 2 {
                keys.addArrangedSubview(key("⌫", accessibility: "Delete") { [weak self] in
                    self?.textDocumentProxy.deleteBackward()
                })
            }
            root.addArrangedSubview(keys)
        }
        let bottom = row()
        bottom.addArrangedSubview(key(symbols ? "ABC" : "?123", accessibility: "Symbols") { [weak self] in
            guard let self else { return }
            self.symbols.toggle()
            self.render()
        })
        bottom.addArrangedSubview(key(",", accessibility: "Comma") { [weak self] in
            self?.textDocumentProxy.insertText(",")
        })
        bottom.addArrangedSubview(key("space", accessibility: "Space") { [weak self] in
            self?.textDocumentProxy.insertText(" ")
        })
        bottom.addArrangedSubview(key(".", accessibility: "Period") { [weak self] in
            self?.textDocumentProxy.insertText(".")
        })
        bottom.addArrangedSubview(key("return", accessibility: "Return") { [weak self] in
            self?.textDocumentProxy.insertText("\n")
        })
        root.addArrangedSubview(bottom)
    }

    private func changeShift() {
        let now = Date()
        if now.timeIntervalSince(lastShiftTap) < 0.4 {
            capsLocked = !capsLocked
            shift = capsLocked
        } else {
            capsLocked = false
            shift.toggle()
        }
        lastShiftTap = now
        render()
    }

    private func renderMedia() {
        let toolbar = row()
        toolbar.addArrangedSubview(key("ABC", accessibility: "Return to typing") { [weak self] in
            self?.mediaMode = false
            self?.render()
        })
        toolbar.addArrangedSubview(key("Refresh", accessibility: "Refresh stickers") { [weak self] in
            self?.library.reload()
            self?.render()
        })
        root.addArrangedSubview(toolbar)

        let search = UITextField()
        search.borderStyle = .roundedRect
        search.placeholder = "Search stickers"
        search.accessibilityLabel = "Search stickers"
        search.autocorrectionType = .no
        search.delegate = self
        search.addTarget(self, action: #selector(searchChanged), for: .editingChanged)
        search.text = searchQuery
        searchField = search
        root.addArrangedSubview(search)

        message.font = .preferredFont(forTextStyle: .caption1)
        message.textColor = .secondaryLabel
        message.numberOfLines = 2
        message.text = hasFullAccess
            ? "Tap a sticker to copy it, then paste in the app if supported."
            : "Typing works without Full Access. Enable it in iPhone Keyboard settings to copy stickers."
        root.addArrangedSubview(message)

        let scroll = UIScrollView()
        let grid = UIStackView()
        grid.axis = .vertical
        grid.spacing = 3
        mediaGrid = grid
        grid.translatesAutoresizingMaskIntoConstraints = false
        scroll.addSubview(grid)
        NSLayoutConstraint.activate([
            grid.leadingAnchor.constraint(equalTo: scroll.leadingAnchor),
            grid.trailingAnchor.constraint(equalTo: scroll.trailingAnchor),
            grid.topAnchor.constraint(equalTo: scroll.topAnchor),
            grid.bottomAnchor.constraint(equalTo: scroll.bottomAnchor),
            grid.widthAnchor.constraint(equalTo: scroll.widthAnchor)
        ])
        populateMediaGrid()
        root.addArrangedSubview(scroll)
    }

    private func populateMediaGrid() {
        guard let grid = mediaGrid else { return }
        grid.arrangedSubviews.forEach { $0.removeFromSuperview() }
        let query = searchQuery
        let matches = library.stickers.filter {
            query.isEmpty || $0.displayName.localizedCaseInsensitiveContains(query)
                || $0.label.localizedCaseInsensitiveContains(query)
        }
        if matches.isEmpty {
            let empty = UILabel()
            empty.text = library.stickers.isEmpty ? "Import stickers in the ReaxKey app." : "No matches."
            empty.textAlignment = .center
            empty.textColor = .secondaryLabel
            grid.addArrangedSubview(empty)
        } else {
            for start in stride(from: 0, to: min(matches.count, 120), by: 5) {
                let line = row()
                for sticker in matches[start..<min(start + 5, matches.count)] {
                    let button = key("", accessibility: sticker.label.isEmpty ? sticker.displayName : sticker.label) { [weak self] in
                        self?.copySticker(sticker)
                    }
                    if let thumbnail = library.thumbnail(for: sticker, maxPixels: 120) {
                        button.setImage(thumbnail.withRenderingMode(.alwaysOriginal), for: .normal)
                        button.imageView?.contentMode = .scaleAspectFit
                    } else {
                        button.setTitle("□", for: .normal)
                    }
                    line.addArrangedSubview(button)
                }
                for _ in line.arrangedSubviews.count..<5 {
                    let spacer = UIView()
                    line.addArrangedSubview(spacer)
                }
                grid.addArrangedSubview(line)
            }
        }
    }

    @objc private func searchChanged() {
        searchQuery = searchField?.text ?? ""
        populateMediaGrid()
    }

    private func copySticker(_ sticker: StickerRecord) {
        guard hasFullAccess else {
            message.text = "Enable Allow Full Access for ReaxKey in iPhone Keyboard settings."
            return
        }
        guard let data = library.data(for: sticker) else {
            message.text = "Sticker unavailable. Reopen ReaxKey and import it again."
            return
        }
        UIPasteboard.general.setData(data, forPasteboardType: sticker.typeIdentifier)
        try? library.recordUse(sticker.id)
        message.text = "Copied. Paste it into the current app if image paste is supported."
    }

    private func row() -> UIStackView {
        let stack = UIStackView()
        stack.axis = .horizontal
        stack.spacing = 3
        stack.distribution = .fillEqually
        return stack
    }

    private func key(_ title: String, accessibility: String? = nil, action: @escaping () -> Void) -> UIButton {
        let button = UIButton(type: .system)
        button.setTitle(title, for: .normal)
        button.accessibilityLabel = accessibility ?? title
        button.titleLabel?.font = .systemFont(ofSize: title.count > 4 ? 14 : 20, weight: .medium)
        button.backgroundColor = .secondarySystemBackground
        button.layer.cornerRadius = 6
        button.heightAnchor.constraint(equalToConstant: 47).isActive = true
        button.addAction(UIAction { _ in action() }, for: .touchUpInside)
        return button
    }
}
