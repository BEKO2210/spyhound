# Spyhound – Roadmap

## Vision
Spyhound wird **die** Sicherheits-App für Android: ehrlich, offline, für Laien verständlich, für Profis tief. Start mit
Spyware-/Stalkerware-Spuren; danach weitere Schutzfunktionen (Antivirus u. a., siehe „Stufe 4“). (Belkis, 2026-11-01)

## Kompass (aktuelle Schwerpunkte, neu geordnet 2026-11-02 01:20)
1. ~~**Einrichtung ohne PC als geführter Assistent**~~ – gebaut (1186e7d), Gerätetest offen. Größter Hebel für Laien. Abnahme: 3 Schritte, die sich selbst
   abhaken (WLAN-Debugging an → Kopplungsdialog offen → Code eingegeben), Knopf öffnet direkt die richtige Einstellung,
   Hinweis/Knopf für Splitscreen; Pixel 9: Kopplung ohne Rückfrage durchführbar.
2. **Kleine Funde vom Pixel (Politur)** – siehe „Offen aus Gerätetests“. Abnahme je Punkt dort.
3. ~~**Tablet/Querformat überall zwei Spalten**~~ – gebaut (1186e7d), Prüfung im Querformat auf dem Pixel 9 offen. Abnahme: Breite ≥ 600 dp zwei Spalten,
   keine leere Hälfte; Telefon unverändert (Pixelvergleich).
4. ~~**Übergänge Start → Scan → Ergebnis**~~ – gebaut (a8bcc48), Gerätetest offen. (Radar wandert, gemeinsame Elemente). Abnahme: kein Sprung, reduzierte
   Bewegung respektiert, 60 fps auf Pixel 9.

## Release 0.3.5 (2026-11-02) – erstes vollständig reproduzierbares Release (verify-reproducible: 464 Dateien gleich), Positivliste Pixel + Samsung (10830), ruhigere Scan-Zeile. Startprobe Release-APK Pixel 9 ohne Absturz; Pixel 11 (Positivliste) vorher abgenommen. Abnahme durch Belkis offen.

## Release 0.3.4 (2026-11-02) – abgenommen von Belkis auf dem Pixel 11 Pro XL („funktioniert“): einheitliche Quellenzahl in der Aktualisierungsmeldung, DNS-Schalter in einfacher Sprache.

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
- [ ] Positivliste: weitere Hersteller (Xiaomi, OnePlus, Motorola) und je Gerät neue Builds nach Updates. Abnahme wie oben.

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
- [ ] Barrierefreiheit-Durchgang (DESIGN §1.6): TalkBack-Labels aller Knöpfe/Chips/Karten, Tippflächen ≥ 48 dp,
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
- [ ] Elfenbein vereinheitlichen: die Oberfläche nutzt `#F2EBE0`, die Marke `#F3EEE3`.
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
- [ ] Spot-Illustrationen (illu_*.xml) im Stil von Logo v2 neu zeichnen – Einführung zeigt noch den kantigen v1-Hund.
- [x] Englische Oberfläche + Sprachwahl. Beleg: Cherry-Pick der i18n-Commits, 58 Tests, docs/shots/en-*.

- [ ] UI/UX-Durchgang (Cloud-Sitzung): übersichtlicher, animierter, selbsterklärend in DE+EN; u. a. weiche Ausblendung
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
- [ ] Shell-Probe schlägt fehl → einmal automatisch trennen und neu verbinden (Shizuku-Dienst), erst dann Lücke melden.
      Fund: Pixel 2026-11-01, veralteter Dienst nach App-Update; App-Neustart behob es. Abnahme: Update der App bei
      laufendem Shizuku → Tiefenprüfung läuft ohne Neustart.

## Stufe 2 – Tiefe ohne Root
- [x] Fehlerbericht-Import, Analyse-Schicht: `bugreport/` liest bugreport-*.zip (Prozesse, Pakete inkl.
      Hidden/deinstalliert, dumpsys, logcat, getprop) und füttert die Shell-Analysen.
      Beleg: BugreportTest 7/7, `:app:testDebugUnitTest` 16/16.
- [x] Fehlerbericht in der Oberfläche (Teilen/Auswählen). Beleg: 69ca581. Offen: Abnahme mit echtem Bericht (Pixel).
- [x] DNS-Wächter gebaut (e91f5d7, 6 Tests). Offen: Abnahme auf Gerät mit Test-Domain.
      löst Warnung aus, kein Datenabfluss.
- [x] Geführter Assistent „Tiefere Prüfung einrichten“ (Checkliste hakt sich selbst ab, Direkt-Knöpfe). Beleg: 6764331, e62c904.
- [ ] Assistent mit echtem Laien testen (ohne Hilfe bis „bereit“). Abnahme: eine Person ohne Technikwissen schafft es allein.

## Upstream (Zero Breach Toolkit)
- [ ] PR #31 „Android: package names in <queries> declarations are not install traces“ – offen seit 2026-11-01.
      Abnahme: gemergt. Beleg lokal: 1959 Tests grün, build --check grün.
      Blocker: Upstream verlangt signierte Commits. Belkis (am PC): `gh auth refresh -s admin:ssh_signing_key`,
      `gh ssh-key add ~/.ssh/id_ed25519.pub --type signing`, dann Commit mit -S neu signieren + force-with-lease.
- [ ] Issue: Spyhound als Begleit-App vorschlagen – erst nach öffentlichem, signiertem Release.

