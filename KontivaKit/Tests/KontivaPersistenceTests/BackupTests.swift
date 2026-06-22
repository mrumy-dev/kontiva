import XCTest
import KontivaCore
import KontivaSecurity
@testable import KontivaPersistence

final class BackupTests: XCTestCase {

    private let iters = 1_000

    private func tempLocation() throws -> StoreLocation {
        let dir = FileManager.default.temporaryDirectory
            .appendingPathComponent("kontiva-backup-\(UUID().uuidString)")
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return StoreLocation(directory: dir)
    }

    /// A store with some data.
    private func seededStore(passphrase: String) async throws -> (EncryptedStore, StoreLocation) {
        let loc = try tempLocation()
        let store = EncryptedStore(location: loc)
        try await store.createVault(passphrase: passphrase, iterations: iters)
        let income = Income(label: "Lohn", monthlyNet: Money.parse("6'500.00")!)
        try await store.mutate { $0.incomes = [income] }
        let bill = OneOffBill(provider: "Zahnarzt", amount: Money(rappen: 28_000), dueDate: Date())
        try await store.mutate { $0.bills = [bill] }
        return (store, loc)
    }

    func testBackupIsEncryptedNotPlaintext() async throws {
        let (store, _) = try await seededStore(passphrase: "local")
        let backup = try await store.makeBackup(backupPassphrase: "backup-pass", appVersion: "0.0.1")
        let asString = String(decoding: backup, as: UTF8.self)
        // The header is plaintext JSON, but the sealed payload must not be.
        XCTAssertFalse(asString.contains("Lohn"))
        XCTAssertFalse(asString.contains("Zahnarzt"))
    }

    func testPreviewCountsWithoutChangingData() async throws {
        let (store, _) = try await seededStore(passphrase: "local")
        let backup = try await store.makeBackup(backupPassphrase: "bp", appVersion: "0.0.1")
        let preview = try await store.previewBackup(data: backup, backupPassphrase: "bp")
        XCTAssertEqual(preview.counts["incomes"], 1)
        XCTAssertEqual(preview.counts["bills"], 1)
    }

    func testWrongBackupPassphraseThrows() async throws {
        let (store, _) = try await seededStore(passphrase: "local")
        let backup = try await store.makeBackup(backupPassphrase: "right", appVersion: "0.0.1")
        do {
            _ = try await store.previewBackup(data: backup, backupPassphrase: "wrong")
            XCTFail("expected wrongPassphrase")
        } catch {
            XCTAssertEqual(error as? SecurityError, .wrongPassphrase)
        }
    }

    func testCorruptBackupRejected() async throws {
        let (store, _) = try await seededStore(passphrase: "local")
        do {
            _ = try await store.previewBackup(data: Data("not a backup".utf8), backupPassphrase: "x")
            XCTFail("expected corrupted")
        } catch {
            XCTAssertEqual(error as? StoreError, .corrupted)
        }
    }

    /// The key portability test: a backup made on one vault restores into a
    /// DIFFERENT vault (different local passphrase / master key), bringing the
    /// dataset across intact.
    func testBackupIsPortableAcrossVaults() async throws {
        let (storeA, _) = try await seededStore(passphrase: "mac-a-pass")
        let backup = try await storeA.makeBackup(backupPassphrase: "portable-pass", appVersion: "0.0.1")

        // A brand-new vault on a "different Mac" with a different local passphrase.
        let locB = try tempLocation()
        let storeB = EncryptedStore(location: locB)
        try await storeB.createVault(passphrase: "mac-b-different", iterations: iters)
        let initialCount = try await storeB.snapshot().incomes.count
        XCTAssertEqual(initialCount, 0) // starts empty

        try await storeB.restoreBackup(data: backup, backupPassphrase: "portable-pass")

        let restored = try await storeB.snapshot()
        XCTAssertEqual(restored.incomes.first?.label, "Lohn")
        XCTAssertEqual(restored.incomes.first?.monthlyNet, Money.parse("6'500.00"))
        XCTAssertEqual(restored.bills.first?.provider, "Zahnarzt")
    }

    func testRestoreReplacesExistingData() async throws {
        let (storeA, _) = try await seededStore(passphrase: "a")
        let backup = try await storeA.makeBackup(backupPassphrase: "bp", appVersion: "0.0.1")

        let locB = try tempLocation()
        let storeB = EncryptedStore(location: locB)
        try await storeB.createVault(passphrase: "b", iterations: iters)
        // Pre-existing junk that must be wiped by the restore.
        try await storeB.mutate { $0.incomes = [Income(label: "OLD", monthlyNet: Money(rappen: 1))] }

        try await storeB.restoreBackup(data: backup, backupPassphrase: "bp")
        let restored = try await storeB.snapshot()
        XCTAssertEqual(restored.incomes.count, 1)
        XCTAssertEqual(restored.incomes.first?.label, "Lohn") // OLD is gone
    }
}
