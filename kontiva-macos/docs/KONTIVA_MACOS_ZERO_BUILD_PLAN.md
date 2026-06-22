# Kontiva — macOS Zero Build Plan

> Status: **Foundation phase (Phase 0).** This is **not** a beta and **not** V1.
> macOS-only, native Swift. No cloud, no telemetry, no network, no payment logic.

This document is the single source of truth for how Kontiva is being built from
absolute zero as a native macOS app. It is written before any application code
so that the architecture, calculation model, security model, and quality gates
are explicit and reviewable.

---

## 1. Folder / repo state

Inspected on 2026-06-18 in `/Users/mohamedrumy/kontiva`.

| Item | Finding |
|------|---------|
| Source code | None. No prior implementation of any kind. |
| `assets/` | Present. Approved Kontiva brand masters + Claude-upload copies. **Untouched, preserved.** |
| `.DS_Store` | Present at root and inside `assets/`. macOS junk; ignored via `.gitignore`, never committed. |
| Other | Nothing else. |

**Verdict: the folder is safe to scaffold.** Scaffolding only *adds* new files in
new directories (`Sources/`, `Tests/`, `docs/`, `Resources/`). No existing file
is modified, moved, or deleted. The brand masters under `assets/brand/masters/`
are treated as read-only.

---

## 2. Git state and exact git commands run

Commands run during inspection (read-only):

```
git status --short
git branch --show-current
git remote -v
git log --oneline -5
```

Results:

- `git status --short` → `?? .DS_Store` and `?? assets/` (both untracked).
- `git branch --show-current` → `main`.
- `git remote -v` → `origin git@github.com-personal:mrumy-dev/kontiva.git` (fetch + push).
- `git log` → no commits yet (fresh repository).

**Policy for this build:** no commit, no branch, no tag, no push, no PR unless you
explicitly ask. A `.gitignore` is added so that — when you *do* choose to commit —
secrets, databases, vault files, backups, build artifacts, and `.DS_Store` cannot
be staged by accident.

---

## 3. Asset folder inventory and usage decision

All brand assets live under `assets/brand/`. Two mirrors exist:

- `assets/brand/masters/` — **the authoritative brand masters. Never modified.**
- `assets/brand/upload-friendly/` — duplicate subset for uploading to Claude projects.

### Inventory (masters)

| File | Type / size | Role |
|------|-------------|------|
| `kontiva-icon.svg` | SVG 1024² (dark, gradient tile) | Primary app-icon master |
| `kontiva-icon-light.svg` | SVG 1024² (light tile) | Light/alt app-icon master |
| `kontiva-mark.svg` | SVG 512² (monochrome K + ledger bars) | Standalone mark, sidebar glyph |
| `kontiva-wordmark.svg` | SVG 1080×260 (charcoal text) | Light-context wordmark |
| `kontiva-wordmark-dark.svg` | SVG 1080×260 (white on charcoal) | Dark-context wordmark |
| `kontiva-icon-{32..1024}.png` | PNG, transparent | App-icon raster source |
| `kontiva-wordmark{,-dark}.png` | PNG 1080×260 | Wordmark raster |
| `kontiva-logo-concept-sheet.png` | PNG 1536×1024 | Approved concept reference |
| `kontiva-brand-reference.pdf` | PDF | Full brand reference |
| `*.md` | Markdown | Brand brief, usage rules, manifest |

### Approved visual concept (verified by viewing the assets)

Dark rounded-square tile (`#121A22` charcoal with a subtle gradient), a white
geometric **K**, white ledger balance bars top and bottom, and a single small
**Swiss-red (`#E11D2E`) minus accent** at the left. The wordmark is "Kontiva" in
a calm geometric sans with a small red square accent. Palette per brand brief:
charcoal `#121A22`, off-white `#F6F7F8`, warm light `#F8F7F4`, Swiss red
`#E11D2E`, soft border `#DCE1E5`.

### Usage decision

