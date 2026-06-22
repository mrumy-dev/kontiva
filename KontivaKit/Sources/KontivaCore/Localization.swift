import Foundation

/// Every user-facing string key in Kontiva. Using an enum (rather than loose
/// `.strings` files) makes **exact key parity** across all four languages a
/// compile-and-test guarantee: each language table must provide a non-empty
/// value for every case, verified by `LocalizationParityTests`.
public enum L10nKey: String, CaseIterable, Sendable {
    // App / brand
    case appName
    case appTagline
    case notBetaNotice

    // Navigation
    case navOverview
    case navPlanning
    case navSparen
    case navBills
    case navSettings
    case navReport
    case navInsights

    // Sparen (savings)
    case sparenSubtitle
    case sparenEmpty
    case sparenMonthlyTotal
    case sparenAccumulatedTotal
    case sparenPerMonth
    case sparenContributions
    case sparenSince
    case sparenOpenEnded
    case sparenGoalsLabel
    case savingsCatCar
    case savingsCatVacation
    case savingsCatHome
    case savingsCatRetirement
    case savingsCatEmergency
    case savingsCatEducation
    case savingsCatOther
    case savingsCatWedding
    case savingsCatFamily
    case savingsCatRenovation
    case savingsCatElectronics
    case savingsCatTaxes
    case savingsCatInvestment
    case savingsCatHealth
    case savingsCatGift

    // Insights
    case insightsSubtitle
    case insightOverspending
    case insightTightBudget
    case insightHealthySurplus
    case insightHighFixed
    case insightHighHousing
    case insightLargestFixed
    case insightLargestVariable
    case insightOverdue
    case insightNoSavings
    case insightGoodSavings
    case insightAllHealthy
    case insightHousingHint
    case insightNoSavingsDetail
    case insightAllHealthyDetail
    case fragOfNetIncome
    case fragOfFixedCosts
    case fragShortfall

    // Common
    case commonAdd
    case commonEdit
    case commonDelete
    case commonCancel
    case commonSave
    case commonClose
    case commonDone
    case commonShowSampleData
    case commonHideSampleData
    case commonEmptyHint
    case commonCopy
    case commonOptions
    case commonMoveUp
    case commonMoveDown
    case monthToday

    // Forms (add/edit)
    case formName
    case formAmount
    case formStatus
    case formNotes
    case formInvalidAmount
    case formThirteenthAmount
    case formThirteenthModel
    case thirteenthModelSeparate
    case thirteenthModelAveraged
    case formTarget
    case formSaved
    case formStartDate
    case formStartingBalance
    case formMonthlyContribution
    case formCategory
    case formLimitedDuration
    case formLimitedDurationHint
    case formStartMonth
    case formInstallments
    case planningStandingOrder
    // Category names
    case catRent
    case catInsurance
    case catSubscription
    case catLeasing
    case catUtilities
    case catTelecom
    case catGroceries
    case catFuel
    case catTransport
    case catPersonal
    case catDining
    case catHousehold
    case catOther
    // Fixed-cost categories (added)
    case catMortgage
    case catHealthInsurance
    case catSerafe
    case catPublicTransport
    case catChildcare
    case catMembership
    case catAlimony
    case catTaxes
    // Variable-budget categories (added)
    case catClothing
    case catHealth
    case catLeisure
    case catEntertainment
    case catChildren
    case catPets
    case catGifts
    case catTravel
    case catCharity
    // Shared (fixed + variable)
    case catEducation

    // Overview
    case overviewTitle
    case overviewAvailableThisMonth
    case overviewNetIncome
    case overviewThirteenthSeparate
    case overviewRecurringFixed
    case overviewPlannedVariable
    case overviewBillsDueThisMonth
    case overviewOverdueBills
    case overviewSavingsTarget
    case overviewPlannedSavings
    case overviewShowCalculation
    case overviewTrendTitle
    case overviewStatusGood
    case overviewStatusTight
    case overviewStatusNegative
    case overviewAllocationOf
    case overviewSecurityStatus
    case overviewFormulaExplainer
    case overviewEmpty
    case chartSpendingTitle
    case chartByCategory

    // Planning
    case planningTitle
    case planningIncome
    case planningFixed
    case planningVariable
    case planningSavings
    case planningFixedExplainer
    case planningVariableExplainer
    case planningOneOffHint
    case planningEmpty
    case planningBalance

    // Bills
    case billsTitle
    case billsStatusOpen
    case billsStatusPaid
    case billsStateOverdue
    case billsStateDueThisMonth
    case billsStateFuture
    case billsAffectsBalance
    case billsNoBalanceImpact
    case billsDueDate
    case billsProvider
    case billsEmpty
    case billsMarkPaid
    case billsMarkOpen
    case billsOpenTotal

