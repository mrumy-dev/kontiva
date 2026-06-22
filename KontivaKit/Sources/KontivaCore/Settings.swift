import Foundation

/// Region grouping for the language picker: the official Swiss languages first,
/// then European, then Asian & Middle East.
public enum LanguageGroup: Int, Sendable, CaseIterable {
    case swiss, european, asian
}

/// Supported UI languages. de-CH is the default. Declaration order = picker order.
public enum AppLanguage: String, Codable, Sendable, CaseIterable, Identifiable {
    public var id: String { rawValue }

    // Swiss (official national languages + English)
    case deCH = "de-CH"
    case frCH = "fr-CH"
    case itCH = "it-CH"
    case rm
    case en

    // European
    case es
    case ptPT = "pt-PT"
    case ptBR = "pt-BR"
    case nl
    case da
    case nb
    case sv
    case fi
    case pl
    case ro
    case hu
    case sq
    case sr
    case hr
    case bs
    case mk
    case tr
    case ru
    case uk

    // Asian & Middle East
    case ar
    case zhHans = "zh-Hans"
    case ja
    case ko
    case vi
    case th
    case hi
    case ta
    case si
    case ur
    case ps

    /// Endonym shown in the language picker.
    public var displayName: String {
        switch self {
        case .deCH: return "Deutsch (Schweiz)"
        case .frCH: return "Français (Suisse)"
        case .itCH: return "Italiano (Svizzera)"
        case .rm:   return "Rumantsch"
        case .en:   return "English"
        case .es:   return "Español"
        case .ptPT: return "Português"
        case .ptBR: return "Português (Brasil)"
        case .nl:   return "Nederlands"
        case .da:   return "Dansk"
        case .nb:   return "Norsk"
        case .sv:   return "Svenska"
        case .fi:   return "Suomi"
        case .pl:   return "Polski"
        case .ro:   return "Română"
        case .hu:   return "Magyar"
        case .sq:   return "Shqip"
        case .sr:   return "Srpski"
        case .hr:   return "Hrvatski"
        case .bs:   return "Bosanski"
        case .mk:   return "Македонски"
        case .tr:   return "Türkçe"
        case .ru:   return "Русский"
        case .uk:   return "Українська"
        case .ar:   return "العربية"
        case .zhHans: return "简体中文"
        case .ja:   return "日本語"
        case .ko:   return "한국어"
        case .vi:   return "Tiếng Việt"
        case .th:   return "ไทย"
        case .hi:   return "हिन्दी"
        case .ta:   return "தமிழ்"
        case .si:   return "සිංහල"
        case .ur:   return "اردو"
        case .ps:   return "پښتو"
        }
    }

    /// Foundation locale used for date/number formatting.
    public var locale: Locale {
        let id: String
        switch self {
        case .deCH: id = "de_CH"
        case .frCH: id = "fr_CH"
        case .itCH: id = "it_CH"
        case .rm:   id = "rm_CH"
        case .en:   id = "en_CH"   // English UI, Swiss conventions
        case .es:   id = "es_ES"
        case .ptPT: id = "pt_PT"
        case .ptBR: id = "pt_BR"
        case .nl:   id = "nl_NL"
        case .da:   id = "da_DK"
        case .nb:   id = "nb_NO"
        case .sv:   id = "sv_SE"
        case .fi:   id = "fi_FI"
        case .pl:   id = "pl_PL"
        case .ro:   id = "ro_RO"
        case .hu:   id = "hu_HU"
        case .sq:   id = "sq_AL"
        case .sr:   id = "sr_Latn_RS"
        case .hr:   id = "hr_HR"
        case .bs:   id = "bs_BA"
        case .mk:   id = "mk_MK"
        case .tr:   id = "tr_TR"
        case .ru:   id = "ru_RU"
        case .uk:   id = "uk_UA"
        case .ar:   id = "ar"
        case .zhHans: id = "zh_Hans"
        case .ja:   id = "ja_JP"
        case .ko:   id = "ko_KR"
        case .vi:   id = "vi_VN"
        case .th:   id = "th_TH"
        case .hi:   id = "hi_IN"
        case .ta:   id = "ta_IN"
        case .si:   id = "si_LK"
        case .ur:   id = "ur_PK"
        case .ps:   id = "ps_AF"
        }
        return Locale(identifier: id)
    }