| Need | Decision | Rationale |
|------|----------|-----------|
| **App icon (`.icns`)** | Generate from `kontiva-icon-1024.png` (master) into a **separate generated location** (`Resources/AppIcon/` / build output), never mixed with masters. Use `sips` + `iconutil` (ship with Command Line Tools). | Faithful downscale of the approved high-res raster; no redesign/recolor. |
| **Sidebar glyph** | Render a faithful native vector of the mark in SwiftUI using the **exact coordinates from `kontiva-mark.svg`** (`KontivaMark` view), tinted to context (charcoal on light, off-white on dark) with the red accent preserved. | Crisp at every size; uses approved geometry verbatim, not a redesign. The official PNGs are also bundled as fallback truth. |
| **Sidebar / header wordmark** | Bundle the official `kontiva-wordmark.png` / `kontiva-wordmark-dark.png` (unmodified copies) as app resources and display the version matching the appearance. | Unambiguously the approved wordmark; no font substitution. |
| **Onboarding / About** | Use the icon + wordmark combination. | Per brand usage rules. |

**Rules honoured:** no redesign, no recolor, no stretch, no crop, no canton
emblems, no generic finance icons, no placeholder branding. Masters stay
read-only; every generated output is derived only from approved high-res sources
and stored separately.

---

## 4. Proposed native macOS architecture

### Toolchain reality (drives every decision below)

- **Swift 6.3.2** is installed and works. SwiftUI, AppKit, Foundation, CryptoKit
  all resolve and type-check against the Command Line Tools macOS SDK (verified).
- **Full Xcode is NOT installed** — only Command Line Tools. Therefore
  `xcodebuild` and `.xcodeproj`/`.xcworkspace` builds are unavailable, and no
  `xcodegen`/`tuist` are present.
- **Consequence:** the project is a **Swift Package Manager** package
  (`Package.swift`), built with `swift build` / `swift test`. A small packaging
  script later wraps the built executable into a `Kontiva.app` bundle
  (Info.plist + Resources + `.icns`). The same `Package.swift` opens directly in
  full Xcode later with zero changes — so this choice does not lock anyone in;
  it only removes the dependency on a 12 GB Xcode install for the core work.
- **Pending action (you, once):** `sudo xcodebuild -license accept` — the SDK
  license has not been accepted on this machine, which can gate some compiler
  invocations. This is a one-time `sudo` step; this build does not run `sudo`
  on your behalf.

### Module layout (SwiftPM targets)

```
Kontiva (executable, @main)        SwiftUI app entry; wires UI ↔ services
 └─ KontivaUI (library)            SwiftUI views, design system, brand, L10n
     └─ KontivaCore (library)      Pure domain: money, calc engine, bills,
                                   classification, redaction, L10n tables.
                                   Foundation only — NO SwiftUI, NO persistence.
 (later) KontivaSecurity (library) KDF, key wrap, AES-GCM vault crypto, auto-lock
 (later) KontivaPersistence (lib)  Encrypted SQLite repositories behind protocols
 KontivaCoreTests (test target)    Money, calc, bills, redaction, L10n parity
```

Key boundary: **the UI never talks to the storage/crypto layer directly.** It
depends on repository *protocols*. In Phase 0 those protocols are backed by
**in-memory** implementations only — no private data is written to disk until the
encrypted persistence layer exists.

### Cross-cutting rules baked into the architecture

- **No networking.** No `URLSession`, no sockets, no analytics/crash SDKs. The
  shipped app will declare an App Sandbox with **no** network entitlement.
- **No floats for money.** Money is `Int64` Rappen everywhere; CHF strings are
  presentation-only and formatted from the integer without going through `Double`.
- **No private data in logs.** A redaction layer exists before any text leaves a
  buffer (e.g. the bug-report composer).

---

## 5. Screen architecture

`NavigationSplitView`: a branded sidebar + detail area, native macOS window
behaviour, responsive from compact to wide. A **Lock gate** sits in front of
everything until unlocked (real crypto arrives in the security phase).

