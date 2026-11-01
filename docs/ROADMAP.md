# Spyhound – Roadmap

## Kompass (aktuelle Schwerpunkte)
1. Erste lauffähige APK mit starker Optik → auf S23 / Tab S9 testen.
2. Fehlerbericht-Import (ohne Root und ohne Shizuku tiefer prüfen).
3. Unabhängiges Sicherheits-Review der Shell- und Download-Schicht.

## Stufe 1 – Fundament
- [x] Indikator-Engine: STIX 2, MISP-CSV, Domainlisten, Toolkit-TSV; Host-Suffix-Matching; Shared-Services ausgenommen.
      Beleg: 8ac9fff, CoreTest 9/9, 7.547 mitgelieferte Indikatoren.
- [x] Analysen: Apps (ID, APK-SHA-256, Zertifikat, Sideload+Rechte), Gerät (Props, Patch, Bootloader, Root, Debugging),
      SMS-Links, Shell (Prozesse, alle Pakete, logcat/dumpsys, Dateien, Hashes). Beleg: 8ac9fff.
- [x] Zugriffsstufen APP / SHELL (Shizuku) / ROOT mit Lücken-Meldung je fehlender Stufe. Beleg: 8ac9fff.
- [ ] Oberfläche (Start, Scan, Ergebnis) – eigenständige, hochwertige Optik. Abnahme: Screenshots aller Zustände, kein
      Material-Standardlook, Belkis-Abnahme.
- [ ] App-Icon + Spürhund-Maskottchen (Vektor). Abnahme: adaptive Icon, hell/dunkel.
- [ ] Indikator-Update auf dem Gerät. Abnahme: 29/29 Quellen geladen, SHA-geprüft, auf echtem Gerät.

## Stufe 2 – Tiefe ohne Root
- [ ] Fehlerbericht-Import (bugreport.zip über „Teilen“). Abnahme: echter S23-Bericht wird gelesen; Prozesse, Pakete,
      dumpsys, logcat gehen in die Shell-Analysen; Test mit Fixture.
- [ ] DNS-Wächter (lokales VpnService, kein Server): meldet Verbindungen zu Indikator-Domains live. Abnahme: Test-Domain
      löst Warnung aus, kein Datenabfluss.
- [ ] Anleitung „Shizuku ohne PC einrichten“ in der App (WLAN-Debugging, Schritt für Schritt mit Bildern).

## Stufe 3 – Qualität und Reichweite
- [ ] Englische Oberfläche (values-en).
- [ ] „Zeitreichweite“ wie im Toolkit: wie weit reichen SMS/Logs zurück, überlappt das die Kampagnenzeit?
- [ ] Signierte Release-APK + Reproduzierbarkeit + SHA256SUMS.
- [ ] Neueste Indikatoren (ungepinnt) optional, klar als „ungeprüft“ markiert.

## Genom (was wir gelernt haben – nie löschen, nur verfeinern)
- Unter `sudo` zeigt `~` auf /root: Das Desktop-Toolkit verlor so Browser, Home und Indikator-Cache. Rechte-Erhöhung
  darf nie den Prüfgegenstand wechseln. (2026-11-01)
- Android-Sandbox: Ohne Shell sieht eine App keine fremden App-Daten, Logs oder Prozesse. Das muss als Lücke sichtbar
  sein, sonst wiegt „nichts gefunden“ Laien in falscher Sicherheit.
- Binder-Transaktionen sind auf ~1 MB begrenzt: Shell-Ausgaben (logcat, dumpsys) über Pipe streamen, nie als String.
- Lizenzen der Indikatoren: MVT MIT, ECHAP CC BY 4.0, Toolkit CC0 → mitliefern; Amnesty/Citizen Lab ohne Lizenz →
  nur auf dem Gerät laden.
