# Spyhound – Roadmap

## Vision
Spyhound wird **die** Sicherheits-App für Android: ehrlich, offline, für Laien verständlich, für Profis tief. Start mit
Spyware-/Stalkerware-Spuren; danach weitere Schutzfunktionen (Antivirus u. a., siehe „Stufe 4“). (Belkis, 2026-11-01)

## Weg zu 1.0 (festgelegt 2026-11-03)
1.0 heißt: Ein Laie kann Spyhound ohne Hilfe installieren, prüfen und das Ergebnis richtig verstehen, auf den
verbreiteten Geräten, ohne Falschalarm, und jeder kann nachprüfen, dass die APK aus dem Quellcode stammt.
- [x] Kern: Basis/Tief/Root-Prüfung, eingebaute Kopplung ohne PC, ehrliche Funde/Hinweise/Lücken, DNS-Wächter, „Wer
      darf was“, Fehlerbericht-Import, Hintergrundprüfung, DE/EN (Belege: Releases 0.1–0.3.7).
- [x] Vertrauen: reproduzierbare Builds ab 0.3.5, gepinnte Quellen, Positivliste Pixel/Samsung/Xiaomi, Sicherheits-
      Review mit 14 Befunden behoben (0.3.7), Laientest (~10 Durchläufe, Belkis).
- [ ] Gerätebreite: Tief-Scan ohne Absturz und ohne Falschalarm auf je einem Gerät von Google, Samsung (Handy mit
      SIM, inkl. SMS-Pfad), Xiaomi/Redmi und einem Android-10/11-Altgerät. Stand: Pixel 11, Pixel 9, Tab S9 Ultra.
- [~] Bedienhilfen: Bedienbaum auf dem Pixel ausgelesen (Start, Wer darf was, Über, Ergebnis): jedes antippbare
      Element hat einen Text, Ziele ≥ 48 dp (Meldungen nur an Rändern unter Kopfzeile/Statusleiste). Fund behoben:
      Startbildschirm blieb unter offenen Seiten im Bedienbaum („Jetzt prüfen“ erreichbar) → clearAndSetSemantics.
      Offen: echter TalkBack-Durchgang (TalkBack darf ich nicht einschalten; Belkis ~5 Min). Abnahme: jeder Knopf
      vorgelesen, keine Sackgasse.
- [~] Verteilung: F-Droid-Antrag offen: fdroiddata!55616 (2026-11-03, Fork BEKO2210/fdroiddata), Version 0.3.8,
      reproduzierbar mit unserer Signatur (lokal: sauberer Klon von v0.3.8 ohne Schlüssel gebaut, `apksigcopier compare`
      gegen die veröffentlichte APK bestanden). fastlane/ DE/EN, README-Installation. Offen: Pipeline/Review-Fragen
      beantworten. IzzyOnDroid nimmt Anträge nur auf Codeberg an (Konto legt nur Belkis an). Google Play nicht
      (SMS/Alle-Apps-Rechte, Konto mit Gebühr).
- [ ] Feldtest: 3 Personen außer Belkis installieren aus dem Release und melden das Ergebnis (Fehlerbericht-Anleitung
      liegt bereit). Abnahme: 3 Rückmeldungen, alle Befunde in der Roadmap.
- [ ] Release-Kandidat 1.0.0-rc: eine Woche ohne neuen Absturz/Falschalarm, dann 1.0.0.
Schätzung: Arbeit im Repo ~3–5 Tage (TalkBack, F-Droid-Metadaten, README, Fixes aus dem Feldtest). Den Takt geben
Geräte und Menschen vor: SMS-Gerät, Altgerät, 3 Tester, dazu die Woche rc → realistisch 2–3 Wochen bis 1.0.0.

## Nach 1.0 – es wächst weiter (Belkis, 2026-11-03: „nach 1.0 soll es organisch weitergehen“)
1.0 ist ein Meilenstein, kein Ende. Danach gilt derselbe Kreislauf: Funde und Wünsche kommen am selben Tag mit
Abnahme hinein, der Kompass wird jede Sitzung neu geordnet, jedes Release muss nachweisbar etwas bringen.
Saat für danach (wird zum Punkt mit Abnahme, sobald es dran ist):
- Jedes Release frische Spuren: `check-pins.py` + neue Quellen; Positivliste mit neuen Builds (`knowngood-from-ota.py`).
- Virenschutz ausbauen (Stufe 4), sobald lizenzklare Quellen da sind (abuse.ch, YARA).
- Weitere Sprachen nach Bedarf der Nutzer (Rückmeldungen aus dem Feldtest).
- Rückmeldungen aus Issues und Fehlerberichten werden Roadmap-Punkte, nicht Liste im Kopf.

## Kompass (aktuelle Schwerpunkte, neu geordnet 2026-11-05)
1. **Weg zu 1.0** – TalkBack-Durchgang, F-Droid-Metadaten + README-Installation, Feldtest mit 3 Personen.
2. **Virenschutz-Quellen** – abuse.ch hat zugestimmt (2026-11-05): MalwareBazaar-Android-Hashes einbauen, erst nach Belkis’ Konto + Auth-Key im Vault; Positivliste wächst mit jedem Gerät.

## Lokaler Prüfverlauf (2026-11-05)
- [~] Höchstens 30 manuelle/Hintergrund-Prüfungen, neueste zuerst, app-privat und von Backups ausgeschlossen.
      Liste DE/EN, historische Ergebnisansicht, Löschbestätigung und Rückgängig; persönliche Fundstellen entfernt.
      Speicher-, Bedien- und Screenshot-Testfälle ergänzt. Abnahme offen: Unit-Tests, Debug-Build,
      Screenshot-Aufzeichnung/Verifikation und Sichtprüfung der PNGs; hier ausdrücklich nicht ausgeführt.
      Übergabe: [history.md](handoff/history.md).

## Zeitraum „App-Installationen“ (2026-11-05)
- [~] S21 Ultra zeigte „reicht bis 31.12.2008“, Pixel „01.01.2020“ – Platzhalter. Fix f3fdeeb: Daten vor dem Erscheinen
      der Auslieferungs-Android-Version (`ro.product.first_api_level`, −6 Monate) fallen weg; Unit-Test mit beiden Daten
      grün. Abnahme offen: S21 zeigt ein reales Datum (Sperrbildschirm unterbrach den Gerätetest).

## Ergebnis auf kleinen Handys (2026-11-03)
- [x] Zusammenfassung scrollt als erstes Listenelement mit; Kopfbereich blendet leicht aus, Fußbuttons bleiben fest.
      Tablet unverändert. Unit-Tests + verifyRoborazzi grün, 4 Ergebnisbilder (DE/EN, Fund/Hinweis) neu, nichts abgeschnitten
      (ea9ef8d). Signiert aufs Pixel 11 installiert. Belkis auf dem Pixel: „Läuft“ → Release 0.3.12.

## Release 0.3.9–0.3.11 (2026-11-03) – 0.3.9/0.3.10 nur Build-Fixes für F-Droid (verification-metadata für leeren Gradle-Home, kein „Dependency metadata“-Signaturblock); F-Droid-Pipeline 0.3.10 grün, „compared built binary to supplied reference binary successfully“. 0.3.11: Startbildschirm passt auf kleine/gezoomte Handys (Radar skaliert mit Höhe, < 700 dp scrollt alles, beim Scrollen schrumpft Radar auf 55 % und Statusleiste auf Icons, fingergeführt), 3 ungenutzte Rechte aus libadb entfernt, LEGAL/App-Text nennen jede Netzwerknutzung. Belkis auf Samsung-Handy mit Anzeigezoom: „läuft super“. Reproduzierbar (466) + apksigcopier lokal bestanden.

