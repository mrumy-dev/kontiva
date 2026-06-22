import SwiftUI
import KontivaCore

/// Native menu-bar commands and keyboard shortcuts. The model is owned by the app
/// so these can drive the same instance the windows use.
///
/// - File: Add entry (⌘N), Export report (⌘E), Lock (⌘L)
/// - View: jump to a section (⌘1…⌘6)
public struct KontivaCommands: Commands {
    private let model: AppModel
    public init(model: AppModel) { self.model = model }

    private var loc: Localization { model.localizer.localization }

    public var body: some Commands {
        CommandGroup(replacing: .newItem) {
            Button(loc.string(.commandsAdd)) { model.newItemSignal &+= 1 }
                .keyboardShortcut("n")
            Button(loc.string(.exportReport)) { model.exportSignal &+= 1 }
                .keyboardShortcut("e")
            Divider()
            Button(loc.string(.lockTitle)) { Task { await model.lock() } }
                .keyboardShortcut("l")
        }

        CommandGroup(after: .sidebar) {
            Divider()
            Button(loc.string(.navOverview)) { model.selection = .overview }.keyboardShortcut("1")
            Button(loc.string(.navInsights)) { model.selection = .insights }.keyboardShortcut("2")
            Button(loc.string(.navPlanning)) { model.selection = .planning }.keyboardShortcut("3")
            Button(loc.string(.navSparen))   { model.selection = .sparen }.keyboardShortcut("4")
            Button(loc.string(.navBills))    { model.selection = .bills }.keyboardShortcut("5")
            Button(loc.string(.navSchulden)) { model.selection = .schulden }.keyboardShortcut("6")
            Button(loc.string(.navReport))   { model.selection = .report }.keyboardShortcut("7")
        }
    }
}
