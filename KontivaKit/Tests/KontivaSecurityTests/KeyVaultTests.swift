import XCTest
import CryptoKit
@testable import KontivaSecurity

final class KeyVaultTests: XCTestCase {

    // Small iteration count keeps tests fast; production uses defaultIterations.
    private let iters = 1_000

    private func raw(_ key: SymmetricKey) -> Data { key.withUnsafeBytes { Data($0) } }

    func testCreateThenUnwrapReturnsSameMasterKey() throws {
        let (material, master) = try KeyVault.create(passphrase: "correct horse", iterations: iters)
        let unwrapped = try KeyVault.unwrap(passphrase: "correct horse", material: material)
        XCTAssertEqual(raw(master), raw(unwrapped))
    }

    func testWrongPassphraseThrows() throws {
        let (material, _) = try KeyVault.create(passphrase: "right", iterations: iters)
        XCTAssertThrowsError(try KeyVault.unwrap(passphrase: "wrong", material: material)) { error in
            XCTAssertEqual(error as? SecurityError, .wrongPassphrase)
        }
    }

    func testMaterialContainsNoPlaintextKeyOrPassphrase() throws {
        let (material, master) = try KeyVault.create(passphrase: "s3cret-pass", iterations: iters)
        // The wrapped key must not equal the raw master key bytes.
        XCTAssertNotEqual(material.wrappedKey, raw(master))
        // The passphrase bytes must not appear in any stored field.
        let pass = Data("s3cret-pass".utf8)
        XCTAssertFalse(material.wrappedKey.range(of: pass) != nil)
        XCTAssertFalse(material.salt.range(of: pass) != nil)
        XCTAssertEqual(material.kdf, KeyVault.kdfIdentifier)
        XCTAssertEqual(material.cipher, KeyVault.cipherIdentifier)
    }

    func testEachCreateUsesFreshSalt() throws {
        let (a, _) = try KeyVault.create(passphrase: "same", iterations: iters)
        let (b, _) = try KeyVault.create(passphrase: "same", iterations: iters)
        XCTAssertNotEqual(a.salt, b.salt)
        XCTAssertNotEqual(a.wrappedKey, b.wrappedKey)
    }

    func testChangePassphrasePreservesMasterKeyAndData() throws {
        let (material, master) = try KeyVault.create(passphrase: "old-pass", iterations: iters)

        // Encrypt some data under the original master key.
        let secret = Data("Lohn: 6500.00".utf8)
        let box = try SecretBox.seal(secret, with: master)

        // Rotate the passphrase.
        let rotated = try KeyVault.changePassphrase(old: "old-pass", new: "new-pass",
                                                    material: material, newIterations: iters)

        // Old passphrase no longer unwraps; new one does.
        XCTAssertThrowsError(try KeyVault.unwrap(passphrase: "old-pass", material: rotated))
        let recovered = try KeyVault.unwrap(passphrase: "new-pass", material: rotated)

        // The master key is unchanged, so previously encrypted data still opens.
        XCTAssertEqual(raw(master), raw(recovered))
        XCTAssertEqual(try SecretBox.open(box, with: recovered), secret)
    }

    func testChangePassphraseWithWrongOldThrows() throws {
        let (material, _) = try KeyVault.create(passphrase: "old-pass", iterations: iters)
        XCTAssertThrowsError(
            try KeyVault.changePassphrase(old: "not-old", new: "new", material: material, newIterations: iters)
        ) { XCTAssertEqual($0 as? SecurityError, .wrongPassphrase) }
    }

    func testMaterialCodableRoundTrip() throws {
        let (material, _) = try KeyVault.create(passphrase: "p", iterations: iters)
        let data = try JSONEncoder().encode(material)
        let decoded = try JSONDecoder().decode(WrappedKeyMaterial.self, from: data)
        XCTAssertEqual(material, decoded)
    }

    func testUnicodeNormalizationOfPassphrase() throws {
        // "é" composed vs decomposed should derive the same key.
        let composed = "Cafe\u{00E9}"        // é precomposed
        let decomposed = "Cafe\u{0065}\u{0301}" // e + combining acute
        let (material, _) = try KeyVault.create(passphrase: composed, iterations: iters)
        XCTAssertNoThrow(try KeyVault.unwrap(passphrase: decomposed, material: material))
    }
}