## Release 0.3.8 (2026-11-03) – Bedienhilfen: Startbildschirm unter offenen Seiten für Bildschirmleser ausgeblendet (Bedienbaum aller Hauptseiten auf dem Pixel ausgelesen), fastlane-Store-Texte DE/EN, README-Installation. verify-reproducible: 466 gleich; apksigcopier compare (F-Droid-Prüfung) bestanden. Tab S9 Ultra: Update + Start ohne Absturz; Fix vorher auf dem Pixel geprüft. Download geprüft.

## Release 0.3.7 (2026-11-03) – Sicherheits-Review (Falschtreffer URL/DNS/STIX/CSV, gepinnte kuratierte Listen), Positivliste + Xiaomi (20.841), robuster Fehlerbericht-Import und DNS-Wächter, fette Begriffe in Anleitungen, einfache Sprache, Lizenztexte, „1 Fund“. verify-reproducible: 466 Dateien gleich. Pixel 11: Update über 0.3.6, „Tief“ blieb, Tief-Scan 42 s, 0 Funde · 5 Hinweise · 2 Lücken (wie vorher), „Wer darf was“ < 3 s, 0 Abstürze. Download geprüft. Tab S9 Ultra (One UI 8): Update über 0.3.6, „Tief“ blieb, Tief-Scan 79 s, 58.105 geprüft, 0 Funde · 5 Hinweise · 2 Lücken (vorher 8 Hinweise · 4 Lücken vor den Samsung-Korrekturen), 0 Abstürze. Belkis: „es funktioniert“.

## Release 0.3.6 (2026-11-03) – „Wer darf was“ ~2 s statt ~12 s mit Fortschritt und „Genauer ansehen“, ruhigere Kopplungs-Benachrichtigung, Samsung-Korrekturen (Updater, Einstellungsnamen, Akku), Kopfzeile in einer Reihe, Quellen in „Über“ zugeklappt. verify-reproducible: 464 Dateien gleich. Pixel 11: Test-Build als Update über 0.3.5, Kopplung blieb, tiefe Prüfung 42 s ohne Absturz; 0.3.6 installiert und startet. Offen: Flacker-Test der Benachrichtigung nach Neukopplung (Unit-Tests grün), veröffentlicht auf Belkis' Go: github.com/BEKO2210/spyhound/releases/tag/v0.3.6 (Download geprüft).

## Rückmeldung zur Bildschirmaufnahme 0.3.5 (2026-11-02)
- [x] „Wer darf was“ zeigte ~12 s nur „Apps werden gelesen …“. Ursache: hashte jede APK (GB). Behoben in a1751f2: ohne
      APK-Hashes, Fortschritt „n von m“. Beleg: ~2 s auf dem Pixel 11 (Belkis, Test-Build 4753783, 2026-11-03).
      Kein Zwischenspeicher: ~2 s reichen (Belkis, 2026-11-03); dafür bleibt die Liste nie veraltet.
- [x] „Wer darf was“: auffällige Kombinationen oben unter „Genauer ansehen“ (Regeltabelle `LookRule`, Amber, Text nennt
      harmlose Träger wie Uhr-/Auto-Apps), normale Rechte darunter ohne Warnfarbe; alles an einer Textkante.
      Beleg: 44a0aad, 5418521, PermissionAuditTest, audit_*.png.
- [x] „Über Spyhound“: Quellen zugeklappt („44 Quellen“), gleiche Karte und Zeile wie „Rechtliches“. Beleg: info_*.png,
      info_expanded_*.png.
- [x] Lizenzhinweise in der App: „Über Spyhound“ → „Lizenzen“ (zugeklappt, „4 Lizenztexte“) zeigt LICENSE, NOTICE
      (Quellen, libadb-android, Shizuku, Conscrypt) und die OFL-Texte von Inter und Space Grotesk wortgetreu.
      LicenceAssetsTest hält die Kopien in assets/licenses gleich mit LICENSE/NOTICE im Repo. Beleg: info_licences_*.png.
- [x] Grenze „findet nur bekannte Spuren“ steht im Onboarding, im Ergebnis („Keine Spuren“, nie „sauber“) und als Lücke.

## Sicherheits-Review 2026-11-03 (ioc/, bugreport/, dns/; 14 Befunde, jeder vor dem Fix selbst am Code geprüft)
- [x] Falschtreffer: URL-Pfad wurde kleingeschrieben (/AbC traf /abc) und ein DNS-Label „evil.example“ (ein Label mit
      Punkt) traf evil.example. Jetzt nur Schema+Host klein, Labels nur [A-Za-z0-9-], Name ≤ 255 Byte. Beleg: CoreTest,
      DnsPacketTest (beide vor dem Fix rot), Merge 2026-11-03.
- [x] Fehlerbericht-ZIP: abgelehnter Eintrag umging das Byte-Budget, viele Log-/dumpsys-Abschnitte ohne Gesamtgrenze
      (jetzt 96 MiB), Zeilen > 1 MiB still gekürzt (jetzt Lücke). Beleg: 4 neue BugreportTest-Fälle.
- [x] Englische Analyse-Texte: 3 blieben teils deutsch (Fremdinstallation/„Darf:“, „Diese Apps dürfen …“, WLAN-
      Debugging). TextsEnCoverageTest treibt die echten Analyzer (169 Texte) und schlägt bei Deutsch fehl.
- [x] STIX: `AND`-Ausdrücke werden zu Einzel-Indikatoren, widerrufene Indikatoren zählen (Parsers.kt:56). Abnahme:
      Test mit `domain AND hash` → kein Domain-Treffer allein; `revoked: true` → ignoriert.
- [x] MISP-CSV: mehrzeilige Kommentare in Anführungszeichen werden zeilenweise als Indikator gelesen (Parsers.kt:185).
- [x] Kuratierte Toolkit-Listen werden ohne SHA-256 und ohne KnownGood-Ablehnung geladen (IocStore.kt:64).
- [x] Katalog: `commit` muss 40-stelliger Hex-Wert sein (sonst wäre ein Branch-Name möglich).
- [x] Parser-Speicher: Felder/Zeilen vor dem Anlegen begrenzen (CSV mit Millionen Kommas).
- [x] Fehlerbericht-Import ohne Zeitlimit (hängende Content-Provider-Pipe).
- [x] DNS-Wächter: unbegrenzte Arbeitswarteschlange (DnsGuardService.kt:104); DnsHitStore-Schreibkonflikt bei
      gleichzeitigen Treffern (DnsHitStore.kt:31).
      Beleg zu den sieben Punkten oben: IocHardeningTest (9 Fälle, vorher rot; 8.296 mitgelieferte Indikatoren je Quelle
      unverändert gepinnt), Robustness2Test (Warteschlange 256, 60 gleichzeitige Treffer, Stillstand > 60 s statt
      Gesamtlimit, damit große Berichte nicht als „unlesbar“ enden). Quellenzahl bleibt 44 (kuratierte Listen gehören
      zum Toolkit). Gerätetest offen.
- [x] DNS-Wächter leitet nicht blockierte Anfragen an das Netz weiter (notfalls Quad9/Cloudflare): gewollt und in
      LEGAL.md 2.4 DE/EN genau so beschrieben (Code DnsGuardService.FALLBACK_DNS stimmt überein); SPEC nennt die Ausnahme.

