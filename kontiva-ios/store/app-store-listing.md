# Kontiva — App Store Connect listing

Ready-to-paste metadata. Character limits noted in (parentheses). Fill the bracketed
placeholders. Primary market is Swiss‑German, so German (de‑CH) is given alongside English.

---

## App information (not localized)

- **Bundle ID:** `ch.kontiva.app`
- **Primary category:** Finance
- **Secondary category:** (optional) Productivity
- **Age rating:** 4+ (no objectionable content)
- **Price:** Free
- **Privacy policy URL:** `https://mrumy-dev.github.io/kontiva/privacy.html`
- **Support URL:** `mailto:mohamed.rumy@bluewin.ch` (or a simple support page)
- **Marketing URL:** (optional)

## App Privacy ("nutrition label")

Answer **"No, we do not collect data from this app."** → label shows **Data Not Collected**.
(The bundled `PrivacyInfo.xcprivacy` already backs this up.)

---

## English (en)

**App Name** (30) → `Kontiva`

**Subtitle** (30) → `Private budgeting, on-device` *(28)*
- alt: `Your calm Swiss budget` *(22)*

**Promotional text** (170)
> A calm, private home for your Swiss household budget. Everything stays on your iPhone, encrypted — no cloud, no accounts, no tracking. Precise to the Rappen.

**Keywords** (100, comma-separated)
> budget,household,finance,savings,bills,debt,swiss,private,offline,encrypted,money,expenses

**Description** (4000)
> Kontiva is a calm, private home for your Swiss household budget — and it never leaves your iPhone.
>
> No accounts. No cloud. No tracking. Everything you enter is stored on your device in an encrypted vault (AES‑256‑GCM), unlocked by a code you choose and, if you like, Face ID.
>
> SEE WHERE YOUR MONEY GOES
> A clear monthly overview shows exactly what's available after fixed costs, variable spending, bills, and savings — calculated to the Rappen, with no rounding errors.
>
> PLAN YOUR MONTH
> Set your income (including the 13th salary), recurring fixed costs, and variable budgets. Kontiva knows Swiss life: rent, health insurance, Serafe, public-transport passes, Pillar 3a, and more.
>
> STAY ON TOP OF BILLS & DEBTS
> Track one-off bills with due dates, tick them off when paid, and keep an eye on anything overdue — including Betreibung and Verlustscheine — with plain-language guidance on your rights.
>
> SAVE WITH INTENTION
> Set savings goals, track your monthly contributions, and watch your progress grow.
>
> MADE FOR SWITZERLAND, IN YOUR LANGUAGE
> 35 interface languages, Swiss number formatting, and a design that respects your privacy.
>
> PRIVATE BY DESIGN
> • Fully on-device — no servers, no cloud
> • Encrypted with AES‑256‑GCM
> • No analytics, no ads, no tracking
> • Optional encrypted backups you control
> • Export a clean PDF report whenever you need one
>
> Your budget is nobody's business but yours. Kontiva keeps it that way.

**What's New** (first release) → `First public beta of Kontiva. Thanks for testing!`

---

## German — Switzerland (de-CH)

**Name** (30) → `Kontiva`

**Untertitel** (30) → `Privates Budget, lokal` *(22)*
- alt: `Ihr ruhiges Schweizer Budget` *(28)*

**Werbetext** (170)
> Ein ruhiger, privater Ort für Ihr Schweizer Haushaltsbudget. Alles bleibt verschlüsselt auf dem iPhone – keine Cloud, kein Konto, kein Tracking. Rappengenau.

**Schlüsselwörter** (100)
> budget,haushalt,finanzen,sparen,rechnungen,schulden,säule3a,privat,offline,verschlüsselt,geld

**Beschreibung** (4000)
> Kontiva ist ein ruhiger, privater Ort für Ihr Schweizer Haushaltsbudget – und verlässt Ihr iPhone nie.
>
> Kein Konto. Keine Cloud. Kein Tracking. Alles, was Sie eingeben, wird auf dem Gerät in einem verschlüsselten Tresor gespeichert (AES‑256‑GCM), entsperrt mit einem Code Ihrer Wahl und auf Wunsch mit Face ID.
>
> SEHEN, WOHIN IHR GELD GEHT
> Eine klare Monatsübersicht zeigt genau, was nach Fixkosten, variablen Ausgaben, Rechnungen und Sparen verfügbar ist – rappengenau, ohne Rundungsfehler.
>
> DEN MONAT PLANEN
> Erfassen Sie Einkommen (inkl. 13. Monatslohn), wiederkehrende Fixkosten und variable Budgets. Kontiva kennt den Schweizer Alltag: Miete, Krankenkasse, Serafe, ÖV‑Abos, Säule 3a und mehr.
>
> RECHNUNGEN & SCHULDEN IM GRIFF
> Verfolgen Sie einmalige Rechnungen mit Fälligkeitsdatum, haken Sie sie bei Bezahlung ab und behalten Sie Überfälliges im Blick – inklusive Betreibungen und Verlustscheinen, mit verständlicher Orientierung zu Ihren Rechten.
>
> BEWUSST SPAREN
> Legen Sie Sparziele fest, verfolgen Sie Ihre monatlichen Beiträge und sehen Sie Ihren Fortschritt.
>
> FÜR DIE SCHWEIZ GEMACHT, IN IHRER SPRACHE
> 35 Sprachen, Schweizer Zahlenformat und ein Design, das Ihre Privatsphäre respektiert.
>
> PRIVAT BY DESIGN
> • Komplett auf dem Gerät – keine Server, keine Cloud
> • Verschlüsselt mit AES‑256‑GCM
> • Keine Analyse, keine Werbung, kein Tracking
> • Optionale verschlüsselte Backups, die Sie kontrollieren
> • Sauberer PDF‑Bericht, wann immer Sie ihn brauchen
>
> Ihr Budget geht niemanden etwas an ausser Sie. Kontiva sorgt dafür.

**Neuheiten** (Erstveröffentlichung) → `Erste öffentliche Beta von Kontiva. Danke fürs Testen!`

---

## TestFlight — Beta App Review (for external/public testers)

- **Beta App Description**
  > Kontiva is a private, on-device Swiss household budgeting app — everything stays encrypted on your iPhone. Please test onboarding (language + profile + code), adding income/costs/bills/savings, the monthly overview figures, switching language and theme, and locking/unlocking (incl. Face ID). No account needed; nothing leaves the device.
- **What to test / feedback email:** mohamed.rumy@bluewin.ch
- **Sign-in required?** No (the app has no accounts).
- **Demo account:** Not applicable.
- **Notes for the reviewer:** All data is local and encrypted; the app makes no network calls. Standard AES‑256 only (export-exempt — declared in Info.plist).

---

## Screenshots (required for submission)

A captured **6.9"** set (1320 × 2868, iPhone 17 Pro Max, de-CH) lives in
[`./screenshots/`](./screenshots/) — ready to drop straight into App Store Connect:

1. `01-welcome.png`  — welcome / onboarding (the wordmark hero)
2. `02-overview.png` — overview with the donut + "verfügbar diesen Monat" *(hero)*
3. `03-planning.png` — Monatsplanung (income + fixed + variable)
4. `04-bills.png`    — Rechnungen (one open + one paid)
5. `05-savings.png`  — Sparen goal with the progress ring
6. `06-settings.png` — Einstellungen (profile, language, colour theme)

> Re-capture any time with `xcrun simctl io booted screenshot name.png` (no device
> frame needed — Apple accepts raw screenshots). A 6.5" set is optional; the 6.9"
> set above satisfies submission on its own.