## Testmatrix (Emulatoren, strikt nacheinander – RAM 15 GB)
- [x] Pixel 11 Pro XL, Android 17, echt: Basis + Shizuku. Beleg: siehe Stufe 1.
- [ ] API 29 (minSdk): Start, Scan Basis, Bericht. Abnahme: keine Abstürze, Screenshots.
- [ ] API 31, API 34, API 37: dito + Shizuku (APK per adb, Start per libshizuku.so).
- [ ] Tablet-Profil (Pixel Tablet, ~Tab-S9-Format): Layout zentriert ≤ 640 dp.
- [ ] Root: Emulator mit Magisk (rootAVD), Stufe ROOT inkl. /data/data-, Tombstone-, packages.xml-Prüfung.
- [ ] Testfund: Emulator mit installierter harmloser Test-APK, deren Paketname/Zertifikat als Test-Indikator geladen
      ist → Fund erscheint rot (Ende-zu-Ende-Beweis, nur Debug-Build).
- Lücke: Samsung One UI (Auto Blocker, eigene Menüs) lässt sich im Emulator nicht nachstellen – Anleitungstexte bleiben
  ungetestet, bis ein Samsung-Gerät verfügbar ist.

- [ ] Eingebauter Prüf-Dienst statt separater Shizuku-App: Kopplung per WLAN-Debugging direkt in Spyhound (libadb-android,
      Apache-2.0/GPL-3.0 dual; Shizuku Apache-2.0, API MIT → NOTICE), eigener Dienst per app_process mit ADB-Rechten.
      Abnahme: Pixel ohne installierte Shizuku-App erreicht Stufe „Tief“; nach Neustart ein Tipp zum Neustarten.

## Stufe 3 – Qualität und Reichweite
- [x] „Zeitreichweite“ wie im Toolkit (491530d): wie weit reichen SMS/Logs zurück, überlappt das die Kampagnenzeit?
- [x] Signierte Release-APK + SHA256SUMS (v0.1.0, 2026-11-02).
- [x] Reproduzierbarkeit (2026-11-02): v0.3.4 aus frischem Klon (anderer Pfad) neu gebaut → 463 Dateien byte-gleich, auch
      unsigniert (ohne Schlüssel). Einzige Abweichung: eingebettete Git-Revision, weil die veröffentlichte APK vor dem
      Commit gebaut wurde. Prüfskript `scripts/verify-reproducible.sh`, README DE/EN. Abnahme ab 0.3.5: Skript meldet
      REPRODUCIBLE.
- [ ] Neueste Indikatoren (ungepinnt) optional, klar als „ungeprüft“ markiert.

## Stufe 4 – Weitere Schutzfunktionen (Ideen, jeweils mit Abnahme vor Start präzisieren)
- [~] Antivirus offline – Schritt 1 erledigt (ESET-Hashlisten, 79 Hashes, 0 Falschtreffer auf 703 APKs Pixel 9). Offen: Gerätetest A3/A5/A6, Positivliste, weitere lizenzklare Quellen (abuse.ch anfragen, YARAify prüfen). Ursprünglich: APKs und Downloads gegen öffentliche Malware-Hashlisten (z. B. MalwareBazaar-Exporte) und
      YARA-Regeln prüfen; Lizenz jeder Quelle prüfen. Abnahme: EICAR-ähnliche Test-APK wird erkannt, 0 Falschtreffer
      auf Pixel.
- [x] Rechte-Prüfer „Wer darf was“: 11 Rechte-Gruppen, Systemapps ausblendbar, Verlauf lokal, „neu seit letzter Prüfung“.
      Beleg: PermissionAuditTest, Screenshots audit_*. Offen: Abnahme auf dem Pixel.
- [~] Netzwerk-Wächter ausbauen – gebaut (Tracker/Phishing, Warnen/Blockieren je Kategorie, Lizenzen geprüft). **Gerätetest offen**: DNS an, Phishing-Testdomain → Warnung; Blockieren → NXDOMAIN; Aus → normales DNS (Lehre vom 2026-11-01!). Erst danach Release.
- [x] Geräte-Härtung: Checkliste mit Direkt-Knöpfen (491530d) (Bildschirmsperre, Play Protect, unbekannte Quellen, Updates).
- [x] Zeitgesteuerte Prüfung im Hintergrund (WorkManager) mit Benachrichtigung bei neuem Fund (295ff6d).

- [x] Finanzierung ohne Bezahlschranke – entschieden von Belkis 2026-11-01: Spenden über GitHub Sponsors (FUNDING.yml,
      README, Info-Bildschirm). Später optional „Pro“. Ursprünglicher Vorschlag: Prüfung immer kostenlos; Spenden
      (GitHub Sponsors/Ko-fi); optional „Pro“ mit Komfortfunktionen (Hintergrundprüfung, Verlauf, PDF) per Offline-Lizenz-
      schlüssel, kein Server. Kein Bezahlen pro Scan (bricht „offline“, schreckt gefährdete Menschen ab, bei Apache-2.0
      leicht zu umgehen).

- [ ] Rechtstexte vor öffentlichem Release fachlich prüfen lassen (Liste aus 33939df: § 18 MStV, DPF-Status GitHub,
      Rolle als Verantwortlicher, § 25 TDDDG, Haftung/Apache-2.0, Aufsichtsbehörde-Adresse, § 202a StGB-Hinweis, Art. 13
      Pflichtangaben) – plus: externe Links (Hilfe, GitHub Sponsors) öffnen nur auf Tipp den Browser.

## Genom (was wir gelernt haben – nie löschen, nur verfeinern)
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