## Galaxy-Test (2026-11-02, Tab S9 Ultra, One UI 8 / Android 16, X910XXS6EZI2)
- [x] Alle 8 Knöpfe der Anleitungen lösen auf die richtige One-UI-Seite auf (`scripts/check-settings-intents.sh`, 812537e).
- [x] Eingebaute Kopplung ohne PC: Code in die Benachrichtigung, „Spyhound – Derzeit verbunden“; tiefe Prüfung „Tief“
      in 64 s, 58.161 geprüft, 0 Funde · 8 Hinweise · 4 Lücken, kein Absturz.
- [x] Fund: Deutsche Namen stimmten weder auf Samsung noch auf Pixel: „Debugging über WLAN“ und „Gerät über einen
      Kopplungscode koppeln“ (aus SettingsGoogle- und SecSettings-Ressourcen belegt). Behoben 0b1a176.
- [x] Fund: Samsung-Uhr und Galaxy-Buds-Plugins als „nicht aus App-Store“ gemeldet – Installer `updatecenter` und
      `budsunitemgr` (System-Apps) fehlten. Behoben dc7fc24, Test `samsungSystemUpdatersAreNotSideloading`.
- [x] Fund: Fertig-Seite nach eingebauter Kopplung sprach von Shizuku. Behoben dc7fc24.
- [x] Fund: Samsung nennt die Akku-Stufe „Nicht eingeschränkt“, nicht „uneingeschränkt“. Behoben.
- [x] Auto Blocker, „Sicherheit und Datenschutz“, „Eingeschränkte Einstellungen zulassen“: Bezeichnungen stimmen (SecSettings de).
- [x] Assistent wartet 5 Min. auf den Kopplungscode. Beleg: Belkis hat die Einrichtung rund zehnmal durchlaufen, ohne
      Timeout (2026-11-03). Nur bei echten Rückmeldungen wieder öffnen.
- [x] Kopplungs-Benachrichtigung wurde alle 1,5 s neu gepostet (lautlos). Abnahme: nur bei Zustandswechsel neu posten.
      Code: f07529f, Test ceec635. Pixel 11 mit 0.3.6, 2026-11-03 (Mitschnitt `notification_enqueue`): 5 Posts in
      ~2,5 min statt ~100 – 2 min Warten ohne Post, 18 s Code-Eingabe ohne Post, je ein Post nur beim Öffnen des
      Kopplungsdialogs und beim Koppeln. Danach „Tief“ über die eingebaute Kopplung.
- [x] Tablet-Kopf einzeilig: Logo, DE/EN und Info in einer Zeile, sobald die Spalte ≥ 320 dp breit ist. Beleg:
      start_de_tablet.png, verifyRoborazziDebug grün.
- [ ] SMS-Rechte: Tab ohne Telefonie – SMS-Pfad braucht ein Galaxy-Handy (S24 Ultra, nur mit Freigabe der Besitzerin).

## Tagesrunde 2026-11-02 (17:40)
- [x] Positivliste um echte Systemdateien erweitert: 1450 SHA-256 aller APK/JAR/ODEX/VDEX/.so eines Pixel 11 Pro XL
      (Android 17, CD1A.261005.003), Asset `iocs/knowngood-pixel.sha256` (heute `knowngood-system.sha256`) (166 KB). Eine Quelle, die eine davon listet,
      fliegt ganz raus. Beleg: 0 Überschneidung mit den 161 Malware-Hashes; 243 Tests grün; Release auf Pixel 11:
      Start ohne Absturz, 44/44 Quellen gültig, Tief-Scan 26 s, 0 Funde, 0 Zeilen im Crash-Log.
- [x] Scan-Schritt-Text (behoben: erst aus, dann ein, je 80 ms; Wechsel höchstens alle 400 ms. Beleg Pixel 11: 40
      Serienaufnahmen ohne Überlagerung, verifyRoborazzi grün, 0 Abstürze): Beim Wechsel überlagern sich alter und neuer Untertitel sichtbar („Checking file fingerprints…“
      über „File list…“, Screenshot Pixel 11, 17:38). Abnahme: Überblendung ohne lesbare Überlagerung (alter Text erst
      aus, dann neuer ein, ≤ 200 ms), Screenshot-Test unverändert.
- [x] Positivliste um Pixel 9 (Android 17, CP3A.261005.005) ergänzt: +1435 → 2885 Hashes. Beleg: 0 Überschneidung mit
      allen 44 Quellen (3675 Hashes mitgeliefert + 7 aus 14 Download-Quellen, am gepinnten Commit geladen), Tests grün.
- [x] Positivliste um Samsung ergänzt: Galaxy S24 Ultra (One UI, Android 16, +3615) und Galaxy Tab S9 Ultra (+4330) →
      10830 Hashes, Asset umbenannt in `knowngood-system.sha256` (1,2 MB, im APK 0,5 MB). Beleg: 0 Überschneidung mit allen
      44 Quellen, Tests grün, Release-Start Pixel 9 ohne Absturz; Pixel 11: 44/44 Quellen, Tief-Scan 0 Funde · 5 Hinweise · 2 Lücken (wie vorher), 0 Abstürze. Geräte nur lesend per adb, danach getrennt.
- [x] Positivliste um Xiaomi ergänzt, ohne Gerät: offizielle Voll-OTAs (EEA, HyperOS 3/Android 16) von Redmi Note 14
      Pro+, Xiaomi 15, POCO F7, Xiaomi 14 über `scripts/knowngood-from-ota.py` (lädt per HTTP-Range nur die Partitionen,
      prüft jede Operation und jedes Abbild gegen die Hashes im Payload-Manifest) → +10.011, gesamt 20.841. Beleg: 0
      Überschneidung mit allen 44 Quellen (Test + 7 Hashes der Gerät-Quellen), Tests grün.
- [ ] Positivliste OnePlus/Motorola: keine stabile Hersteller-Quelle (OnePlus-Links signiert und ablaufend, Motorola
      nur per Login-Werkzeug/inoffizielle Spiegel). Abnahme: Hersteller-URL belegt oder echtes Gerät per adb.

## Release 0.3.2 (2026-11-02) – Virenschutz Schritt 2: 161 Malware-Hashes (ESET 14 Kampagnen + Meta), Positivliste, 0 Falschtreffer auf 703 APKs (Pixel 9), Datei-Fund offline belegt (Emulator, Flugmodus). Offen: abuse.ch/YARAify anfragen, Positivliste um echte Systembibliotheken erweitern.

## Release 0.3.1 (2026-11-02) – offiziell nach Abnahme durch Belkis auf dem Pixel 11 Pro XL („funktioniert“): Virenschutz Schritt 1, DNS-Tracker/Phishing, Quellenzähler-Fix.

## Offen (Tagesrunde 2026-11-02)
- [x] Screenshot-Test der Info-Seite hängt von der Versionsnummer ab (behoben: Test zeigt fest 0.0.0, Beleg versionName 9.9.9 → verify grün) (jede Version = 4 neue Bilder). Abnahme: Version im
      Test fest „0.0.0-test“, Bilder ändern sich beim Hochzählen nicht.
- [x] Virenschutz-Gerätetest A3/A5/A6: APK in Downloads erkannt, Scan im Flugmodus, 25 s (Emulator) – Belege in docs/ANTIVIRUS.md. Positivliste: läuft (Copilot, weitere Quellen).

