import XCTest
import CryptoKit
import KontivaCore
@testable import KontivaSecurity

final class AutoLockTests: XCTestCase {

    private let t0 = Date(timeIntervalSince1970: 1_000_000)

    func testNeverNeverLocks() {
        let later = t0.addingTimeInterval(60 * 60 * 24)
        XCTAssertFalse(AutoLock.shouldLock(lastActivity: t0, now: later, interval: .never))
    }

    func testLocksWhenIdlePastInterval() {
        let after6min = t0.addingTimeInterval(6 * 60)
        XCTAssertTrue(AutoLock.shouldLock(lastActivity: t0, now: after6min, interval: .fiveMinutes))
    }

    func testDoesNotLockBeforeInterval() {
        let after4min = t0.addingTimeInterval(4 * 60)
        XCTAssertFalse(AutoLock.shouldLock(lastActivity: t0, now: after4min, interval: .fiveMinutes))
    }

    func testExactBoundaryLocks() {
        let exactly5 = t0.addingTimeInterval(5 * 60)
        XCTAssertTrue(AutoLock.shouldLock(lastActivity: t0, now: exactly5, interval: .fiveMinutes))
    }

    // MARK: - Session lifecycle: the key is present only while unlocked

    func testSessionUnlockHoldsKeyAndLockClearsIt() {
        let session = SecuritySession(now: t0)
        XCTAssertFalse(session.isUnlocked)
        XCTAssertNil(session.currentKey())

        let key = SymmetricKey(size: .bits256)
        session.unlock(with: key, now: t0)
        XCTAssertTrue(session.isUnlocked)
        XCTAssertNotNil(session.currentKey())

        session.lock()
        XCTAssertFalse(session.isUnlocked)
        XCTAssertNil(session.currentKey())   // key dropped from memory
    }

    func testLockIfIdleLocksOnlyWhenIdle() {
        let session = SecuritySession(now: t0)
        session.unlock(with: SymmetricKey(size: .bits256), now: t0)

        XCTAssertFalse(session.lockIfIdle(interval: .fiveMinutes, now: t0.addingTimeInterval(60)))
        XCTAssertTrue(session.isUnlocked)

        XCTAssertTrue(session.lockIfIdle(interval: .fiveMinutes, now: t0.addingTimeInterval(6 * 60)))
        XCTAssertFalse(session.isUnlocked)
    }

    func testActivityResetsIdleTimer() {
        let session = SecuritySession(now: t0)
        session.unlock(with: SymmetricKey(size: .bits256), now: t0)
        // Activity at +4min resets the clock; +6min from t0 is only +2min from activity.
        session.noteActivity(now: t0.addingTimeInterval(4 * 60))
        XCTAssertFalse(session.lockIfIdle(interval: .fiveMinutes, now: t0.addingTimeInterval(6 * 60)))
        XCTAssertTrue(session.isUnlocked)
    }
}