    // Schulden (debts)
    case navSchulden
    case schuldenSubtitle
    case schuldenEmpty
    case schuldenAddCta
    case schuldenTotal
    case schuldenOverdueBills
    case schuldenRecorded
    case schuldenManagedInBills
    case debtTypeOpenClaim
    case debtTypeBetreibung
    case debtTypePfaendung
    case debtTypeVerlustschein
    case debtTypeOther
    case debtCreditor
    case debtReference
    case debtDate
    case debtType
    case schuldenGuidanceTitle
    case schuldenTipContactTitle
    case schuldenTipContactBody
    case schuldenTipBetreibungTitle
    case schuldenTipBetreibungBody
    case schuldenTipExistenzminimumTitle
    case schuldenTipExistenzminimumBody
    case schuldenTipVerlustscheinTitle
    case schuldenTipVerlustscheinBody
    case schuldenTipCounselingTitle
    case schuldenTipCounselingBody
    case schuldenDisclaimer

    case commonSaved

    // Menu commands
    case commandsAdd

    // Empty-state calls to action
    case billsAddCta
    case sparenAddCta
    case overviewAddCta

    // Settings
    case settingsTitle
    case settingsLanguage
    case settingsTheme
    // Accent theme names
    case themeSwissRed
    case themeOrange
    case themeSand
    case themeGreen
    case themeTeal
    case themeBlue
    case themePurple
    case themePink
    case settingsData
    case settingsHousehold
    case settingsCanton
    // Profile
    case settingsProfile
    case profileName
    case profileChoosePicture
    case profileNoPicture
    case profileLocalNote
    case settingsSecurity
    case settingsAutoLock
    case settingsChangePassphrase
    case settingsBackup
    case settingsRestore
    case settingsPrivacy
    case settingsDangerZone
    case settingsDeleteAll
    case backupCreateTitle
    case backupPassphrase
    case backupHint
    case restorePreview
    case restoreConfirm
    case restoreWarning
    case backupInvalid
    case backupSavedHint

    // Report a problem
    case reportTitle
    case reportSummary
    case reportExpected
    case reportActual
    case reportSteps
    case reportArea
    case reportRedactionNote
    case reportCopyHint

    // PDF report export
    case exportReport
    case pdfTitle
    case pdfSubtitle
    case pdfSummary
    case pdfGeneratedOn
    case pdfConfidential
    case pdfPage
    case pdfTotal
    case pdfColProgress
    case pdfUnencryptedNote
    case pdfNoData
    case pdfDefaultHousehold

    // Lock / unlock
    case lockTitle
    case lockWelcomeSetup
    case lockWelcomeBack
    case lockSetupTitle
    case lockEnterPassphrase
    case lockWrongPassphrase
    case lockRecoveryWarning
    case lockRecoveryAcknowledge
    case securityNote

    // Onboarding (first-run guided flow)
    case onboardingIntroBody
    case onboardingStart
    case onboardingProfileTitle
    case onboardingProfileBody
    case onboardingSkip
    case onboardingFeaturePrivate
    case onboardingFeatureSecure
    case onboardingFeatureMoney
    case onboardingConfirmPassphrase
    case onboardingPassphraseNote
    case backupNudgeText
    case commonNext

    // iOS tab chrome (the "More" tab + its lock action)
    case navMore
    case actionLock
}

public struct Localization: Sendable {
    public let language: AppLanguage
    private let table: [L10nKey: String]

    public init(language: AppLanguage) {
        self.language = language
        self.table = Localization.tables[language] ?? Localization.tables[.deCH]!
    }

    /// Localized string for a key. Falls back to de-CH (never to a raw key,
    /// because parity is enforced by tests so every key is always present).
    public func string(_ key: L10nKey) -> String {
        table[key] ?? Localization.tables[.deCH]?[key] ?? key.rawValue
    }

    public static let tables: [AppLanguage: [L10nKey: String]] = [
        .deCH: deCH,
        .frCH: frCH,
        .itCH: itCH,
        .en: en,
        .rm: rm,
        .es: es,
        .ptPT: ptPT,
        .ptBR: ptBR,
        .nl: nl,
        .pl: pl,
        .da: da,
        .nb: nb,
        .sv: sv,
        .fi: fi,
        .ro: ro,
        .hu: hu,
        .sq: sq,
        .hr: hr,
        .sr: sr,
        .bs: bs,
        .mk: mk,
        .tr: tr,
        .ru: ru,
        .uk: uk,
        .ar: ar,
        .zhHans: zhHans,
        .ja: ja,
        .ko: ko,
        .vi: vi,
        .th: th,
        .hi: hi,
        .ta: ta,
        .si: si,
        .ur: ur,
        .ps: ps,
    ]
}