## Offen nach der Nacht (2026-11-02, 02:45)
- [x] **Gerätetest Pixel 9/11** (Pixel 9: Release-Start ohne Absturz; Pixel 11: Belkis „Läuft“, 2026-11-02) für alles aus den Nachtrunden 1–5 (Assistent ohne PC, Querformat, Übergänge, Handy
      absichern, Zeitraum, Wer darf was, Hintergrundprüfung). Danach Release 0.2.0 (APK + Notizen liegen bereit in
      ~/StudioProjects/spyhound-release/v0.2.0/).
- [x] Tablet-Start: linke Spalte zeigt Radar und Panel klein oben, darunter leer. Abnahme: Radar skaliert mit der
      Spaltenhöhe, Inhalt vertikal zentriert, keine leere Hälfte (Screenshot start_de_tablet).
- [x] Tablet-Scan: viel Leerfläche (handoff-polish.md). Abnahme wie oben.
- [x] Assistent-Titel kleiner als andere Bildschirmtitel; Gedankenstriche uneinheitlich (– vs —). Beleg: Tagesrunde 1.
- [x] Test `ReviewFixesTest.timeoutEndsReadEvenIfAChildKeepsThePipeOpen` einmal zeitabhängig rot. Abnahme: 20 Läufe
      grün oder Test deterministisch gemacht.

## Nachtrunde 4 (2026-11-02, ab 02:00) – Beleg 295ff6d: 173 Tests, verifyRoborazzi + Release grün, Gerätetest offen
- [x] Rechte-Prüfer „Wer darf was“: Apps je Recht-Gruppe, lokaler Verlauf „seit letzter Prüfung neu“. Abnahme: reine
      Auswertung + Tests, Bildschirm DE/EN, Screenshot-Test.
- [x] Prüfung im Hintergrund (WorkManager, wöchentlich, nur Basis, Standard aus), Benachrichtigung nur bei neuem Fund.
      Abnahme: Worker-Test, Schalter DE/EN, keine Netzwerkzugriffe.
- [x] Release 0.2.0 vorbereiten → veröffentlicht als 0.2.2 (0.2.0 Startabsturz, 0.2.1 Fix, 0.2.2 SMS-Hilfe) – veröffentlichen erst nach Gerätetest auf Pixel 9/11.

## Nachtrunde 3 (2026-11-02, ab 01:50) – Beleg 491530d: 156 Tests, verifyRoborazzi + Release grün, Gerätetest offen
- [x] Barrierefreiheit-Durchgang (s. u.). Abnahme: TalkBack-Labels aller Bedienelemente, Tippflächen ≥ 48 dp, Kontrast AA
      (Liste mit Messwerten), Screenshots unverändert oder bewusst neu aufgenommen.
- [x] Geräte-Härtung „Handy absichern“ (Gerätetest offen): Checkliste mit Direkt-Knöpfen (Bildschirmsperre, USB-/WLAN-Debugging aus wenn
      nicht genutzt, Apps mit Erlaubnis „unbekannte Apps installieren“, Sicherheitspatch-Alter, Entwickleroptionen).
      Abnahme: reine Bewertungsfunktion mit Tests, Karte auf dem Start, DE/EN, Screenshot-Test.
- [x] Zeitreichweite (Stufe 3): wie weit reichen SMS/Logs/Dateien zurück; im Ergebnis ehrlich anzeigen. Abnahme: reine
      Funktion + Tests, Zeile „Geprüfter Zeitraum“ im Ergebnis und im geteilten Bericht.

## Nachtrunde 2 (2026-11-02, ab 01:35) – Beleg a8bcc48: 139 Tests, verifyRoborazzi grün, Release-Build grün
- [x] Übergänge Start → Scan → Ergebnis (gemeinsames Radar, Überblendung + 16 dp). Abnahme: kein Sprung, reduzierte
      Bewegung = sofortiger Wechsel, Tests grün.
- [x] JVM-Screenshot-Tests (Roborazzi + Robolectric) für Start, Scan, Ergebnis, Assistent – DE/EN, Telefon/Tablet.
      Abnahme: PNGs im Repo, `./gradlew verifyRoborazziDebug` grün. Grund: UI ohne Gerät nachts prüfbar.
- [x] Barrierefreiheit-Durchgang (erledigt in Nachtrunde 3, 491530d) (DESIGN §1.6): TalkBack-Labels aller Knöpfe/Chips/Karten, Tippflächen ≥ 48 dp,
      Kontrast AA. Abnahme: Liste der Funde + Fixes, Unit-/Screenshot-Test wo möglich.

## Offen aus Gerätetests (2026-11-01/02)
Beleg Nachtrunde 1: 1186e7d, 114 Tests grün, Release-Build mit Prüfsummen grün. **Gerätetest auf dem Pixel 9 offen** (Gerät nachts gesperrt) – morgens nachholen.
- [x] Einführung: Seitenpunkte ungleich verteilt (Lücke nach der breiten Pille kleiner). Abnahme: gleiche Abstände
      Mitte-zu-Mitte, Pixelmessung.
- [x] Start-Radar zeigt >100 graue Punkte vom letzten Scan → unruhig. Abnahme: nur Funde/Hinweise sichtbar, Rest als
      zarter Schimmer (≤ 15 % Deckkraft).
- [x] Systemprotokolle (logcat) brauchen lange ohne sichtbaren Fortschritt. Abnahme: Zähler/Fortschritt im Schritt,
      Obergrenze 45 s, danach ehrliche Lücke statt Hängen.
- [x] Assistent zeigt unter Android 17 „Entwickleroptionen“ evtl. fälschlich als offen (Flag vor Apps verborgen).
      Abnahme: WLAN-Debugging aktiv ⇒ Schritt abgehakt (Test).
- [x] DNS-Wächter: JVM-Tests für Upstream-Auswahl (eigene Adresse ausgeschlossen, Link-Local zuletzt). Abnahme: Tests grün.
- [x] Release-Build-Abhängigkeit libadb von JitPack: Prüfsumme pinnen (Gradle dependency verification). Abnahme:
      `gradle/verification-metadata.xml` mit SHA-256 für libadb/conscrypt, Build grün.

## Erledigt 2026-11-02 (Release-Runde)
- [x] Eingebauter Prüf-Dienst ohne PC und ohne Shizuku. Beleg: 8cc3f73; Pixel 9 (Android 17): Kopplung per
      Benachrichtigung, Tiefen-Scan 73 s (Debug) / 58 s (Release, R8), 0 Abstürze; Pixel 11 Pro XL: Kopplung und Scan durch
      Belkis abgenommen; nach App-Update sofort wieder „Tief“ ohne neue Kopplung.
- [x] UI/UX-Durchgang: Statuspanel, Karten, Scan- und Ergebnisbildschirm, Einführung. Beleg: 1532fe2, f7a1d2c; Abnahme
      Belkis auf Pixel 11 Pro XL („Läuft alles“).
- [x] DNS-Wächter legte DNS lahm und ließ sich nicht abschalten – behoben, gemessen auf Pixel 9. Beleg: f71bb84.
- [x] Release nur ARM (15 MB statt 20 MB). 16-KB-Seiten: alle `.so` mit `LOAD 0x4000`; RELRO-Warnung nur bei Debug-Builds.

## Stufe 1 – Fundament
- [x] Indikator-Engine: STIX 2, MISP-CSV, Domainlisten, Toolkit-TSV; Host-Suffix-Matching; Shared-Services ausgenommen.
      Beleg: 8ac9fff, CoreTest 9/9, 7.547 mitgelieferte Indikatoren.
