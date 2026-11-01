# Handoff: eingebaute ADB-Verbindung (Branch `builtin-adb`)

## Was geändert wurde
- `shell/`: `AdbShell` (libadb-android, 127.0.0.1), `BuiltinAdb` (Controller + Benachrichtigung mit RemoteInput),
  `AdbSetupState` (reine Zustandsmaschine), `AdbKeys`, `SelfSignedCert`.
- Shell-Auswahl: Root → Shizuku → eingebaute ADB → Basis (`MainViewModel`). Bestehende Wege unverändert.
- Neues `GapFix.RESTART_BUILTIN_ADB` („Tiefe Prüfung wieder starten“, DE+EN) nach Neustart (WLAN-Debugging aus).
- Assistent: nur Button „Ohne PC einrichten“ + Statuszeile.
- Tests: `AdbSetupTest` (15), `SelfSignedCertTest` (2), `GuidanceTest` erweitert.
- libadb-android 3.1.1 gibt es nicht auf Maven Central, nur über JitPack (Content-Filter auf `com.github.MuntashirAkon`).

## Bedrohungsmodell
- Schlüssel wird nie exportiert oder geloggt; liegt in `filesDir`, nur für die App lesbar, mit nicht exportierbarem
  Keystore-AES-Schlüssel umhüllt (Fallback: Klartext im privaten Speicher). `allowBackup=false`.
- Verbindung nur zu `127.0.0.1`; mDNS-Treffer nur von eigenen IPs. Kein Internetzugriff.
- libadb akzeptiert beim TLS jedes Zertifikat; Schutz = ausschließlich Loopback. Die Bibliothek ist nicht auditiert.
- Risiko: Solange WLAN-Debugging an ist, können andere Geräte im WLAN koppeln, wenn sie den Code kennen. Empfehlung: nach
  der Prüfung ausschalten.
- Ein Angreifer mit Zugriff auf den App-Speicher könnte den ADB-Schlüssel nutzen (Keystore-Hülle hilft nur eingeschränkt).

## Auf dem Pixel prüfen (Android 11+)
1. Entwickleroptionen aktivieren, WLAN verbinden.
2. Spyhound → Tiefere Prüfung einrichten → „Ohne PC einrichten“; Benachrichtigungen erlauben.
3. Einstellungen → Entwickleroptionen → Debugging über WLAN an → „Gerät mit Kopplungscode koppeln“.
4. Spyhound-Benachrichtigung: 6-stelligen Code eintippen. Erwartet: „Verbunden“.
5. Scan starten: Shell-Prüfungen laufen, keine Shell-Gaps.
6. Falscher Code: Fehler, nach max. 5 ungültigen Codes Abbruch, kein Endlos-Retry.
7. Telefon neu starten, Scan: Gap „Tiefe Prüfung wieder starten“. WLAN-Debugging an, erneut starten: Verbindung ohne neuen Code.
8. Ohne WLAN/Kopplungsdialog offen: sinnvolle Fehlermeldung.

## Bekannte Grenzen
- Kein Geräte-/Emulatortest, kein R8-Release-Build geprüft (Keep-Regeln in `proguard-rules.pro`).
- Android 10 (API 29): nicht unterstützt (TLS-Kopplung erst ab 11).
- Stirbt der App-Prozess während der Kopplung, ist der Zustand weg (Benachrichtigung zeigt „verloren“).
- Chip-Label für `AccessLevel.SHELL` heißt „Shizuku“ (`ui/components/Common.kt`); wegen paralleler Designarbeit nicht angefasst.
- `shell:`-Streams liefern keinen Exit-Code, stderr ist in stdout gemischt.
- Ob JitPack im CI ohne Cache auflöst, ist ungeprüft.

## Device test (Pixel 9, Android 17)
- Android 17 hides `development_settings_enabled` from apps (always 0); wireless debugging on now implies developer options.
- libadb never signals the end of a `shell:` stream, so every command is wrapped as `{ cmd }; echo <nonce>$?` and the reader stops at the marker (`MarkerInputStream`). Before this the scan hung at 72 % (logcat) until the timeout.
- Full scan over the built-in connection: 67 s, depth "Erweitert (Shizuku, ADB-Rechte)", 0 findings, only the expected root gap.
- Open: native libs (`libconscrypt_jni`, `libspake2`) are not 16 KB aligned (debug warning dialog on 16 KB devices).
