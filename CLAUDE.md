# Spyhound – Arbeitsanleitung

Android-App (Kotlin, Jetpack Compose), die Laien ohne PC prüfen lässt, ob ihr Handy Spuren bekannter
Spionage-Software (Pegasus, Predator …) oder Stalkerware trägt. Ziel: **die** Sicherheits-App für Android.
Verteilung als APK, kein Play Store. Logik nach dem Zero Breach Toolkit (Apache-2.0).

## Zuerst lesen
1. `docs/ROADMAP.md` – Kompass, offene Punkte mit Abnahmekriterium, **Genom** (Lehren, nie löschen). Zu Beginn jeder
   Sitzung lesen, Kompass neu ordnen, Funde noch am selben Tag mit Abnahmekriterium eintragen, Erledigtes mit Beleg
   (Commit, Messwert) abhaken.
2. `docs/SPEC.md` – verbindliche Grundsätze und Architektur.
3. `docs/TESTMATRIX.md` (falls vorhanden) – was auf welchem Gerät/Emulator belegt ist.

## Grundsätze (nicht verhandelbar)
- **Offline.** Der Scan macht keinen Netzwerkzugriff. Netzwerk nur in `IocStore.update()`: nur Allow-List-Repos
  (`FeedCatalog.ALLOWED_REPOS`), nur vom festgelegten Commit, SHA-256 muss passen, keine Redirects.
- **Ehrlich.** Nie „sauber“, sondern „keine bekannten Spuren im geprüften Bereich“. Jede nicht gelaufene Prüfung ist
  ein `Gap` mit `GapFix`. Ein Falschtreffer ist schlimmer als eine Lücke.
- **Funde vs. Hinweise:** `Finding` = veröffentlichter Indikator passt; `Lead` = prüfenswert, nie Alarm.
- **Keine Telemetrie, keine Tracker, keine Werbung**, keine Abhängigkeiten mit Netzwerkzugriff.
- **Zweisprachig:** `res/values` = Englisch (Standard), `res/values-de` = Deutsch. Keine Texte hart im Code.
  Ton: ruhig, kurz, Du-Form / „you“, nicht alarmistisch.
- **Optik:** eigenständiges „Nachtjagd“-Design (Tinte `#0A0E14`, Elfenbein `#F2EBE0`, Bernstein `#FFB547` als
  einziger Akzent, Rot nur für Funde, Teal für „keine Spuren“). Kein Material-Standardlook. Nichts darf springen:
  wechselnde Texte in festen Slots, Animationen nur als Zeichnung (drawBehind/graphicsLayer), nie animierte Größen.

## Bauen und testen
```
./gradlew :app:testDebugUnitTest      # JVM-Tests (scan/, ioc/, bugreport/, dns/ …) – müssen grün sein
./gradlew :app:assembleDebug          # Debug-APK
./gradlew :app:assembleRelease        # signiert nur, wenn ~/.config/spyhound/keystore.properties existiert
```
- `local.properties` mit `sdk.dir=…` anlegen (nicht im Repo).
- Der Release-Schlüssel liegt **nur** auf pop-os in `~/.config/spyhound/` (nicht im Repo, nie committen). In einer
  Cloud-Sitzung gibt es ihn nicht – dort nur Debug bauen.
- Kein Emulator/kein Gerät in der Cloud: UI-Änderungen dort nur bauen und testen; Screenshots/Gerätetests macht
  eine Sitzung auf pop-os (Pixel 11 Pro XL per WLAN-adb, Emulatoren nacheinander, danach `adb emu kill` und
  `./gradlew --stop`).
- Neue Logik braucht einen Unit-Test. Analysen bleiben reine Kotlin-Funktionen ohne `Context`.

## Architektur in Kürze
- `ioc/` – Indikatoren: Parser (STIX 2, MISP-CSV, Domainlisten, Toolkit-TSV), `IocSet`, `IocStore` (Assets + Downloads).
- `assets/iocs/` – mitgeliefert: MVT (MIT), ECHAP (CC BY 4.0), Toolkit-Listen (CC0). Amnesty/Citizen Lab ohne Lizenz →
  nur auf dem Gerät laden. `sources.json` = 29 Quellen mit Commit- und SHA-256-Pins.
- `scan/` – Analysen (Apps, Gerät, SMS, Shell, Text), `ScanEngine`, `Report`, `ReportText`.
- `collect/` – Android-Sammler. `shell/` – Root (`su`) und Shizuku (AIDL-UserService, Ausgabe per Pipe gestreamt).
- `bugreport/` – Fehlerbericht-ZIP-Analyse (Zip-Bomben-/Zip-Slip-Schutz). `dns/` – DNS-Wächter (lokales VpnService).
- `ui/` – Compose: Start (Radar-Knopf), Scan (Radar mit Sektoren), Ergebnis, Assistent „Tiefere Prüfung einrichten“.

## Commits und Veröffentlichung
- Autor immer `Belkis Aslani <belkis.aslani@gmail.com>`. **Keine** KI-, Agenten- oder Tool-Namen in Commits, Code,
  Kommentaren, Doku, PRs oder Releases – auch keine Co-authored-by-Zeilen.
- Kleine Commits, Nachricht englisch, sagt *warum*.
- Nichts veröffentlichen, das nicht nachweisbar etwas bringt (Release-Gate). Öffentliche Schritte (Release, Issues,
  PRs bei Fremdprojekten) nur nach Rückfrage bei Belkis.
- Upstream: PR interplains/Zero-Breach-Toolkit#31 – braucht signierte Commits (siehe Roadmap).

## Sprache und Zusammenarbeit
- Mit Belkis auf Deutsch, knapp, entscheiden statt Optionslisten. Behauptungen belegen (Testausgabe, Screenshot,
  Messwert), nicht „sollte gehen“.
