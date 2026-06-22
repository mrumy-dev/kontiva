import Foundation
import LocalAuthentication
import Security

/// Which biometric the Mac offers.
enum BiometricKind {
    case touchID, faceID, opticID, none

    var label: String {
        switch self {
        case .touchID: return "Touch ID"
        case .faceID:  return "Face ID"
        case .opticID: return "Optic ID"
        case .none:    return ""
        }
    }
    var icon: String {
        switch self {
        case .touchID: return "touchid"
        case .faceID:  return "faceid"
        case .opticID: return "opticid"
        case .none:    return "lock"
        }
    }
    var isAvailable: Bool { self != .none }
}

enum Biometrics {
    /// The biometric the Mac currently has enrolled and available.
    static var kind: BiometricKind {
        let ctx = LAContext()
        var error: NSError?
        guard ctx.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) else { return .none }
        switch ctx.biometryType {
        case .touchID: return .touchID
        case .faceID:  return .faceID
        case .opticID: return .opticID
        default:       return .none
        }
    }
}

/// Stores the unlock code in the Keychain as a *convenience* layer over the code:
/// retrieval is gated by an explicit Touch ID check (`LAContext.evaluatePolicy`).
/// The code stays the root secret, the no-recovery guarantee is unchanged, and the
/// item never leaves the device (`...ThisDeviceOnly`, no iCloud sync).
///
/// NOTE: the biometric gate is enforced in-app, not by the Secure Enclave — a
/// deliberate trade-off mirroring iOS. Production hardening: bind the item to the
/// Secure Enclave with a `.biometryCurrentSet` `SecAccessControl`.
enum BiometricVault {
    private static let service = "ch.kontiva.app.biometric-code"
    private static let account = "vault"

    /// Is a code stored? (No biometric prompt — plain presence check.)
    static var hasStored: Bool {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: false,
        ]
        return SecItemCopyMatching(query as CFDictionary, nil) == errSecSuccess
    }

    @discardableResult
    static func store(code: String) -> Bool {
        delete()
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecValueData as String: Data(code.utf8),
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
        ]
        return SecItemAdd(query as CFDictionary, nil) == errSecSuccess
    }

    static func delete() {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
        SecItemDelete(query as CFDictionary)
    }

    /// Prompts for biometrics and, on success, returns the stored code.
    static func retrieve(reason: String) async -> String? {
        let ctx = LAContext()
        do {
            let ok = try await ctx.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: reason)
            guard ok else { return nil }
        } catch {
            return nil
        }
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
        ]
        var item: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &item) == errSecSuccess,
              let data = item as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }
}
