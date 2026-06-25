# Kontiva iOS – Beta (TestFlight) · Vorbereitung & Texte

Alles auf Deutsch. Texte zum Kopieren in App Store Connect + eine Schritt-für-Schritt-
Anleitung zum Start der Beta. Stand: frischer Neustart (App in App Store Connect gelöscht).

---

## 0. App-Eckdaten (bereits im Projekt gesetzt – nichts zu tun)

| Feld | Wert |
|---|---|
| App-Name | **Kontiva** |
| Bundle-ID | `ch.kontiva.app` |
| Version (MARKETING_VERSION) | `0.0.1` |
| Build (CURRENT_PROJECT_VERSION) | `1` |
| Team | `469PBZ2ZVH` (automatische Signierung) |
| Mindest-iOS | 26.0 |
| Geräte | iPhone |

Technisch ist alles für TestFlight vorbereitet:
- ✅ App-Icon inkl. 1024×1024-Marketing-Icon
- ✅ Face-ID-Begründung (`NSFaceIDUsageDescription`)
- ✅ Export-Compliance vordeklariert (`ITSAppUsesNonExemptEncryption = NO`) → **keine Verschlüsselungsfrage beim Upload**
- ✅ Datenschutz-Manifest (`PrivacyInfo.xcprivacy`): kein Tracking, keine Datenerhebung
- ✅ Keychain-Entitlement (für Face-ID-Entsperrung)
- ✅ Release-Konfiguration kompiliert sauber

---

## 1. App Store Connect – App-Informationen (Texte zum Kopieren)

### Name (max. 30 Zeichen)
```
Kontiva
```
> Falls „Kontiva“ belegt ist: `Kontiva – Budget`

### Untertitel (max. 30 Zeichen)
```
Ihr privates Haushaltsbudget
```

### Kategorie
- Primär: **Finanzen**
- Sekundär: **Produktivität**

### Werbetext / Promotional Text (max. 170 Zeichen)
```
Behalten Sie Ihr Haushaltsbudget im Griff – ruhig, klar und komplett privat. Alle Daten bleiben verschlüsselt auf Ihrem iPhone. Keine Cloud, kein Konto, kein Tracking.
```

### Beschreibung
```
Kontiva – Haushaltsbudget, das ganz Ihnen gehört.

Kontiva hilft Ihnen, den Überblick über Ihr Haushaltsbudget zu behalten – ruhig, klar und komplett privat. Alle Daten bleiben verschlüsselt auf Ihrem iPhone. Keine Cloud, kein Konto, kein Tracking.

AUF DIE SCHWEIZ ZUGESCHNITTEN
• 13. Monatslohn in allen gängigen Varianten: ganz im Dezember, im November, 11/12 + 1/12 oder halbjährlich – dazu Sonderzahlungen
• Kategorien wie Krankenkasse, Serafe, ÖV-Abo, Ratenzahlung, Streaming und Kreditkarte
• Schulden im Blick – mit Hinweisen zu Betreibung, Existenzminimum und kostenloser Schuldenberatung

KLAR UND EHRLICH
• Monatsgenaue Übersicht: Einkommen, Fixkosten, variable Ausgaben, Rechnungen und Sparen – auf den Rappen genau
• Sparziele mit Fortschritt; eine kleine Feier, wenn ein Ziel erreicht ist
• Proaktive Hinweise zu Ihrem Budget

PRIVAT BY DESIGN
• 100 % lokal, Ende-zu-Ende verschlüsselt (AES-256-GCM)
• Entsperren mit Face ID oder Code
• Kein Internet nötig

GANZ IHR STIL
• Über 30 Sprachen
• Farbthemen: Voreinstellungen, Verläufe und Zweifarb-Themes – oder ein komplett eigenes Thema

Ihr Geld. Ihre Daten. Ihr Gerät.
```

### Keywords (max. 100 Zeichen, kommagetrennt) — nur für die spätere App-Store-Veröffentlichung
```
Budget,Haushalt,Finanzen,Sparen,Geld,privat,offline,Ausgaben,Lohn,Rechnung,Schweiz,Schulden
```

### Support-URL / Marketing-URL
- Support-URL (Pflicht): eine erreichbare Seite, z. B. `https://kontiva.ch` oder eine simple Notion-/GitHub-Pages-Seite mit Kontakt.
- Marketing-URL (optional): kann leer bleiben.

