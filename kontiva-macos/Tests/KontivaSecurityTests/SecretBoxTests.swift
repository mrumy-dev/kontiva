import XCTest
import CryptoKit
@testable import KontivaSecurity

final class SecretBoxTests: XCTestCase {

    func testRoundTrip() throws {
        let key = SymmetricKey(size: .bits256)
        let plaintext = Data("Krankenkasse: 340.00 CHF".utf8)
        let sealed = try SecretBox.seal(plaintext, with: key)
        XCTAssertNotEqual(sealed, plaintext)          // actually encrypted
        XCTAssertEqual(try SecretBox.open(sealed, with: key), plaintext)
    }

    func testWrongKeyFails() throws {
        let sealed = try SecretBox.seal(Data("x".utf8), with: SymmetricKey(size: .bits256))
        XCTAssertThrowsError(try SecretBox.open(sealed, with: SymmetricKey(size: .bits256))) {
            XCTAssertEqual($0 as? SecurityError, .decryptionFailed)
        }
    }

    func testTamperingIsDetected() throws {
        let key = SymmetricKey(size: .bits256)
        var sealed = try SecretBox.seal(Data("important amount".utf8), with: key)
        sealed[sealed.count - 1] ^= 0xFF              // flip a tag byte
        XCTAssertThrowsError(try SecretBox.open(sealed, with: key)) {
            XCTAssertEqual($0 as? SecurityError, .decryptionFailed)
        }
    }

    func testNonceIsRandomPerSeal() throws {
        let key = SymmetricKey(size: .bits256)
        let p = Data("same plaintext".utf8)
        XCTAssertNotEqual(try SecretBox.seal(p, with: key), try SecretBox.seal(p, with: key))
    }

    func testCodableConvenience() throws {
        struct Row: Codable, Equatable { let provider: String; let rappen: Int64 }
        let key = SymmetricKey(size: .bits256)
        let row = Row(provider: "Zahnarzt", rappen: 28_000)
        let sealed = try SecretBox.seal(row, with: key)
        XCTAssertEqual(try SecretBox.open(Row.self, from: sealed, with: key), row)
    }
}