- [x] Analysen: Apps (ID, APK-SHA-256, Zertifikat, Sideload+Rechte), Gerät (Props, Patch, Bootloader, Root, Debugging),
      SMS-Links, Shell (Prozesse, alle Pakete, logcat/dumpsys, Dateien, Hashes). Beleg: 8ac9fff.
- [x] Zugriffsstufen APP / SHELL (Shizuku) / ROOT mit Lücken-Meldung je fehlender Stufe. Beleg: 8ac9fff.
- [x] Oberfläche (Start, Scan, Ergebnis) – eigenständige, hochwertige Optik. Abnahme: Screenshots aller Zustände, kein
      Material-Standardlook, Belkis-Abnahme.
- [x] App-Icon + Spürhund-Maskottchen (Vektor). Abnahme: adaptive Icon, hell/dunkel.
      Beleg: `hound_mark`, Adaptive Icon (Grund, Vordergrund, Monochrom), Spot-Bilder, `docs/brand`, `:app:assembleDebug`.
- [x] Elfenbein vereinheitlichen (2026-11-02: Code und Vektoren nutzen nur `#F2EBE0`, BRAND.md angeglichen): die Oberfläche nutzt `#F2EBE0`, die Marke `#F3EEE3`.
      Abnahme: eine Elfenbeinfarbe in Theme und Vektoren.
- [x] Indikator-Update auf dem Gerät. Beleg: Pixel 11 Pro XL (Android 17), 29/29 Quellen, 12.566 Indikatoren, 2026-11-01.
- [x] Erster echter Scan (Basis). Beleg: Pixel 11 Pro XL, 16 s, 15.208 Elemente, 0 Funde, 5 plausible Hinweise, 2 Lücken.
- [x] Shizuku-Tiefenprüfung auf echtem Gerät. Beleg: Pixel 11 Pro XL, 43 s, 18.228 Elemente, 0 Funde nach Fix eines
      Falschtreffers (com.microsoft.familysafety aus `queriesPackages`).
- [x] Shizuku nach Neustart/Update der App automatisch neu verbinden, wenn Freigabe besteht. Abnahme: nach `adb install -r`
      steht die Stufe ohne Tippen auf Shizuku. Beleg: Pixel, e3bfe51.
- [x] Scan-Bildschirm ohne Springen. Beleg: Pixel, 6 Aufnahmen à 300 ms unterhalb der Detailzeile pixelgleich (f528096).
- [x] Logo v2 (runder Spürhund, Witterungsbögen), Belkis-Wahl „A“. Beleg: Pixel-Kopfzeile, Launcher im Emulator (b3a36af).

- [x] Einführung beim ersten Start + Info-Knopf „Über Spyhound“ (was/was nicht, Datenschutz, Quellen, Hilfe). Abnahme:
      Screenshots de/en, Belkis-Abnahme. (Wunsch Belkis 2026-11-01) Beleg: 29feeaa, docs/shots/intro-*, info-*.
- [x] Spot-Illustrationen (illu_*.xml) im Stil von Logo v2 (erledigt in 5c4173f, Render-Kontrolle 2026-11-02: alle fünf v2) – Einführung zeigte den kantigen v1-Hund.
- [x] Englische Oberfläche + Sprachwahl. Beleg: Cherry-Pick der i18n-Commits, 58 Tests, docs/shots/en-*.

- [x] UI/UX-Durchgang (erledigt: 1532fe2, f7a1d2c, Abnahme Belkis „Läuft alles“) (Cloud-Sitzung): übersichtlicher, animierter, selbsterklärend in DE+EN; u. a. weiche Ausblendung
      an der Scroll-Kante unter dem festen Kopfbereich (Abschnitte wirken dort „abgeschnitten“). Abnahme: Belkis testet
      auf dem Pixel.
      - Klar sichtbar, **was genutzt wird**: Prüftiefe (Basis/Shizuku/Root) und welche Quellen/Rechte aktiv waren (SMS,
        Dateien, Indikator-Quellen x/29, DNS-Wächter) – auf Start und im Ergebnis als übersichtliche Statusleiste mit
        Symbolen statt Fließtext. (Belkis 2026-11-01)
      - Phase A Start/Fundament: Tokens, Bewegung, Haptik und fünf Statuschips gebaut; 61 Unit-Tests und Debug-Build
        grün (2026-11-01). Offen: DE-/Chip-Screenshots und Pixelvergleich unter dem Radar. Auf pop-os fehlt `/dev/kvm`;
        beide x86_64-AVDs zeigen bei Software-Emulation wiederholt System-UI-ANR. Abnahme: DE/EN/Chip-Aufnahmen
        ohne Systemdialog, drei Aufnahmen im Abstand 300 ms unter dem Radar pixelgleich.
      - Scan-Bildschirm (§3/§4): Sweep-Nachleuchten, Punkt-Aufleuchten, monotone Prozentzahl, Häkchen-Pfad, Shimmer,
        Haptik, Abbrechen fest, zwei Spalten ab 600 dp. Abnahme: Pixel DE/EN, drei Aufnahmen im Abstand 300 ms
        pixelgleich (nichts wandert), Querformat mit Radar links.
- [x] Shell-Probe schlägt fehl → einmal automatisch trennen und neu verbinden (Shizuku-Dienst), erst dann Lücke melden.
      Fund: Pixel 2026-11-01, veralteter Dienst nach App-Update; App-Neustart behob es. Abnahme: Update der App bei
      laufendem Shizuku → Tiefenprüfung läuft ohne Neustart. Code fertig (281c49b, `probeWithReconnect` + Test),
      Abgenommen 2026-11-02 22:42 auf dem Pixel 11: `adb install -r` 0.3.5 bei laufendem Shizuku, App ohne Neustart
      → Tiefe Prüfung „Tief“, 0 Funde · 5 Hinweise · 2 Lücken (Virenschutz-Umfang, Root), keine Shell-Lücke, kein Absturz.

## Stufe 2 – Tiefe ohne Root
- [x] Fehlerbericht-Import, Analyse-Schicht: `bugreport/` liest bugreport-*.zip (Prozesse, Pakete inkl.
      Hidden/deinstalliert, dumpsys, logcat, getprop) und füttert die Shell-Analysen.
      Beleg: BugreportTest 7/7, `:app:testDebugUnitTest` 16/16.
- [x] Fehlerbericht in der Oberfläche (Teilen/Auswählen). Beleg: 69ca581. Offen: Abnahme mit echtem Bericht (Pixel).
- [x] DNS-Wächter gebaut (e91f5d7, 6 Tests). Offen: Abnahme auf Gerät mit Test-Domain.
      löst Warnung aus, kein Datenabfluss.
- [x] Geführter Assistent „Tiefere Prüfung einrichten“ (Checkliste hakt sich selbst ab, Direkt-Knöpfe). Beleg: 6764331, e62c904.
- [x] Assistent ohne Hilfe bis „bereit“. Beleg: Belkis rund zehn Durchläufe auf Pixel und Tab (2026-11-03); ein
      eigener Laientest entfällt auf seinen Entscheid. Echte Nutzer-Rückmeldungen ersetzen ihn.

## Upstream (Zero Breach Toolkit)
- [x] PR #31 „Android: package names in <queries> declarations are not install traces“: Fix ist upstream in 2.0.0
      und 2.0.1 ausgeliefert, mit Nennung im Changelog; der Betreuer hat ihn als eigenen signierten Commit übernommen
      und den PR geschlossen (gh, 2026-11-03). Signierte Commits sind damit für uns kein Blocker mehr.
- [x] Issue: Spyhound als Begleit-App vorgeschlagen: interplains/Zero-Breach-Toolkit#40 (2026-11-03), Antwort offen.