---

## 2. Datenschutzrichtlinie (Pflicht für externe Tester)

App Store Connect braucht für **externe** TestFlight-Tester eine **Datenschutzrichtlinie-URL**.
Da Kontiva nichts erhebt, genügt ein kurzer Text. Hosten Sie ihn (z. B. GitHub Pages,
Notion „öffentlich teilen", eigene Website) und tragen Sie die URL in App Store Connect →
App-Informationen → Datenschutzrichtlinie ein.

```
Datenschutz – Kontiva

Kontiva erhebt, speichert oder überträgt keine personenbezogenen Daten an uns oder Dritte.
Alle von Ihnen erfassten Finanzdaten bleiben ausschliesslich auf Ihrem Gerät und sind dort
mit AES-256-GCM verschlüsselt. Es gibt kein Benutzerkonto, keine Cloud-Synchronisierung,
keine Analyse- oder Tracking-Dienste und keine Werbung. Die App benötigt keine
Internetverbindung.

Face ID / Touch ID werden ausschliesslich lokal zum Entsperren des verschlüsselten Tresors
verwendet; biometrische Daten verlassen das Gerät nie und werden von Apple sicher verwaltet.

Sprache und Farbthema werden lokal gespeichert, damit sie schon vor dem Entsperren gelten.

Kontakt: <Ihre E-Mail-Adresse>
Stand: Juni 2026
```

> **Tipp:** Für rein **interne** Tests (Team-Mitglieder) ist keine Datenschutz-URL nötig.

---

## 3. TestFlight – Beta-Texte (zum Kopieren)

### Beta-App-Beschreibung
```
Kontiva ist ein privater Haushaltsbudget-Planer für die Schweiz – komplett offline und
auf dem Gerät verschlüsselt. Dies ist eine frühe Beta: Bitte testen Sie auf Herz und Nieren
und melden Sie alles, was unklar, hakelig oder falsch wirkt.
```

### Feedback-E-Mail
```
<Ihre E-Mail-Adresse>
```

### „Was gibt es zu testen?" (What to Test)
```
Vielen Dank fürs Testen von Kontiva! Bitte richten Sie zuerst die App ein und probieren
Sie danach die einzelnen Bereiche aus. Melden Sie alles, was unklar, falsch oder hakelig
wirkt – gerne mit Screenshot (in TestFlight schütteln oder Screenshot teilen).

1. EINRICHTUNG
   • Onboarding durchlaufen, Code festlegen, Face ID aktivieren
   • App schliessen/sperren und wieder entsperren (Code + Face ID)

2. EINKOMMEN (Tab „Monatsplanung")
   • Einkommen erfassen
   • 13. Monatslohn testen: Modell wechseln (Dezember / November / 11-12 + 1-12 / halbjährlich)
   • Sonderzahlung (Bonus) hinzufügen und prüfen, ob sie im richtigen Monat auftaucht

3. FIXKOSTEN / VARIABLE AUSGABEN
   • Einträge erfassen – die Kategorie ersetzt die Bezeichnung (z. B. Streaming, Ratenzahlung, Kreditkarte)
   • Befristeten Dauerauftrag mit Laufzeit testen

4. RECHNUNGEN (Tab „Rechnungen")
   • Rechnung erfassen, als bezahlt markieren – die verfügbare Summe darf sich dadurch NICHT erhöhen
   • Sortierung ausprobieren

5. SPAREN (Tab „Sparen")
   • Sparziel mit Zielbetrag anlegen
   • Ein Ziel auf 100 % bringen → Glückwunsch-Dialog (Ziel erhöhen / abschliessen / weiter sparen)
   • Abgeschlossenes Ziel: zählt es danach nicht mehr in der Übersicht?

6. SCHULDEN (Tab „Mehr" → „Schulden")
   • Schuld erfassen
   • Lange auf eine Schuld tippen → „Als Rechnung / Als Fixkosten" zum Abzahlen

7. ÜBERSICHT & ERKENNTNISSE
   • Donut, Monatswechsel (Pfeile / „Heute")
   • „Erkenntnisse" ansehen

8. FARBTHEMA (Tab „Mehr" → „Einstellungen" → „Farbthema")
   • Voreinstellung wählen → ändert sich die ganze App?
   • „Eigenes Thema" → Farbe(n) + Stil wählen
   • Prüfen: ändern sich auch Sprache- und Sperre-Werte mit der Farbe?

9. SPRACHE
   • Sprache wechseln (z. B. Englisch, Französisch) und durch die App gehen

10. SICHERN
   • „Verschlüsseltes Backup" erstellen und „Wiederherstellen" testen

HINWEIS: Die Daten liegen nur lokal. Geben Sie für den Test ruhig Beispieldaten ein.
```

---

## 4. Schritt-für-Schritt: Beta starten

### A) App ID registrieren + App-Datensatz anlegen
0. **App ID registrieren** (neue Bundle-ID, einmalig): developer.apple.com → **Certificates, Identifiers & Profiles** → **Identifiers** → **+** → **App IDs** → **App** → Description `Kontiva`, **Explicit** Bundle ID `ch.kontiva.app`, **keine** Zusatz-Capabilities → **Register**. *(Bei automatischer Signierung legt Xcode die ID beim ersten Archive sonst selbst an – dieser Schritt ist optional, aber sauber.)*
1. **appstoreconnect.apple.com** → **Apps** → **+** → **Neue App**.
2. Plattform **iOS** · Name **Kontiva** · Primärsprache **Deutsch (Schweiz)** · Bundle-ID **`ch.kontiva.app`** (aus der Liste wählen) · SKU z. B. `kontiva-app-001` · **Vollzugriff**.
3. (Nur für externe Tester nötig) **App-Informationen** → **Datenschutzrichtlinie-URL** eintragen (Text aus Abschnitt 2).

### B) Build hochladen (Xcode)
4. In Xcode `kontiva-ios/Kontiva.xcodeproj` öffnen.
5. Oben das Ziel auf **„Any iOS Device (arm64)"** stellen (NICHT Simulator).
6. Target **Kontiva** → **Signing & Capabilities**: Team = Ihr Team, **„Automatically manage signing"** aktiv.
7. Menü **Product → Archive** (1–2 Min.).
8. Im **Organizer**: Archiv wählen → **Distribute App** → **TestFlight & App Store** (bzw. „App Store Connect") → **Upload** → Standardoptionen bestätigen → **Upload**.
9. Warten, bis der Build unter **TestFlight** von **„Processing"** auf bereit wechselt (ca. 5–15 Min., Apple schickt eine E-Mail).

### C) TestFlight einrichten
10. App Store Connect → Kontiva → **TestFlight**.
11. **Export-Compliance**: dank Vordeklaration kommt **keine** Frage. (Falls doch: „Verwendet die App nicht-exempte Verschlüsselung?" → **Nein**.)
12. Beim Build **„Test Details / What to Test"** den Text aus Abschnitt 3 eintragen.
13. Tester hinzufügen:
    - **Interne Tester** (am schnellsten, sofort, bis 100): nur Personen **mit Rolle in Ihrem App-Store-Connect-Team**. Gruppe „App Store Connect Users" → Build aktivieren.
    - **Externe Tester** (Freunde, bis 10’000): neue **Gruppe** erstellen → Tester per E-Mail oder **öffentlichem Link** einladen. Der **erste externe Build** durchläuft eine kurze **Beta-App-Prüfung** durch Apple (meist < 24 h).
14. Tester bekommen eine Einladung → installieren **TestFlight** aus dem App Store → Einladung annehmen → **Kontiva** installieren.

### D) Feedback
15. Tester können in TestFlight Screenshots + Feedback senden – landet bei Ihrer Feedback-E-Mail.

---

## 5. Hinweise / Stolpersteine

- **„Build-Nummer bereits verwendet"** beim Upload? Dann in `kontiva-ios/project.yml` `CURRENT_PROJECT_VERSION` erhöhen (z. B. auf `2`), `xcodegen generate` ausführen und neu archivieren.
- **Freunde testen** = **externe** Tester ⇒ Datenschutz-URL + einmalige Beta-App-Prüfung nötig. **Team-Mitglieder** = interne Tester, ohne Prüfung sofort.
- **Screenshots** für den App-Store-Eintrag sind für die TestFlight-Beta **noch nicht** nötig (erst für die öffentliche Veröffentlichung).
- Die Texte sind Vorschläge – passen Sie Name/E-Mail/URL frei an.
