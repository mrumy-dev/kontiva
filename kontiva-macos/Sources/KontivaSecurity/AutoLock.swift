import Foundation
import CryptoKit
import KontivaCore

/// Pure, testable auto-lock decision. The UI owns the timer; this just answers
/// whether enough idle time has passed to re-lock.
public enum AutoLock {
    public static func shouldLock(lastActivity: Date, now: Date,
                                  interval: AutoLockInterval) -> Bool {
        guard let seconds = interval.seconds else { return false } // .never
        return now.timeIntervalSince(lastActivity) >= seconds
    }
}

/// Holds the decrypted master key while the app is unlocked and drops it on lock.
///
/// CryptoKit's `SymmetricKey` zeroes its backing buffer when it is deallocated,
/// so releasing the reference on `lock()` removes the key material from memory as
/// far as the platform allows. There is no recovery path and no backdoor.
public final class SecuritySession {
    private var masterKey: SymmetricKey?
    public private(set) var lastActivity: Date

    public init(now: Date = Date()) {
        self.lastActivity = now
    }

    public var isUnlocked: Bool { masterKey != nil }

    public func unlock(with key: SymmetricKey, now: Date = Date()) {
        masterKey = key
        lastActivity = now
    }

    /// The current master key, only while unlocked.
    public func currentKey() -> SymmetricKey? { masterKey }

    public func noteActivity(now: Date = Date()) {
        if isUnlocked { lastActivity = now }
    }

    /// Drop the decrypted key. After this, `currentKey()` is nil.
    public func lock() {
        masterKey = nil
    }

    /// Lock if idle past the interval; returns true if it locked.
    @discardableResult
    public func lockIfIdle(interval: AutoLockInterval, now: Date = Date()) -> Bool {
        guard isUnlocked,
              AutoLock.shouldLock(lastActivity: lastActivity, now: now, interval: interval)
        else { return false }
        lock()
        return true
    }
}
