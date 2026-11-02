# DNS-Wächter (Stufe 2)

Lokales `VpnService` ohne Remote-Server: Das System sendet DNS nur an virtuelle Adressen im Tunnel;
der Dienst leitet Anfragen an die normalen Resolver des Geräts weiter und vergleicht QNAMEs mit `IocSet.lookupHost`.
Bei Treffer: Benachrichtigung (Kanal **Warnungen**) und Eintrag in `filesDir/dns_guard_hits.json` (max. 500).
Harmlose Domains werden nicht geloggt. Spyware-Treffer warnen **nur** (nie blockieren); Tracker/Phishing siehe „Stufe 4“ unten.

## API

```kotlin
DnsGuard.start(activity)          // ggf. VPN-Freigabe-Dialog
DnsGuard.stop(context)
DnsGuard.isRunning
DnsGuard.recentHits(context)      // Liste der letzten Treffer
```

Nach VPN-Freigabe `start()` erneut aufrufen (z. B. im Activity-Result).

## Manifest (anderer Branch — hier nur Snippet)

In `AndroidManifest.xml` unter `<application>`:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

<service
    android:name="de.belkis.spyhound.dns.DnsGuardService"
    android:permission="android.permission.BIND_VPN_SERVICE"
    android:exported="false"
    android:foregroundServiceType="specialUse">
    <intent-filter>
        <action android:name="android.net.VpnService" />
    </intent-filter>
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="dns_monitoring" />
</service>
```

**Erklärung:** `BIND_VPN_SERVICE` schützt den Dienst vor fremden App-Starts; `VpnService`-Intent-Filter ist für die System-VPN-Freigabe nötig. Foreground mit `specialUse` und der Property beschreibt den Zweck (DNS-Beobachtung ohne allgemeinen Datentunnel) für Android 14+.

## Paket

| Datei | Rolle |
|-------|--------|
| `dns/DnsPacket.kt` | IPv4/IPv6 + UDP + DNS-Namen (JVM-testbar) |
| `dns/DnsGuardService.kt` | TUN, Weiterleitung, Treffer |
| `dns/DnsGuard.kt` | Öffentliche Steuerung |
| `dns/DnsHitStore.kt` | Lokale Trefferliste |

Virtuelle DNS: `10.111.222.1/32`, `fd00:5ec::1/128` — Routen nur dorthin, übriger Verkehr unverändert.

# Stufe 4: Tracker- und Phishing-Listen, Blockieren

Stand der Recherche: **2026-11-02**. Lizenzdateien wurden am gepinnten Commit des Quell-Repos geöffnet (raw.githubusercontent.com).
Regel: nur nehmen, was **mitgeliefert** werden darf oder von **festem Commit mit SHA-256** geladen wird und zu Apache-2.0 passt. Im Zweifel weglassen.

## 1. Quellen-Recherche

| Quelle | Lizenz (Beleg, 2026-11-02) | Entscheidung |
|---|---|---|
| **Phishing.Database** (`Phishing-Database/Phishing.Database`, früher mitchellkrogza), Commit `12a20bf`, 2026-10-02 | `LICENSE`: „MIT License … to use, copy, modify, merge, publish, distribute, sublicense, and/or sell“ | **Ja, nur Download** (gepinnt). `phishing-domains-ACTIVE.txt`: 11 023 128 Byte, 392 178 Zeilen. Nicht mitgeliefert: zu groß für das APK und veraltet in Tagen. Ohne Update-Lauf ist die Liste leer |
| **Perflyst/PiHoleBlocklist** `android-tracking.txt`, Commit `ad5020b` | `LICENSE`: „MIT License, Copyright (c) 2018 Perflyst“ | **Ja, mitgeliefert**: 2 153 Byte, 80 Domains |
| **StevenBlack/hosts**, Commit `6d56ab4` | `license.txt`: „The MIT License (MIT)“. Aber die vereinigte `hosts`-Datei bündelt laut README-Tabelle u. a. MVPS (CC BY-NC-SA 4.0), Dan Pollock (non-commercial), KADhosts (CC BY-SA), yoyo.org (ohne Lizenz) | **Nur** die eigene MIT-Teilliste `data/StevenBlack/hosts`, mitgeliefert: 98 038 Byte, 2 894 Domains. Vereinigte Datei und Alternates **nicht** |
| **Hagezi dns-blocklists** | GPL-3.0; README: Listen nur unter den Bedingungen dieser Lizenz weitergeben | **Weglassen.** Ob Daten-Listen unter GPL-Copyleft fallen und was das für ein Apache-2.0-Paket heißt, ist ungeklärt → im Zweifel nein |
| **Block List Project** (`blocklistproject/Lists`) | Unlicense; Repo archiviert; README: gleicht mit Upstream-Listen ab, u. a. Hagezi und ShadowWhisperer | **Weglassen.** Die Unlicense deckt fremde Anteile nicht; Herkunft mit GPL-Anteilen unsauber |
| **URLhaus** (abuse.ch) | Nutzungsbedingungen: Exporte für nicht-kommerzielle Zwecke; CC0-Ausnahme nur für Feodo Tracker, SSL Blacklist, YARAify; API verlangt Auth-Key | **Weglassen.** Keine Weitergaberechte, Key pro Nutzer, kein Commit zum Pinnen. Siehe `docs/ANTIVIRUS.md` |
| **AssoEchap/stalkerware-indicators** | README, „License“: „The content of this repository is licensed under CC-BY“ (CC BY 4.0) | **Bereits eingebunden** (Stalkerware-C2, Namensnennung in `NOTICE`). Keine weitere klar lizenzierte Stalkerware-C2-Liste gefunden |

Ehrliche Grenze: Die Tracker-Listen sind klein (rund 3 000 Domains); große Blocklisten sind lizenzrechtlich schwierig. Die Phishing-Liste ist ein Schnappschuss am Pin. Vor jedem Pin-Update Lizenzen erneut prüfen.

## 2. Umsetzung

- Eigene Kategorien `TRACKER` und `PHISHING` (`ioc/Category`), getrennt von den Spyware-Kategorien; `Category.isEvidence` trennt im Code.
- Eigener Katalog `assets/iocs/domainlists.json` (Repo, Commit, Pfad, SHA-256). Die Listen landen **nie** in einem `IocSet` und nicht in `sources.json`: ein Scan darf keine Werbe-Domain als Spyware melden. `IocStore.loadDomainLists()` prüft den SHA-256; fehlende oder veränderte Dateien werden übersprungen. `IocStore.update()` lädt die Phishing-Liste (Allow-List, fester Commit, SHA-256).

| Kategorie | Warnen (Standard) | Blockieren (Standard) |
|---|---|---|
| Phishing | **an** | aus |
| Tracker/Werbung | **aus** | aus |
| Spyware, Stalkerware, Malware | immer | **nie** |

- **Blockieren** = lokale **NXDOMAIN**-Antwort (`DnsPacket.buildNxdomain`: gleiche ID und Frage, QR=1, RA=1, RCODE=3, keine Records). Wirkt sofort ohne Neustart.
- **Spyware bleibt nur Warnung.** Ein blockierter Abruf zerstört die Spur, die als Beweis gebraucht wird, und ein Täter könnte den Eingriff bemerken. Steht eine Domain zugleich auf einer Tracker-/Phishing-Liste, wird die Anfrage normal beantwortet.
- Tracker-Treffer: nur Listeneintrag, keine Benachrichtigung. Gleiche Tracker-/Phishing-Namen werden höchstens alle 10 Minuten neu protokolliert.
- Trefferliste (max. 500): bei Überlauf fallen zuerst Tracker-/Phishing-Einträge, nie Spyware (`DnsHitStore.trim`).
- Einstellungen: `dns/DnsFilterPrefs`; UI: Karte „DNS-Wächter“ auf dem Startbildschirm.

## 3. Speicher und Leistung

`ioc/DomainList`: Labels umgekehrt („www.example.com“ → „com.example.www“), alle Einträge in **einem** sortierten `ByteArray` mit `IntArray`-Offsets. Host und Eltern-Domains werden per Binärsuche ohne Allokation geprüft. Reine TLDs, IP-Zeilen, ungültige Namen und `SharedServices` werden verworfen. Speicher ≈ Namensbytes + 4 Byte je Eintrag (kein HashMap-Overhead). Test: 100 000 Einträge, 20 000 Anfragen, Mittel **< 1 ms** pro Anfrage.

## 4. Tests

`DnsFilterTest` (18 Tests): Parser, Suffix-Treffer ohne Lookalikes, Leistung, Kategorie-Trennung, Standardwerte, Warn-/Blockier-Logik, Spyware nie blockiert, NXDOMAIN-Paket, Kürzung der Trefferliste.
