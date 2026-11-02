# Politur nach Screenshot-Sichtung

Maßstab: `docs/DESIGN.md`. Format: gefunden → behoben.

## Behoben

| Datei (Bild) | Fund → Behebung |
|---|---|
| `audit_*` (`PermissionAuditScreen.kt`) | Titel doppelt (Überschrift + Label „WER DARF WAS“) → Label durch normalen Hinweistext ersetzt |
| `audit_*` (`PermissionAuditScreen.kt`) | App-Namen/Paketnamen mit „…“ → bis 2 Zeilen, kein Abschneiden |
| `audit_*` (Einstiegskarte, `PermissionAuditScreen.kt`) | Kartenkopf-Stil ≠ andere Karten → Versalien-Label wie übrige Karten |
| `start_*`/`result_*`/`scan_*` Tablet (`Theme.kt`, `MainActivity.kt`, `Tokens.kt`) | Zwei-Spalten-Layout in 640-dp-Scaffold gequetscht, Wörter mitten im Wort umgebrochen („Verdächtig/e Dateien“, „HINTERGRUNDPRÜFU/NG“) → Scaffold für Zwei-Spalten-Screens bis 960 dp |
| `start_*` (`StartScreen.kt`) | Texte abgeschnitten (`maxLines` + Clip/Ellipsis) → Limits entfernt; Tablet-Zusatzpadding entfernt, linke Spalte oben ausgerichtet |
| `result_*` (`ResultScreen.kt`) | abgeschnittene Texte (Verdict, Kontext, Headline, Paketname) → Limits entfernt |
| `result_*` (`ResultScreen.kt`) | „Geprüft mit“/„Geprüfter Zeitraum“ in anderem Label-Stil → Versalien wie übrige Labels |
| `values-de/strings.xml` (t_005, t_032) | falsche Anführungszeichen ‚Root anfragen‘ → „Root anfragen“ |
| `values/strings.xml` (t_005, t_032) | „Request root“ ohne Anführungszeichen → “…” ergänzt |
| `values/strings.xml` (`audit_hint`) | „right right now“ → „currently have each right“ |
| `ScreenshotTest.kt` | untere Startkarten ungeprüft → neue Bilder `start_bottom_*` (Handy absichern) und `start_lower_*` (Wer darf was, Hintergrundprüfung), DE/EN Telefon + Tablet |

## Geprüft ohne Befund
Tausenderpunkte DE (1.384, 4.200) / Komma EN; „Du/Dein“ nur am Satzanfang; keine Sprachmischung.

## Offen / nicht behoben
- Scan-Tablet: große Leerfläche (Test-Viewport 800×1280 hochkant).
- Wizard Schritt 2: leere Fläche (Illustration im Test nicht gerendert); Wizard-Titel kleiner als „Über Spyhound“/Rechte-Prüfer.
- Gedankenstriche „—“ und „–“ gemischt; „…“ mit/ohne Leerzeichen uneinheitlich (DE/EN).
- „metres“ (brit.) vs. US-Datum in EN.
- `ReviewFixesTest.timeoutEndsReadEvenIfAChildKeepsThePipeOpen` ist zeitabhängig und schlug einmalig fehl (danach grün).
