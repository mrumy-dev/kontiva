import Foundation
import KontivaSecurity

/// The decrypted contents of a backup: the whole dataset. Sealed under the backup
/// key before being written to a `.kontivabackup`.
struct BackupPayload: Codable, Sendable {
    var schemaVersion: Int
    var dataset: AppDataset
}

/// The on-disk backup container: a non-secret header (KDF params, metadata) plus
/// the AES-256-GCM sealed payload. Portable to another Mac; opened only with the
/// separate backup passphrase.
struct BackupContainer: Codable, Sendable {
    var format: String          // "kontiva.backup.v1"
    var kdf: String
    var cipher: String
    var salt: Data
    var iterations: Int
    var createdAt: Date
    var appVersion: String
    var counts: [String: Int]
    var sealed: Data
}

/// What the user sees before confirming a restore (no secrets).
public struct BackupPreview: Sendable, Equatable {
    public let createdAt: Date
    public let appVersion: String
    public let counts: [String: Int]
}

public extension AppDataset {
    /// Item counts for backup metadata / restore preview.
    var counts: [String: Int] {
        [
            "incomes": incomes.count,
            "fixedCosts": fixedCosts.count,
            "variableBudgets": variableBudgets.count,
            "savingsGoals": savingsGoals.count,
            "bills": bills.count,
        ]
    }
}