## Testmatrix (Emulatoren, strikt nacheinander – RAM 15 GB)
- [x] Pixel 11 Pro XL, Android 17, echt: Basis + Shizuku. Beleg: siehe Stufe 1.
- [x] API 29 (minSdk), 31, 34: Start, Scan Basis, Testspy rot, keine Abstürze. Neu belegt 2026-11-03 mit Assistent und
      eingebauter Kopplung (TESTMATRIX.md). API 37 im Emulator instabil, auf echten Pixeln belegt.
- [x] Shizuku im Emulator: Ursache war der Testablauf, nicht die Verbindung. Der Server lief, aber das Skript tippte nur
      „Scan“ und fragte nie die Shizuku-Erlaubnis an; außerdem blockierte Schritt 2 („Entwickleroptionen“), weil das
      Flag im Emulator aus ist. Behoben: Assistent zählt laufendes Shizuku als Schritt 2 erledigt (DeepCheckSetupTest),
      Matrix geht wie ein Nutzer über „Alternative: Shizuku“ → „Spyhound erlauben“ → Shizuku-Dialog. Ergebnis API 34:
      Tief, 1.360 geprüft, Testspy rot, keine Abstürze. Beleg: api34-shizuku-*.png.
- [x] Fund dabei: Schritt-Titel im Shizuku-Teil des Assistenten waren hart deutsch (auch in englischer App). Jetzt
      Ressourcen `deep_step_*` DE/EN.
- [x] Tablet-Profil (Pixel Tablet, API 36): Spalte zentriert ≤ 640 dp (868 px ≤ 1392 px), Testspy rot. Neu belegt 2026-11-03
      mit dem Stand nach 0.3.6. Beleg: api36-tablet-*.png.
- [x] Root: Emulator mit Magisk (rootAVD), Stufe ROOT, Testspy rot. Neu belegt 2026-11-03.
- [ ] Magisk-Freigabedialog: im Emulator nicht prüfbar. Versuch 2026-11-03: Magisk-App 31.1 nachinstalliert, „Zusätzliche
      Einrichtung“ + Neustart, `root_access=3` – Magisk lehnt **jede** su-Anfrage nach ~10 ms ohne Dialog ab, auch die
      aus der adb-Shell (`su: request rejected (2000)`), also kein Spyhound-Fehler. Spyhound fragt `su -c id` und wertet
      `uid=0`; mit gesetzter Magisk-Freigabe läuft „Root“ (oben). Abnahme: echtes Root-Gerät zeigt den Dialog,
      „Gewähren“ → Stufe Root. Braucht ein Gerät mit offenem Bootloader (Pixel 11 nicht, Entsperren löscht alles).
- [x] Testfund: Emulator mit installierter harmloser Test-APK (`testapp`, Paketname/Zertifikat in `debug-test.tsv`) →
      „Testspy“ erscheint rot; Release-Build zeigt ihn nie. Beleg: matrix-emulator-test.sh (check_finding, release-Stufe),
      TESTMATRIX 2026-11-03: API 29/31/34 und Root rot.
- Samsung One UI (Auto Blocker, eigene Menüs) lässt sich im Emulator nicht nachstellen – auf dem Tab S9 Ultra geprüft
  (2026-11-02, siehe Galaxy-Test); SMS-Pfad offen.

- [x] Eingebauter Prüf-Dienst statt separater Shizuku-App (erledigt: 8cc3f73, Pixel 9 + 11): Kopplung per WLAN-Debugging direkt in Spyhound (libadb-android,
      Apache-2.0/GPL-3.0 dual; Shizuku Apache-2.0, API MIT → NOTICE), eigener Dienst per app_process mit ADB-Rechten.
      Abnahme: Pixel ohne installierte Shizuku-App erreicht Stufe „Tief“; nach Neustart ein Tipp zum Neustarten.

## Stufe 3 – Qualität und Reichweite
- [x] „Zeitreichweite“ wie im Toolkit (491530d): wie weit reichen SMS/Logs zurück, überlappt das die Kampagnenzeit?
- [x] Signierte Release-APK + SHA256SUMS (v0.1.0, 2026-11-02).
- [x] Reproduzierbarkeit (2026-11-02): v0.3.4 aus frischem Klon (anderer Pfad) neu gebaut → 463 Dateien byte-gleich, auch
      unsigniert (ohne Schlüssel). Einzige Abweichung: eingebettete Git-Revision, weil die veröffentlichte APK vor dem
      Commit gebaut wurde. Prüfskript `scripts/verify-reproducible.sh`, README DE/EN. Abnahme ab 0.3.5: Skript meldet
      REPRODUCIBLE.
- [x] Neueste Indikatoren: entschieden, **keine** ungepinnten Downloads (bräche „nur fester Commit + SHA-256“; eine
      vergiftete oder kaputte Liste könnte Falschtreffer erzeugen). Stattdessen prüft `scripts/check-pins.py`, ob eine
      gepinnte Datei upstream neuer ist (Git-Blob-Vergleich, ohne Download); `--apply` zieht Pin, SHA-256 und
      mitgelieferte Kopie nach. Frische Indikatoren kommen so mit jedem Release. Stand 2026-11-03: 47 von 47 aktuell.

## Stufe 4 – Weitere Schutzfunktionen (Ideen, jeweils mit Abnahme vor Start präzisieren)
- [~] Antivirus offline – Schritt 1+2 erledigt (161 Hashes ESET+Meta, 0 Falschtreffer, Gerätetest A3/A5/A6, Positivliste 10830; Release 0.3.2/0.3.5). Offen: nur noch weitere lizenzklare Quellen (abuse.ch anfragen, YARAify prüfen). Ursprünglich: APKs und Downloads gegen öffentliche Malware-Hashlisten (z. B. MalwareBazaar-Exporte) und
      YARA-Regeln prüfen; Lizenz jeder Quelle prüfen. Abnahme: EICAR-ähnliche Test-APK wird erkannt, 0 Falschtreffer
      auf Pixel.
- [x] Rechte-Prüfer „Wer darf was“: 11 Rechte-Gruppen, Systemapps ausblendbar, Verlauf lokal, „neu seit letzter Prüfung“.
      Beleg: PermissionAuditTest, Screenshots audit_*; auf dem Pixel abgenommen (0.3.6, ~2 s).
- [x] Netzwerk-Wächter ausbauen – gebaut und geprüft (Emulator: Auflösen/NXDOMAIN/Aus; Release 0.3.1/0.3.4 von Belkis abgenommen). War: DNS an, Phishing-Testdomain → Warnung; Blockieren → NXDOMAIN; Aus → normales DNS (Lehre vom 2026-11-01!). Erst danach Release.
- [x] Geräte-Härtung: Checkliste mit Direkt-Knöpfen (491530d) (Bildschirmsperre, Play Protect, unbekannte Quellen, Updates).
- [x] Zeitgesteuerte Prüfung im Hintergrund (WorkManager) mit Benachrichtigung bei neuem Fund (295ff6d).

- [x] Finanzierung ohne Bezahlschranke – entschieden von Belkis 2026-11-01: Spenden über GitHub Sponsors (FUNDING.yml,
      README, Info-Bildschirm). Später optional „Pro“. Ursprünglicher Vorschlag: Prüfung immer kostenlos; Spenden
      (GitHub Sponsors/Ko-fi); optional „Pro“ mit Komfortfunktionen (Hintergrundprüfung, Verlauf, PDF) per Offline-Lizenz-
      schlüssel, kein Server. Kein Bezahlen pro Scan (bricht „offline“, schreckt gefährdete Menschen ab, bei Apache-2.0
      leicht zu umgehen).

