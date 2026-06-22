# Kontiva — Privacy Statement

Kontiva is built privacy-first. These guarantees are architectural, not policy
promises bolted on afterwards.

## What Kontiva does NOT do

- ❌ No cloud sync
- ❌ No server login, no account
- ❌ No bank login or bank connection
- ❌ No telemetry, no analytics
- ❌ No advertising SDKs
- ❌ No crash-reporting SDKs that send data automatically
- ❌ No hidden network calls — the app makes **no** network requests
- ❌ No private financial data leaving the device
- ❌ No sensitive values written to logs
- ❌ No original document paths shown in the UI

## What Kontiva does

- ✅ Runs entirely on your Mac, offline
- ✅ (Target) Stores private data only in an encrypted local database
- ✅ (Target) Stores documents only in an encrypted local vault
- ✅ (Target) Requires a passphrase/PIN to unlock, with auto-lock
- ✅ (Target) Encrypts portable backups with a separate backup passphrase
- ✅ Composes bug reports locally, redacted, and only when you choose to — you
  review and copy them yourself; nothing is ever sent automatically

## Phase 0 note

In this early build, the encryption layer is not implemented yet. To keep that
promise honest, **Phase 0 persists no private data at all** — there is no
plaintext private data at rest because there is no private data at rest.

## Money

All amounts are stored as integer Rappen and never as floating-point numbers, so
your figures are exact. CHF formatting is for display only.
