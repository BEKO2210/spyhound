# Spyhound – verbindliche Spezifikation

Android-App (Kotlin, Jetpack Compose, minSdk 29, targetSdk 36, AGP 9.3.1, Kotlin 2.4.10), die Laien ohne PC prüfen lässt,
ob ihr Telefon Spuren bekannter Spionage-Software (Pegasus, Predator, Graphite …) oder Stalkerware trägt.
Kotlin-Nachbau der Logik von github.com/interplains/Zero-Breach-Toolkit (Apache-2.0). Kein Play Store: Verteilung als APK.

## Grundsätze (nicht verhandelbar)
1. **Offline.** Der Scan macht keinen Netzwerkzugriff. Netzwerk nur in `IocStore.update()`; Quellen nur aus der Allow-List
   (`FeedCatalog.ALLOWED_REPOS`), nur vom festgelegten Commit, SHA-256 muss passen, keine Redirects.
   Zusätzlich nur lokale Sockets zu `127.0.0.1` für die eingebaute ADB-Verbindung (`AdbShell`); die INTERNET-Permission
   wird dafür gebraucht (Loopback-Sockets) und im Manifest begründet. Nie ein Ziel außerhalb des eigenen Geräts.
   Ausnahme, nur auf Anweisung: Der optionale DNS-Wächter reicht nicht blockierte DNS-Anfragen unverändert an den
   DNS-Server des Netzes weiter (ersatzweise Quad9/Cloudflare), wie in LEGAL.md 2.4 beschrieben; der Scan bleibt offline.
2. **Ehrlich.** Ein Ergebnis ohne Funde heißt „Keine bekannten Spuren im geprüften Bereich“, nie „sauber“. Jede Prüfung,
   die nicht lief, erscheint als `Gap` mit `GapFix` (was sie schließen würde).
3. **Drei Stufen, jede addiert:** `AccessLevel.APP` (keine Sonderrechte, das maximal Mögliche), `SHELL` (Shizuku oder eingebaute ADB-Verbindung
   = ADB-Rechte ohne PC), `ROOT` (su oder Shizuku als root). Ohne Root muss trotzdem alles Mögliche laufen.
4. **Funde vs. Hinweise:** `Finding` = veröffentlichter Indikator passt. `Lead` = prüfenswert, nie Alarm (Sideload + Rechte,
   Bootloader offen, alter Patch …). Stalkerware wird anders formuliert als staatliche Spyware.
5. **Keine Telemetrie, keine Tracker, keine Analytics, keine Werbung.** Keine neuen Abhängigkeiten mit Netzwerkzugriff.
6. Sprache der Oberfläche: Deutsch (Englisch folgt, siehe Roadmap). Ton: ruhig, klar, nicht panisch, für Nicht-Techniker.

## Architektur (Ist-Stand, Commit 8ac9fff)
- `ioc/` – `Indicators.kt` (IocSet, Lookup, Host-Suffix-Matching, Shared-Service-Ausschluss), `Parsers.kt` (STIX 2,
  MISP-CSV, domains-txt, csv-domain-ip, Toolkit-TSV; `FeedCatalog`), `IocStore.kt` (Assets + Downloads, SHA-Prüfung).
- `assets/iocs/` – mitgeliefert: MVT (MIT), ECHAP (CC BY 4.0), Toolkit-Kurationen (CC0); `sources.json` = 29 Quellen mit Pins.
  Amnesty/Citizen Lab haben keine Lizenz → nur auf dem Gerät laden.
- `scan/` – reine Analysen (JVM-testbar): `AppAnalyzer`, `DeviceAnalyzer`, `MessageAnalyzer`, `ShellAnalyzer`,
  `TextScanner`; `Report.kt` (Finding/Lead/Gap/CheckResult/Report/Verdict); `ScanEngine` (Orchestrierung, Fortschritt);
  `ReportText` (Bericht zum Teilen).
- `collect/` – Android-Sammler: `AppCollector`, `DeviceCollector`, `SmsCollector`, `StorageCollector`.
- `shell/` – `Shell`-Interface, `RootShell` (su), `ShizukuShell` + `ShellUserService` (AIDL `IShellService`, streamt über Pipe).
  Eingebaute ADB (Android 11+): `AdbShell` (libadb-android, nur 127.0.0.1, mDNS-Portsuche), `BuiltinAdb` (Controller,
  Kopplungs-Benachrichtigung mit RemoteInput), `AdbSetupState`/`AdbSetup` (reine Zustandsmaschine), `AdbKeys` (RSA-Schlüssel
  app-privat, per Keystore umhüllt), `SelfSignedCert`. Auswahl: Root → Shizuku → eingebaute ADB → Basis.
  Abhängigkeiten: libadb-android 3.1.1 (Apache-2.0 gewählt, nur via JitPack, Build-Zeit) und Conscrypt 2.5.3 (Apache-2.0);
  Herkunft in `NOTICE`. Nach Neustart ist WLAN-Debugging aus → Gap `RESTART_BUILTIN_ADB`.
- `ui/MainViewModel.kt` – Zustand (`UiState`), Aktionen: `updateIndicators`, `connectShizuku`, `requestRoot`, `startScan`,
  `cancelScan`, `closeReport`, `refreshPermissions`.
- Tests: `app/src/test/.../CoreTest.kt` (`./gradlew :app:testDebugUnitTest`), 9/9 grün.

## Regeln für alle Beiträge
- Eigener Git-Worktree, eigener Branch, kleine Commits. Autor: `Belkis Aslani <belkis.aslani@gmail.com>`.
  **Keine** KI-/Agenten-/Tool-Namen in Commits, Code-Kommentaren oder Doku. Kein `git push`.
- Vor Abgabe: `./gradlew :app:testDebugUnitTest :app:assembleDebug` grün. Ausgabe im Abschlussbericht zeigen.
- Bestehende öffentliche Signaturen in `ioc/`, `scan/`, `shell/` nicht brechen; Erweiterungen sind erlaubt.
- Stil: wie der umgebende Code (Kotlin official, knappe englische Kommentare, deutsche UI-Texte).