- [x] Rechtstexte-Prüfung 2026-11-02 (Compliance-Check, Fakten an Primärquellen geprüft): 10 Funde behoben in LEGAL.md
      DE/EN – Aufsichtsbehörde BW umgezogen (Heilbronner Str. 35, seit 22.12.2025), neue Funktionen fehlten (eingebauter
      Prüf-Dienst/WLAN-Debugging, Hintergrundprüfung, Wer-darf-was-Verlauf, Domainlisten, Sperren = lokale Antwort),
      Berechtigungen ACCESS_NETWORK_STATE/POST_NOTIFICATIONS/VIBRATE ergänzt, § 25 Abs. 2 Nr. 2 TDDDG, Art. 13 Abs. 2 e/f
      (keine Pflicht, keine Art.-22-Entscheidung), DPF-Stand (EuG 03.09.2025, Rechtsmittel C-703/25 P anhängig, GitHub
      B.V.), Spenden ohne Funktionsfreischaltung, §§ 202a ff. StGB-Hinweis, neutrale Formulierung „Besitzer“. Links öffnen
      nur auf Tipp (Code geprüft). Restrisiko: keine anwaltliche Prüfung – optional, wenn die App gewerblich wird.
- [ ] DPF: Entscheidung des EuGH zu C-703/25 P beobachten; fällt das DPF, Abschnitt 2.3 auf Art. 49 Abs. 1 lit. a/b DSGVO
      bzw. GitHub B.V. (EU) umstellen. Abnahme: LEGAL.md binnen einer Woche nach dem Urteil angepasst.
- [x] abuse.ch (MalwareBazaar): Zustimmung erteilt am 2026-11-05 (Ticket ST7319719, abuse.ch-Team), mit Bedingungen –
      Wortlaut und Umsetzung in `docs/ANTIVIRUS.md` Abschnitt 6.
- [ ] **MalwareBazaar-Android-Hashes einbauen** (Bedingungen aus ANTIVIRUS.md § 6 sind Pflicht):
      1. Belkis legt das Konto auf auth.abuse.ch an und legt den Auth-Key im Vault ab (`ABUSECH_AUTH_KEY`); nie im Repo.
      2. Skript `scripts/fetch-malwarebazaar.py`: offizieller Export/API mit Auth-Key, nur Dateityp APK, je Eintrag nur
         SHA-256, Familie/Tag, Link; höchstens 1×/Woche (Zeitstempel-Sperre im Skript); Positivliste muss leer schneiden.
      3. Liste liegt im Spyhound-Repo außerhalb von `app/` (nicht in der APK → keine NonFreeAssets für F-Droid), wird nur
         auf dem Gerät über „Quellen aktualisieren“ geladen, gepinnt per Commit + SHA-256 wie die anderen Quellen.
      4. Hinweis (Wortlaut abuse.ch) in App (Info → Quellen/Lizenzen), README DE/EN und NOTICE; nicht als CC0
         bezeichnet, „subject to the abuse.ch Terms of Use, included with permission“; Fork-Bedingung im NOTICE.
      5. Löschweg: Eintrag auf Zuruf von abuse.ch per Sperrliste im Skript entfernen, nächstes Release.
      Vorbereitung (2026-11-05, ohne echten Abruf): APK-Filter mit Wochensperre, Positiv-/Sperrliste,
      TSV-Parser und deaktivierter Katalogeintrag; 44 aktive Quellen unverändert. Hinweise in NOTICE,
      App-Lizenzansicht und README DE/EN. Ablauf: `data/malwarebazaar/README.md`.
      Beleg: 46cd1b6, e800102; 8 Offline-Skripttests grün. Gezielte JVM-Tests durch schreibgeschützten Cache blockiert.
      API-Dateitypabfrage liefert höchstens 1000 aktuelle Einträge, nach Filterung ggf. weniger.
      Abnahme: Liste ≥ 1000 APK-Hashes, 0 Falschtreffer auf Pixel 11/9 + S21 Ultra, Fund einer Test-Datei offline belegt,
      Hinweis in App/README/NOTICE sichtbar, F-Droid-Pipeline weiter grün.
- [x] Ursprünglicher Prüfauftrag, abgearbeitet am 2026-11-02 (Liste aus 33939df: § 18 MStV, DPF-Status GitHub,
      Rolle als Verantwortlicher, § 25 TDDDG, Haftung/Apache-2.0, Aufsichtsbehörde-Adresse, § 202a StGB-Hinweis, Art. 13
      Pflichtangaben) – plus: externe Links (Hilfe, GitHub Sponsors) öffnen nur auf Tipp den Browser.

## Genom (was wir gelernt haben – nie löschen, nur verfeinern)
- Installationsdaten lügen: Samsung stempelt vorinstallierte Apps mit 31.12.2008, Pixel installiert bei der Einrichtung vor dem Stellen der Uhr (01.01.2020) – auch Store-Apps behalten das. Zeitangaben nie ungeprüft als „reicht bis“ zeigen, gegen ein plausibles Mindestdatum (Auslieferungs-Android) prüfen (2026-11-05, S21 Ultra).
- Fragen kostet nichts: eine höfliche, konkrete Anfrage (Zweck, Umfang, Bedingungen) brachte abuse.ch-Daten, die nach den Nutzungsbedingungen tabu waren (2026-11-05).
- Das Matrix-Skript baute die Debug-APK nicht selbst; ein Lauf hätte fast einen alten Stand getestet (2026-11-03). Jetzt baut `main` vor jedem Lauf. Ein Testbeleg gilt nur für den Stand, der wirklich installiert war.
- Bibliotheken ohne targetSdk im Manifest bringen über den Manifest-Merger alte Rechte mit (libadb → READ_PHONE_STATE, Speicher). Nach jeder neuen Abhängigkeit `aapt2 dump permissions` der Release-APK lesen.
- F-Droid-Probe vor jedem Antrag lokal: frischer Klon des Tags, leerer Gradle-Home (`-g`), ohne gradle-wrapper.jar und ohne Schlüssel bauen, dann `apksigcopier compare`. Ein warmer Cache verdeckt fehlende Einträge in verification-metadata.xml (0.3.9).
- AGP legt standardmäßig einen verschlüsselten „Dependency metadata“-Block in die Signatur; F-Droids Scanner lehnt ihn ab → `dependenciesInfo { includeInApk = false }` (0.3.10).
- uiautomator meldet Elemente unter fest stehenden Leisten mit abgeschnittenen Grenzen: eine „zu kleine“ Fläche erst prüfen, ob sie am Rand klebt. Und Knöpfe nie nur über ihren Text suchen („Öffnen“ gibt es mehrfach; einmal öffnete das die Entwickleroptionen).
- Ein Meilenstein (z. B. 1.0) beendet die Roadmap nie; er ist ein Abnahmepunkt, danach wächst sie weiter (Belkis, 2026-11-03).
- Xiaomis bigota-CDN lieferte hier 35 KB/s, derselbe Pfad im Xiaomi-Bucket `bkt-sgp-miui-ota-update-alisgp` 14 MB/s. Vor großen Downloads Tempo beider Hosts messen.
- Für Gerätetests der eingebauten Kopplung erst prüfen, welcher Weg aktiv ist: Läuft Shizuku, nimmt Spyhound Shizuku und der Kopplungsweg wird gar nicht berührt. Benachrichtigungen zählt `logcat -b events | grep notification_enqueue`.
- „Verbindung kommt nicht zustande“ erst glauben, wenn der Server-Prozess geprüft ist: Shizuku lief im Emulator die ganze Zeit, nur der Test bediente die App nicht wie ein Mensch. Testabläufe gehen den Weg des Nutzers.
- Die Liste „Was ist neu“ (arrays.xml `info_whats_new_items`) war bis 0.3.6 auf dem Stand von 0.3.0. Sie gehört zu jedem Release-Schritt, vor dem Tag; Screenshot info_top_* zeigt sie.
- `am start`/`install` holt Spyhound auch über eine gerade benutzte App nach vorne. Auf dem Pixel 11 ist das erlaubt (Belkis, 2026-11-03: „du darfst mein Handy benutzen, auch wenn ich es benutze“). Vor Bildschirmfotos trotzdem den Fokus prüfen, damit nie Privates im Bild landet.
- Bezeichnungen von Einstellungen nie aus dem Gedächtnis schreiben: aus den Ressourcen der Settings-APK des Geräts lesen (`aapt2 dump resources`, Sprache `(de)`). „WLAN-Debugging“ stand monatelang falsch in der App (2026-11-02).
- Benachrichtigungsleiste nie abfotografieren: Sie zeigt fremde Inhalte. Nur per uiautomator lesen (2026-11-02).
- Fokus-Prüfung und Aktion nie in einem Befehl verketten, wenn die Aktion selbst die App wechselt: Erst prüfen, Ergebnis lesen, dann handeln. Sonst landet ein App-Start über der App, in der Belkis gerade schreibt (2026-11-02).
- Ein einmal gekoppeltes Gerät verbindet adb per mDNS von selbst wieder. Fremde Geräte nach dem Lesen trennen und den
  Besitzer bitten, WLAN-Debugging auszuschalten und pop-os unter „Gekoppelte Geräte“ zu entfernen.