    /// Right-to-left scripts (Arabic, Urdu, Pashto) — drives layout direction.
    public var isRTL: Bool {
        switch self {
        case .ar, .ur, .ps: return true
        default: return false
        }
    }

    /// Picker section.
    public var group: LanguageGroup {
        switch self {
        case .deCH, .frCH, .itCH, .rm, .en: return .swiss
        case .ar, .zhHans, .ja, .ko, .vi, .th, .hi, .ta, .si, .ur, .ps: return .asian
        default: return .european
        }
    }
}

public enum AppAppearance: String, Codable, Sendable, CaseIterable {
    case system, light, dark
}

/// User-selectable accent colour. The accent recolours interactive and brand UI
/// (selection, primary buttons, category icons, progress rings). Danger semantics
/// — negative balances, overdue bills, errors — always stay red regardless of the
/// chosen theme, and the logo remains Swiss red. The actual `Color` for each case
/// is defined in the UI layer (see `AccentTheme.color`).
public enum AccentTheme: String, Codable, Sendable, CaseIterable, Identifiable {
    case swissRed   // brand default
    case orange
    case sand       // warm beige/bronze
    case green
    case teal
    case blue
    case purple
    case pink

    public var id: String { rawValue }

    /// Localized display name for the settings picker.
    public var labelKey: L10nKey {
        switch self {
        case .swissRed: return .themeSwissRed
        case .orange:   return .themeOrange
        case .sand:     return .themeSand
        case .green:    return .themeGreen
        case .teal:     return .themeTeal
        case .blue:     return .themeBlue
        case .purple:   return .themePurple
        case .pink:     return .themePink
        }
    }
}

/// Non-secret application settings.
public struct AppSettings: Equatable, Codable, Sendable {
    public var language: AppLanguage
    public var appearance: AppAppearance
    public var accent: AccentTheme

    // Follows the system appearance by default (fluid light/dark).
    public init(language: AppLanguage = .deCH, appearance: AppAppearance = .system,
                accent: AccentTheme = .swissRed) {
        self.language = language
        self.appearance = appearance
        self.accent = accent
    }
}

/// Auto-lock interval (idle time before the app re-locks).
public enum AutoLockInterval: String, Codable, Sendable, CaseIterable {
    case oneMinute, fiveMinutes, fifteenMinutes, never

    public var seconds: TimeInterval? {
        switch self {
        case .oneMinute:     return 60
        case .fiveMinutes:   return 5 * 60
        case .fifteenMinutes:return 15 * 60
        case .never:         return nil
        }
    }

    /// Compact, language-neutral display label (Swiss "Min." abbreviation).
    public var displayLabel: String {
        switch self {
        case .oneMinute:     return "1 Min."
        case .fiveMinutes:   return "5 Min."
        case .fifteenMinutes:return "15 Min."
        case .never:         return "—"
        }
    }
}

/// Security configuration. Contains **no secrets** — only parameters and state.
/// Real key material lives in the (later) KontivaSecurity layer, only ever wrapped.
public struct SecuritySettings: Equatable, Codable, Sendable {
    public var autoLock: AutoLockInterval
    public var hasPassphrase: Bool
    public var recoveryWarningAcknowledged: Bool

    public init(autoLock: AutoLockInterval = .fiveMinutes,
                hasPassphrase: Bool = false,
                recoveryWarningAcknowledged: Bool = false) {
        self.autoLock = autoLock
        self.hasPassphrase = hasPassphrase
        self.recoveryWarningAcknowledged = recoveryWarningAcknowledged
    }
}

/// Metadata about an encrypted backup (no secrets, no private values).
public struct BackupMetadata: Equatable, Codable, Sendable {
    public let createdAt: Date
    public let appVersion: String
    public let kdfIdentifier: String
    public let cipherIdentifier: String
    public let itemCounts: [String: Int]

    public init(createdAt: Date, appVersion: String, kdfIdentifier: String,
                cipherIdentifier: String, itemCounts: [String: Int]) {
        self.createdAt = createdAt
        self.appVersion = appVersion
        self.kdfIdentifier = kdfIdentifier
        self.cipherIdentifier = cipherIdentifier
        self.itemCounts = itemCounts
    }
}