| # | Section | de-CH label | Purpose |
|---|---------|-------------|---------|
| 1 | Overview | Übersicht | Dashboard with the transparent available-this-month calculation |
| 2 | Monthly Planning | Monatsplanung | Income / fixed costs / variable budgets / savings, clearly separated |
| 3 | Bills | Rechnungen | One-off obligations with due dates and explicit balance impact |
| 4 | Documents | Tresor | Encrypted document vault, grouped (tax / salary / insurance / bills / other) |
| 5 | Taxes | Steuern | Tax-prep cockpit: year selector, Swiss checklist, progress, disclaimer |
| 6 | Settings | Einstellungen | Language, household, canton, security, backup/restore, danger zone |
| 7 | Report Problem | Problem melden | Local, user-reviewed, redacted bug-report composer |
| — | Lock / Unlock | Sperren | First-run setup, recovery warning, unlock, auto-lock |

Every screen has a designed empty state. Sidebar carries strong Kontiva brand
presence (mark + wordmark). Layout uses a consistent spacing scale and a card
grid that reflows rather than leaving awkward gaps.

---

## 6. Calculation model

All amounts are `Money` (`Int64` Rappen). The dashboard shows the math openly:

```
Available this month =
    Net income this month
  − Recurring fixed costs
  − Planned variable budgets
  − Open bills due this month
  − Overdue open bills
```

Bill classification (relative to "today", `asOf`), with one rule each:

| Bill state | Counts against available? |
|------------|---------------------------|
| Paid | No |
| Open, due **this month** | **Yes** |
| Open, **overdue** (due before this month, still open) | **Yes** |
| Open, **future** month | No |

13th salary: **never silently averaged.** Default model is `separate` — the 13th
is shown on its own and excluded from monthly net. Only if the user explicitly
chooses the `averaged` model is `13th / 12` added to monthly net income. Every
case is unit-tested.

---

## 7. Security model

Target design (built in the dedicated security phase, **not** Phase 0):

- **Unlock secret:** passphrase or PIN. No automatic unlock by default; no
  recovery backdoor; the user must acknowledge a recovery warning at first run.
- **Key derivation:** Argon2id (memory-hard) preferred, vendored as a small C
  target; documented fallback PBKDF2-HMAC-SHA256 (high iteration count) via
  CryptoKit/CommonCrypto if Argon2 vendoring is deferred.
- **Key hierarchy:** a random 256-bit master data key is generated once and
  stored **only wrapped** by the KDF-derived key (AES-GCM / ChaCha20-Poly1305
  via CryptoKit). The passphrase is never stored.
- **Database at rest:** SQLCipher (page-level AES) is the preferred encrypted
  store. **Tradeoff (flagged):** SQLCipher via SwiftPM means vendoring the
  SQLCipher amalgamation as a C target — non-trivial. **Because Phase 0 persists
  no private data, this decision is deferred without ever storing plaintext.**
  When implemented, the alternative-if-needed is a documented application-layer
  AES-GCM envelope over the store; the choice will be made explicitly, never by
  silently falling back to plaintext.
- **Document vault:** each document encrypted with AES-GCM / ChaCha20-Poly1305,
  random nonce per file, content key wrapped by the master key. Files stored
  under opaque IDs. **Original paths are never persisted and never shown.**
- **Auto-lock:** idle timer + manual lock. On lock, decrypted keys are zeroed
  and dropped from memory as far as Swift/CryptoKit allow.
- **Backup:** encrypted with a **separate backup passphrase** (its own KDF →
  AES-GCM envelope over a serialized export). Portable to another Mac.
- **Restore:** preview first, then an explicit destructive confirmation.

Phase 0 ships a Lock **UI** gate that is honestly labelled as not-yet-encrypted
and stores nothing private — so there is no plaintext private data at rest.

---

## 8. Data model

All money fields are `Int64` Rappen (`Money`). Designed from scratch:

| Entity | Key fields (money = Int64 Rappen) |
|--------|-----------------------------------|
| Household / Profile | name, members, canton |
| Canton | name, abbreviation (e.g. "Zürich", "ZH") — **name + abbr only, no emblem** |
| Income | monthlyNet `Money`, thirteenth model (`separate`/`averaged`) + amount `Money` |
| RecurringFixedExpense | name, monthlyAmount `Money`, category |
| VariableMonthlyBudget | name, plannedAmount `Money`, category |
| OneOffBill | provider, amount `Money`, dueDate, status (open/paid), notes?, documentRef? |
| SavingsGoal | name, target `Money`, monthlyContribution `Money?` |
| Document | type, taxYear?, month?, relevance, importDate, displayName (no path) |
| TaxChecklistItem | key, importance (required/oftenRequired/usefulIfApplicable), done |
| SecuritySettings | autoLock interval, KDF params, lock state (no secrets) |
| AppSettings | language, appearance, sample-data toggle |
| BackupMetadata | created, app version, item counts, KDF/cipher identifiers |