- **Vor jedem Tipp und jeder Aufnahme am Gerät `mCurrentFocus` prüfen – auch mit Freigabe.** Steht Spyhound nicht vorne:
  abbrechen. Am 2026-11-02 ging ein Tipp in eine fremde App und die Serienaufnahmen zeigten private Inhalte (sofort gelöscht).
- **Release erst committen und taggen, dann bauen.** AGP schreibt die Git-Revision in `META-INF/version-control-info.textproto`;
  0.3.4 wurde aus dem Arbeitsbaum vor dem Commit gebaut und ist darum nur inhaltlich, nicht vollständig reproduzierbar (2026-11-02).
- Hängende Agentenläufe nach jeder Runde prüfen (`pgrep -af cursor-agent`): ein Lauf hing 14 h auf einem gelöschten Worktree.
- 0.2.0 stürzte beim Start ab: R8 entfernte den von WorkManager per Reflexion genutzten Room-Konstruktor. Debug-Build,
  181 Unit-Tests und Screenshot-Tests waren grün – keiner davon läuft verkleinert. Lehre: Vor jeder Veröffentlichung
  die **Release-APK auf einem echten Gerät starten** und das Absturzprotokoll lesen (`am start` + `logcat -b crash`
  geht auch bei gesperrtem Gerät). Jede neue Bibliothek mit Reflexion braucht Keep-Regeln. (2026-11-02, behoben 0.2.1)
- JVM-Screenshot-Tests fanden in der ersten Runde drei echte Fehler, die Unit-Tests nie zeigen: Knopf bricht mitten im
  Wort um, Tablet-Spalte schneidet das Statuspanel ab, Zahlen nach Systemsprache statt App-Sprache („4,200“ auf
  Deutsch). Lehre: Jede sichtbare Änderung bekommt ein Screenshot-Bild DE/EN × Telefon/Tablet, und jemand sieht es an.
- Fremde Agenten-Arbeit nie ungelesen mergen: Ein Auftrag „Lizenzen aufklappbar“ brachte eine neue Rubrik mit selbst
  formulierten Lizenzzusammenfassungen („DNS-Listen stehen unter MIT“) – ungeprüfte Rechtsaussagen. Rechtstexte nur
  im Original oder geprüft (2026-11-03).
- Bildbelege nach Animationen erst aufnehmen, wenn sie ausgelaufen sind: Ein API-29-Bild zeigte „0 Funde“ neben einem
  Fund, weil der Zähler noch hochzählte (2026-11-03). Ein Matrix-Skript nie per `source` mit gesetztem `$0` laden –
  dann läuft sein `main` mit (ganze Matrix startete ungewollt).
  (2026-11-02)
- Nachts ist das Testgerät gesperrt, Zugangsdaten werden nie automatisch eingegeben. Lehre: Gerätetests in Arbeitszeiten
  legen, nachts auf Screenshot-Tests und Release-Build stützen; offene Gerätetests in der Roadmap vermerken. (2026-11-02)
- DNS-Wächter legte das DNS des ganzen Handys lahm (Pixel 9, 2026-11-01), vier Fehler in Folge: (1) Tunnel-Adresse =
  DNS-Adresse → Android stellt lokal zu, keine Anfrage erreicht den Tunnel; (2) `activeNetwork` ist bei laufendem VPN
  das VPN selbst → Anfragen im Kreis; (3) fehlende `ACCESS_NETWORK_STATE` → Absturz bei der ersten Anfrage; (4)
  `stopService` beendet einen VpnService mit offenem Tunnel nie. Lehre: Netzwerkfunktionen nur mit Messung auf echtem
  Gerät abnehmen (Anfrage zählt durch den Tunnel, `ping` eines frischen Namens, Ein/Aus) – Unit-Tests der Paketlogik
  allein beweisen nichts.
- Unter `sudo` zeigt `~` auf /root: Das Desktop-Toolkit verlor so Browser, Home und Indikator-Cache. Rechte-Erhöhung
  darf nie den Prüfgegenstand wechseln. (2026-11-01)
- Android-Sandbox: Ohne Shell sieht eine App keine fremden App-Daten, Logs oder Prozesse. Das muss als Lücke sichtbar
  sein, sonst wiegt „nichts gefunden“ Laien in falscher Sicherheit.
- Animierte Größe (`Modifier.size(animiert)`) misst die ganze Spalte jedes Frame neu: der Bildschirm wippt. Puls immer als
  Zeichnung (drawBehind/graphicsLayer) in fester Box. (2026-11-01, Pixel)
- `dumpsys package` nennt in `queriesPackages=` Apps, die eine App nur sehen *möchte* – das ist kein Beleg. Nur echte
  Paket-/Prozess-/Verlaufsnennungen zählen. (2026-11-01, Pixel: Microsoft Family Safety)
- Binder-Transaktionen sind auf ~1 MB begrenzt: Shell-Ausgaben (logcat, dumpsys) über Pipe streamen, nie als String.
- Dumpstate-Abschnitte enden an der nächsten `------ TITLE`-Zeile bzw. `DUMP OF SERVICE`; Zeilen mit
  „was the duration of“ sind Zeitmessung, keine Grenze. (2026-11-01)
- Fehlerbericht-ZIP nie auf die Platte entpacken: Zip-Slip und Zip-Bomben über Eintragszahl und
  unkomprimierte Gesamtgröße abwehren, je Abschnitt 32 MB. (2026-11-01)
- Lizenzen der Indikatoren: MVT MIT, ECHAP CC BY 4.0, Toolkit CC0 → mitliefern; Amnesty/Citizen Lab ohne Lizenz →
  nur auf dem Gerät laden.
