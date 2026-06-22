import XCTest
import KontivaCore
import KontivaSecurity
@testable import KontivaPersistence

final class EncryptedStoreTests: XCTestCase {

    private var tempDir: URL!
    private var location: StoreLocation!
    private let iters = 1_000 // fast for tests; app uses KeyVault.defaultIterations

    override func setUpWithError() throws {
        tempDir = FileManager.default.temporaryDirectory
            .appendingPathComponent("kontiva-tests-\(UUID().uuidString)")
        try FileManager.default.createDirectory(at: tempDir, withIntermediateDirectories: true)
        location = StoreLocation(directory: tempDir)
    }

    override func tearDownWithError() throws {
        try? FileManager.default.removeItem(at: tempDir)
    }

    private func sampleIncome() -> Income {
        Income(label: "Lohn", monthlyNet: Money.parse("6'500.00")!)
    }

    // MARK: - Setup / vault existence

    func testFreshLocationHasNoVault() async {
        let store = EncryptedStore(location: location)
        XCTAssertFalse(store.hasExistingVault())
    }

    func testCreateVaultWritesFilesAndUnlocks() async throws {
        let store = EncryptedStore(location: location)
        try await store.createVault(passphrase: "pass", iterations: iters)
        XCTAssertTrue(store.hasExistingVault())
        let unlocked = await store.isUnlocked
        XCTAssertTrue(unlocked)
        XCTAssertTrue(FileManager.default.fileExists(atPath: location.dataURL.path))
    }

    func testCannotCreateVaultTwice() async throws {
        let store = EncryptedStore(location: location)
        try await store.createVault(passphrase: "pass", iterations: iters)
        do {
            try await store.createVault(passphrase: "pass", iterations: iters)
            XCTFail("expected vaultAlreadyExists")
        } catch {
            XCTAssertEqual(error as? StoreError, .vaultAlreadyExists)
        }
    }

    // MARK: - Encryption at rest

    func testDataFileIsEncryptedNotPlaintext() async throws {
        let store = EncryptedStore(location: location)
        try await store.createVault(passphrase: "pass", iterations: iters)
        try await store.mutate { $0.incomes = [Income(label: "GeheimerLohn", monthlyNet: Money(rappen: 654_321))] }

        let raw = try Data(contentsOf: location.dataURL)
        let asString = String(decoding: raw, as: UTF8.self)
        XCTAssertFalse(asString.contains("GeheimerLohn"))   // label not in plaintext
        XCTAssertFalse(asString.contains("654321"))         // amount not in plaintext
        XCTAssertFalse(asString.contains("incomes"))        // JSON keys not in plaintext
    }

    func testKeystoreContainsNoPassphrase() async throws {
        let store = EncryptedStore(location: location)
        try await store.createVault(passphrase: "my-secret-pass", iterations: iters)
        let raw = try Data(contentsOf: location.keystoreURL)
        XCTAssertNil(raw.range(of: Data("my-secret-pass".utf8)))
    }

    // MARK: - Persistence round-trip across instances (the relaunch case)

    func testDataSurvivesNewStoreInstance() async throws {
        // First "launch": create + add data.
        let first = EncryptedStore(location: location)
        try await first.createVault(passphrase: "pass", iterations: iters)
        let income = sampleIncome()
        try await first.mutate { $0.incomes = [income] }
        await first.lock()

        // Second "launch": brand new instance pointed at the same files.
        let second = EncryptedStore(location: location)
        XCTAssertTrue(second.hasExistingVault())
        try await second.unlock(passphrase: "pass")
        let data = try await second.snapshot()
        XCTAssertEqual(data.incomes.first?.label, "Lohn")
        XCTAssertEqual(data.incomes.first?.monthlyNet, Money.parse("6'500.00"))
    }

    // MARK: - Wrong passphrase & tamper

    func testWrongPassphraseThrows() async throws {
        let store = EncryptedStore(location: location)
        try await store.createVault(passphrase: "right", iterations: iters)
        await store.lock()
        let reopened = EncryptedStore(location: location)
        do {
            try await reopened.unlock(passphrase: "wrong")
            XCTFail("expected wrongPassphrase")
        } catch {
            XCTAssertEqual(error as? SecurityError, .wrongPassphrase)
        }
    }

    func testTamperedDataFileFailsToOpen() async throws {
        let store = EncryptedStore(location: location)
        try await store.createVault(passphrase: "pass", iterations: iters)
        await store.lock()

        // Corrupt the encrypted blob.
        var raw = try Data(contentsOf: location.dataURL)
        raw[raw.count - 1] ^= 0xFF
        try raw.write(to: location.dataURL)

        let reopened = EncryptedStore(location: location)
        do {
            try await reopened.unlock(passphrase: "pass")
            XCTFail("expected decryption failure")
        } catch {
            XCTAssertEqual(error as? SecurityError, .decryptionFailed)
        }
    }

    // MARK: - Change passphrase & delete

    func testChangePassphrasePreservesData() async throws {
        let store = EncryptedStore(location: location)
        try await store.createVault(passphrase: "old", iterations: iters)
        try await store.mutate { $0.bills = [OneOffBill(provider: "Zahnarzt",
                                                         amount: Money(rappen: 28_000),
                                                         dueDate: Date())] }
        try await store.changePassphrase(old: "old", new: "new", newIterations: iters)
        await store.lock()

        let reopened = EncryptedStore(location: location)
        do { try await reopened.unlock(passphrase: "old"); XCTFail("old must fail") }
        catch { XCTAssertEqual(error as? SecurityError, .wrongPassphrase) }

        try await reopened.unlock(passphrase: "new")
        let data = try await reopened.snapshot()
        XCTAssertEqual(data.bills.first?.provider, "Zahnarzt")
    }

    func testDeleteAllDataRemovesEverything() async throws {
        let store = EncryptedStore(location: location)
        try await store.createVault(passphrase: "pass", iterations: iters)
        try await store.deleteAllData()
        XCTAssertFalse(store.hasExistingVault())
        XCTAssertFalse(FileManager.default.fileExists(atPath: location.dataURL.path))
        let unlocked = await store.isUnlocked
        XCTAssertFalse(unlocked)
    }

    func testMutateWhileLockedThrows() async throws {
        let store = EncryptedStore(location: location)
        do {
            try await store.mutate { $0.incomes = [] }
            XCTFail("expected locked")
        } catch {
            XCTAssertEqual(error as? StoreError, .locked)
        }
    }
}
