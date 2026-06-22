import XCTest
@testable import KontivaSecurity

final class KDFTests: XCTestCase {

    private func hex(_ data: Data) -> String {
        data.map { String(format: "%02x", $0) }.joined()
    }

    // Published PBKDF2-HMAC-SHA256 test vectors (P="password", S="salt").
    // These prove the implementation is correct, not just self-consistent.

    func testVectorIteration1() {
        let out = KDF.pbkdf2SHA256(password: Data("password".utf8),
                                   salt: Data("salt".utf8),
                                   iterations: 1, keyLength: 32)
        XCTAssertEqual(hex(out),
            "120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b")
    }

    func testVectorIteration2() {
        let out = KDF.pbkdf2SHA256(password: Data("password".utf8),
                                   salt: Data("salt".utf8),
                                   iterations: 2, keyLength: 32)
        XCTAssertEqual(hex(out),
            "ae4d0c95af6b46d32d0adff928f06dd02a303f8ef3c251dfd6e2d85a95474c43")
    }

    func testVectorIteration4096() {
        let out = KDF.pbkdf2SHA256(password: Data("password".utf8),
                                   salt: Data("salt".utf8),
                                   iterations: 4096, keyLength: 32)
        XCTAssertEqual(hex(out),
            "c5e478d59288c841aa530db6845c4c8d962893a001ce4e11a4963873aa98134a")
    }

    func testDeterministic() {
        let a = KDF.pbkdf2SHA256(password: Data("hunter2".utf8), salt: Data("s".utf8),
                                 iterations: 1000, keyLength: 32)
        let b = KDF.pbkdf2SHA256(password: Data("hunter2".utf8), salt: Data("s".utf8),
                                 iterations: 1000, keyLength: 32)
        XCTAssertEqual(a, b)
    }

    func testDifferentSaltDifferentKey() {
        let a = KDF.pbkdf2SHA256(password: Data("pw".utf8), salt: Data("salt1".utf8),
                                 iterations: 1000, keyLength: 32)
        let b = KDF.pbkdf2SHA256(password: Data("pw".utf8), salt: Data("salt2".utf8),
                                 iterations: 1000, keyLength: 32)
        XCTAssertNotEqual(a, b)
    }

    func testKeyLengthHonoured() {
        XCTAssertEqual(KDF.pbkdf2SHA256(password: Data("p".utf8), salt: Data("s".utf8),
                                        iterations: 10, keyLength: 16).count, 16)
        XCTAssertEqual(KDF.pbkdf2SHA256(password: Data("p".utf8), salt: Data("s".utf8),
                                        iterations: 10, keyLength: 48).count, 48)
    }
}
