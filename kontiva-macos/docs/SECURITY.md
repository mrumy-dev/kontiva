# Kontiva — Security Model (target)

> This describes the target security architecture. **Phases 1–2** are implemented
> and unit-tested: the KDF, key wrapping, authenticated encryption, auto-lock, and
> an **encrypted local store**. The entire dataset is AES-256-GCM sealed at rest
> under the master key; the master key is stored only wrapped (PBKDF2-derived).
> Verified on disk: `store.kenc` is ciphertext with no plaintext budget terms, and
> `keystore.json` holds no passphrase or unwrapped key. Data survives app relaunch
> (the app shows Unlock, not Setup). The **document vault** (per-file encryption)
> is still Phase 3.

## Principles

- Local-first, offline-first. No network calls of any kind.
- No recovery backdoor. If the passphrase is lost, the data is not recoverable.
- Decrypted keys live in memory only while unlocked, and are cleared on lock.
- No private financial data leaves the device; no sensitive values in logs.
- No original document paths are stored or shown — only display names.

## Key hierarchy (target)

```
passphrase / PIN
      │   Argon2id (preferred) / PBKDF2-HMAC-SHA256 (documented fallback)
      ▼
 KEK (key-encryption key)  ──wrap (AES-GCM / ChaCha20-Poly1305)──▶  wrapped master key
                                                                         │
                                                                         ▼
                                                                random master data key
                                                              (only ever stored wrapped)
```

- The passphrase is **never** stored.
- The master key is random, generated once, and stored only in wrapped form.
- Unlock = derive KEK from passphrase → unwrap master key into memory.

## Database at rest (target)

- **Preferred:** SQLCipher (page-level AES) for the encrypted local database.
- **Tradeoff (flagged):** SQLCipher via SwiftPM requires vendoring the SQLCipher
  amalgamation as a C target — non-trivial. Because Phase 0 persists no private
  data, this decision is **deferred without ever falling back to plaintext.**
- **Alternative-if-needed (documented, decided explicitly):** an application-layer
  AES-GCM envelope over the store. The choice will be made deliberately and
  written down — never by silently storing plaintext.

## Document vault (target)

- Each document encrypted with AES-GCM / ChaCha20-Poly1305, a random nonce per
  file, and a content key wrapped by the master key.
- Files stored under opaque IDs. Original filesystem paths are never persisted.

## Auto-lock (target)

- Idle timer (configurable: 1 / 5 / 15 min / never) plus manual lock.
- On lock, decrypted key material is zeroed and dropped as far as Swift and
  CryptoKit allow.
- No automatic unlock by default.

## Backup & restore (target)

- Backup is encrypted with a **separate backup passphrase** (its own KDF →
  AES-GCM envelope over a serialized export). Portable to another Mac.
- Restore shows a **preview first**, then requires an **explicit destructive
  confirmation** before overwriting local data.

## Bug reports

- Composed and redacted **locally**; reviewed and copied **by the user**.
- Only non-sensitive metadata is auto-added (app version, macOS version, app
  language, selected area). No automatic submission, no embedded tokens, no
  telemetry, no automatic attachment of screenshots, documents, logs, the
  database, or vault files.

## Honest status (after Phase 1)

| Control | Status |
|---------|--------|
| KDF (PBKDF2-HMAC-SHA256) | ✅ implemented + tested against published vectors |
| Key wrapping (random master key, AES-GCM wrapped) | ✅ implemented + tested |
| Authenticated encryption (AES-256-GCM `SecretBox`) | ✅ implemented + tested (tamper-detecting) |
| Change passphrase (re-wrap, data preserved) | ✅ implemented + tested |
| Auto-lock logic + key-clearing session | ✅ implemented + tested |
| Idle auto-lock wired into the app (configurable 1/5/15 min, activity-reset) | ✅ implemented; locks + drops the key after idle |
| App Sandbox + no-network entitlement (enforced at OS level) | ✅ ad-hoc signed; verified (data in container, document import works under powerbox) |
| Lock screen backed by real key wrapping | ✅ no plaintext compare |
| Encrypted local store (whole dataset AES-256-GCM at rest) | ✅ implemented + tested; survives relaunch; verified ciphertext on disk |
| Change passphrase / delete-all wired in Settings | ✅ implemented + tested |
| Encrypted document vault (per-file AES-256-GCM, opaque ids) | ✅ implemented + tested; import/export/delete verified live (byte-perfect round-trip, ciphertext on disk, no original paths) |
| Portable encrypted backup (separate backup passphrase) | ✅ implemented + tested; verified live (ciphertext payload, portable across vaults) |
| Guarded restore (preview counts/date, then explicit destructive confirm) | ✅ implemented + tested; verified live |
| Local redaction for bug reports | ✅ implemented + tested |
| No network code | ✅ none present |

KDF cost: PBKDF2-HMAC-SHA256 at 210 000 iterations ≈ 0.4 s on Apple Silicon; the
app runs it off the main thread. Argon2id remains the preferred upgrade.

Sandbox: the app ships ad-hoc signed with `Resources/Kontiva.entitlements`
(`com.apple.security.app-sandbox` = true, `files.user-selected.read-write` = true,
**no network entitlements**). The same entitlements file is reused by the
Developer-ID signing step (`scripts/sign_and_notarize.sh`) once an Apple Developer
account is available.
