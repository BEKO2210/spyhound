# Spyhound – Roadmap

## Vision
Spyhound wird **die** Sicherheits-App für Android: ehrlich, offline, für Laien verständlich, für Profis tief. Start mit
Spyware-/Stalkerware-Spuren; danach weitere Schutzfunktionen (Antivirus u. a., siehe „Stufe 4“). (Belkis, 2026-11-01)

## Kompass (aktuelle Schwerpunkte)
1. **Eingebauter Prüf-Dienst** (kein PC, keine Shizuku-App): Kopplung über WLAN-Debugging komplett in Spyhound, Code per
   Benachrichtigung eingeben. Grund: „Niemand, den ich kenne, kann das am PC“ (Belkis, 2026-11-01).
2. **UI/UX-Durchgang**: übersichtlich, animiert, selbsterklärend (DE+EN), klar sichtbar, was genutzt wird.
3. Abnahme jeder Runde auf dem Pixel 11 Pro XL (End-Testgerät).

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
- [ ] „Zeitreichweite“ wie im Toolkit: wie weit reichen SMS/Logs zurück, überlappt das die Kampagnenzeit?
- [ ] Signierte Release-APK + Reproduzierbarkeit + SHA256SUMS.
- [ ] Neueste Indikatoren (ungepinnt) optional, klar als „ungeprüft“ markiert.

## Stufe 4 – Weitere Schutzfunktionen (Ideen, jeweils mit Abnahme vor Start präzisieren)
- [ ] Antivirus offline: APKs und Downloads gegen öffentliche Malware-Hashlisten (z. B. MalwareBazaar-Exporte) und
      YARA-Regeln prüfen; Lizenz jeder Quelle prüfen. Abnahme: EICAR-ähnliche Test-APK wird erkannt, 0 Falschtreffer
      auf Pixel.
- [ ] Rechte-Prüfer: welche App darf was (Kamera, Mikro, Standort, Bedienungshilfe), mit Verlauf über Zeit.
- [ ] Netzwerk-Wächter ausbauen (DNS-Wächter → auch Tracker/Phishing-Listen, optional blockieren).
- [ ] Geräte-Härtung: Checkliste mit Direkt-Knöpfen (Bildschirmsperre, Play Protect, unbekannte Quellen, Updates).
- [ ] Zeitgesteuerte Prüfung im Hintergrund (WorkManager) mit Benachrichtigung bei neuem Fund.

- [x] Finanzierung ohne Bezahlschranke – entschieden von Belkis 2026-11-01: Spenden über GitHub Sponsors (FUNDING.yml,
      README, Info-Bildschirm). Später optional „Pro“. Ursprünglicher Vorschlag: Prüfung immer kostenlos; Spenden
      (GitHub Sponsors/Ko-fi); optional „Pro“ mit Komfortfunktionen (Hintergrundprüfung, Verlauf, PDF) per Offline-Lizenz-
      schlüssel, kein Server. Kein Bezahlen pro Scan (bricht „offline“, schreckt gefährdete Menschen ab, bei Apache-2.0
      leicht zu umgehen).

- [ ] Rechtstexte vor öffentlichem Release fachlich prüfen lassen (Liste aus 33939df: § 18 MStV, DPF-Status GitHub,
      Rolle als Verantwortlicher, § 25 TDDDG, Haftung/Apache-2.0, Aufsichtsbehörde-Adresse, § 202a StGB-Hinweis, Art. 13
      Pflichtangaben) – plus: externe Links (Hilfe, GitHub Sponsors) öffnen nur auf Tipp den Browser.

## Genom (was wir gelernt haben – nie löschen, nur verfeinern)
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
