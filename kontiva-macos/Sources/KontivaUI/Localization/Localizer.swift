import SwiftUI
import KontivaCore

/// SwiftUI-friendly access to the localized string tables. Driven by the
/// selected `AppLanguage`; updating it re-renders all dependent views.
@MainActor
public final class Localizer: ObservableObject {
    @Published public private(set) var localization: Localization

    public init(language: AppLanguage = .deCH) {
        self.localization = Localization(language: language)
    }

    public var language: AppLanguage { localization.language }

    public func setLanguage(_ language: AppLanguage) {
        localization = Localization(language: language)
    }

    public func callAsFunction(_ key: L10nKey) -> String {
        localization.string(key)
    }

    public func string(_ key: L10nKey) -> String {
        localization.string(key)
    }
}