---

## 9. Phased implementation plan

| Phase | Scope | Persists private data? |
|-------|-------|------------------------|
| **0 (now)** | SwiftPM skeleton, brand, sidebar nav, 4-language L10n with parity, money + bill-classification engine, tests, README, `.gitignore` | **No** |
| 1 | Security core: KDF, key wrap, CryptoKit, auto-lock state machine (unit-tested) | No (keys only) |
| 2 | Encrypted persistence: SQLCipher decision executed; repositories behind protocols | Yes (encrypted) |
| 3 | Encrypted document vault | Yes (encrypted) |
| 4 | Full feature screens wired to encrypted repos (Overview/Planning/Bills/Docs/Taxes) | Yes (encrypted) |
| 5 | Backup / guarded restore crypto | Yes (encrypted) |
| 6 | Report-problem composer + redaction polish | No |
| 7 | Packaging (`.app`/`.icns`), QA, accessibility, localization polish | — |

## 10. First coding phase (Phase 0) — concrete deliverables

1. `Package.swift` (SwiftPM, executable + libraries + test target).
2. `KontivaCore`: `Money` (Int64 Rappen, checked arithmetic), CHF
   parse/format (no float), domain entities, bill classification, available-
   balance engine, 13th-salary logic, redaction, four-language L10n tables.
3. `KontivaUI`: design system (brand palette + spacing + type), `KontivaMark`
   faithful vector, branded sidebar, all seven sections as well-structured
   screens with empty states, in-memory (non-persisted) sample data toggle,
   Lock gate UI.
4. `Kontiva`: `@main` SwiftUI app, native window config.
5. `Tests/KontivaCoreTests`: money/format/parse, available-balance formula,
   12-vs-13 salary, all four bill states, redaction, L10n key parity.
6. Brand resources copied (unmodified) into `Resources/`; `.icns` generation
   documented.
7. `README.md` (states clearly: not beta, not V1), privacy + security docs,
   `.gitignore`.

## 11. Risks / blockers

| Risk | Severity | Handling |
|------|----------|----------|
| No full Xcode → no `xcodebuild`, no signing/notarization now | High (for shipping, not for building) | SwiftPM build + manual `.app` bundling; signing/notarization deferred to V1 when Xcode/credentials exist |
| Xcode SDK license not accepted | Medium | Surface the one-time `sudo xcodebuild -license accept`; do not run sudo unprompted |
| SQLCipher via SwiftPM = vendoring effort | Medium | Deferred past Phase 0; Phase 0 persists nothing, so no plaintext risk |
| Argon2id availability in pure SwiftPM | Low/Medium | Vendor C target or documented PBKDF2 fallback |
| Swiss formatting nuances across 4 locales | Low | CHF Swiss convention centralised; locale refinements tracked |
| Money correctness | High (product-critical) | Int64-only, checked ops, tests that fail under floating point |

## 12. Exact quality gates before beta

**Private alpha** (target after Phases 1–4): basic flows work; encryption not
bypassed; no plaintext private data at rest; main calculations tested; app
launches locally.

**Public beta** (gate — all must hold):
- Visual QA passed (resize small↔large, no clipped text, strong branding).
- Smoke test of every section passed.
- Installer/`.app` bundle launches on a clean Mac.
- Backup → restore verified with disposable data.
- No major UX confusion; no known privacy violation; limitations documented.

**V1.0:** polished macOS app; security model reviewed; installer signed +
notarized; backup/restore reliable; honest support + download copy; no major
workflow gaps. Not merely "tests pass."

**Default verdict, always, until genuinely earned: beta-ready = NO, V1-ready = NO.**
