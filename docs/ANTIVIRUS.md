# Antivirus offline – Konzept (Roadmap Stufe 4)

Status: **Schritt 1 (ESET, 5 Ordner) und Schritt 2 (alle ESET-Android-Ordner, Meta, Positivliste) umgesetzt; Rest Konzept.** Stand der Recherche: **2026-11-02** (alle Quellen an diesem Tag selbst geöffnet).
Grundlage: `docs/SPEC.md` (Grundsätze 1, 2, 4, 5), `app/src/main/assets/iocs/sources.json` (29 gepinnte Quellen, Repos:
`mvt-project/mvt-indicators`, `AmnestyTech/investigations`, `citizenlab/malware-indicators`, `AssoEchap/stalkerware-indicators`),
`ioc/FeedCatalog` (`ALLOWED_REPOS`), `IocStore.update()` (Pin + SHA-256, keine Redirects).

Ausgangslage: Der Abgleich „SHA-256 einer installierten APK gegen `IocType.SHA256`“ existiert bereits
(`AppAnalyzer`, `ApkFile.sha256`). Stufe 4 heißt also vor allem: **mehr Hashes aus rechtlich sauberen, gepinnten Quellen** –
nicht neue Scan-Technik.

## 1. Quellen-Recherche

Bewertung: **mitliefern** (Datei im APK-Asset), **nur Download** (Gerät lädt gepinnten Commit, wie bei Amnesty/Citizen Lab),
**gar nicht**. Pflicht für jede Quelle: Git-Repo mit festem Commit (sonst kein Pin möglich) und SHA-256 der Datei.

| Quelle | Format / Größe / Aktualität | Lizenz (Beleg) | Entscheidung |
|---|---|---|---|
| **MalwareBazaar** (abuse.ch) | CSV/TXT-Hashlisten-Export, API; > 1,16 Mio. Samples insgesamt, Android-Anteil nicht gemessen; laufend aktualisiert | Zugang nur mit persönlichem `Auth-Key` („you need to obtain an `Auth-Key` first“, bazaar.abuse.ch/export). Nutzungsbedingungen (abuse.ch/terms-of-use, Stand 21.10.2026): kostenlos nur „for not-for-profit purposes“; verboten „copy, adapt, alter … or make derivative works based on the Platforms“ und „high volume automatic … process to access … or harvest information“; Nutzung für KI-Training untersagt. CC0 gilt laut §3.4 nur für Feodo Tracker, SSL Blacklist, YARAify – **nicht** für MalwareBazaar | **Gar nicht** (weder mitliefern noch App-seitig laden). Kein Git-Commit, also kein Pin; Weitergabe nicht erlaubt; Auth-Key pro Nutzer widerspricht „kein Konto“. Ausnahme nur mit schriftlicher Zustimmung von abuse.ch/Spamhaus |
| **YARAify** (abuse.ch) | YARA-Treffer-Daten | „free to use without limitations according to Creative Commons principles (CC0) for both commercial and non-commercial purposes“ (Terms §3.4) | Lizenz ok, aber live-Dienst ohne Commit/Pin, keine stabile Android-Hashliste gefunden → **offen**, siehe Abschnitt 5 |
| **ESET `malware-ioc`** (GitHub) | je Kampagne `samples.sha256` (Zeilen-Hex), `.yar`, `.rules`; Android-Kampagnen u. a. `aridspy`, `vajraspy`, `spyloan`, `ngate`, `promptspy`; je Datei < 1 KB, Repo ~2 MB; letzter Commit `5974550` vom 2026-10-21 | BSD-2-Clause (LICENSE: „Redistributions of source code must retain the above copyright notice“). README: „licensed under the permissive BSD two-clause license“ | **Mitliefern erlaubt** (Copyright-Hinweis in `NOTICE`). Auswahl der Android-Ordner von Hand; Repo mischt Plattformen, `samples.sha256` nennt keine → Plattform je Ordner in `sources.json` pflegen |
| **Neo23x0/signature-base** | `iocs/*.csv`, `yara/*.yar`; überwiegend Windows/Linux | Detection Rule License 1.1: Weitergabe erlaubt, wenn Autor („author“), Link zum Regelwerk und Lizenztext erhalten bleiben; Treffermeldungen müssen den Autor nennen. Einzelregeln können abweichende `license`-Meta tragen | Lizenz mit Apache-2.0 vereinbar (Attribution). **Nur Download** (gepinnt) und nur, falls Android-relevante Regeln gefunden werden; derzeit kein Android-Schwerpunkt belegt → zurückgestellt |
| **Yara-Rules/rules** | `.yar`, Kategorie `mobile_malware`; letzter Commit 2022-04-12 | GNU GPLv2 („Our Yara ruleset is under the GNU-GPLv2 license“). README: Android-Regeln brauchen das Androguard-Modul, „deprecated … because it seems an abandoned project“ | **Nicht mitliefern** (Copyleft passt nicht in ein Apache-2.0-Paket). Veraltet, Modul nicht nutzbar → **gar nicht** |
| **Koodous** | Plattform, API mit Schlüssel, öffentliche/private YARA-Rulesets | Nutzungsbedingungen nicht lesbar: koodous.com/terms liefert nur eine JavaScript-Hülle. Regeln gehören ihren Autoren, kein einheitliches Lizenzmodell belegt | **Gar nicht**, bis Bedingungen schriftlich geklärt sind (Konto/Schlüssel widerspricht ohnehin Offline-Grundsatz) |
| **VirusShare** | Hash-/Sample-Archiv (aktuell 120,4 Mio. Samples, Stand 2026-11-02 am Seitenkopf) | „access … is granted by invitation only“; „The use of bots, scripts, or other methods to scrape data … will not be tolerated.“ Keine Weitergaberechte genannt | **Gar nicht** |
| **ashishb/android-malware** (GitHub) | Ordner je Familie mit echten Samples; Repo ~600 MB, letzter Push 2025-12-31 | Keine Lizenz (GitHub-API: `license: null`), enthält lebende Schadsoftware | **Gar nicht** (ohne Lizenz keine Weitergabe; Samples gehören nicht in die Toolchain) |
| Bereits eingebunden: MVT (MIT), ECHAP (CC BY 4.0), Toolkit-Kurationen (CC0); Amnesty (CC BY laut README) und Citizen Lab (CC BY-NC-SA) → nur Download, siehe 7.2 | STIX 2 inkl. `file:hashes.sha-256` | siehe `docs/SPEC.md` | unverändert; deckt Spyware/Stalkerware, **nicht** Banking-Trojaner/Adware/Massenmalware |

**Ehrliches Fazit:** Die großen Massen-Hashlisten (MalwareBazaar, VirusShare, Koodous) sind für eine quelloffene, kontolose,
gepinnte App **nicht nutzbar**. Rechtlich sauber und pinnbar sind kleine, kuratierte Forscher-Repos (ESET). Damit prüft
„Antivirus offline“ realistisch **einige Hundert bis wenige Tausend** bekannte Android-Hashes (gezielte Kampagnen), keinen
Massenmarkt. Das muss in der UI und in der Werbung so stehen („kein Ersatz für Play Protect“, SPEC-Grundsatz 2).
Größen/Anzahlen der Android-Hashes je ESET-Ordner sind noch zu zählen (Abnahme A1).

## 2. Design

### 2.1 Datenfluss (keine neuen Netzzugriffe)
1. Neue Quellen gehen in `sources.json` (repo, commit, path, `format: "hashlist"`, `family`, `category: "malware"`, sha256)
   und `ALLOWED_REPOS` (z. B. `eset/malware-ioc`). Update läuft ausschließlich über `IocStore.update()`:
   Allow-List, fester Commit, SHA-256-Prüfung, keine Redirects. Mitgelieferte Quellen liegen als Asset.
2. Neuer Parser `hashlist` (eine Zeile = ein Hex-SHA-256, `#`-Kommentare erlaubt; Länge 64, `[0-9a-f]`, sonst Zeile verwerfen).
   Die Familie kommt aus `sources.json` (`family`), nicht aus der Datei.
3. Der Scan selbst bleibt netzwerkfrei.

### 2.2 Was geprüft wird
- **Installierte APKs:** vorhandenes `ApkFile.sha256` (Base + Splits) gegen den Hash-Satz.
- **Downloads/Dateien:** nur Dateien mit Endung `.apk`, `.apks`, `.xapk`, `.apkm`, Größe ≤ Limit
  (vorhandenes `StorageCollector`/Shell-`find`-Limit 120 MB). Streaming-SHA-256 (8-KB-Puffer), Fortschritt und Abbruch wie in
  `ScanEngine`. Ohne Speicherzugriff → `Gap` mit `GapFix` (wie bisher). Kein Inhalt verlässt das Gerät.
- **YARA:** nicht in Schritt 1 (keine Quelle mit tragfähiger Lizenz, native Bibliothek, mehr Falschtreffer). Zurückgestellt.

### 2.3 Speicher
Rechnung für N Hashes bei 32 Byte: 10 000 → 320 KB, 100 000 → 3,2 MB. Bei der realistischen Größe (Tausende) genügt ein
**sortiertes `ByteArray` (32 B je Eintrag), Binärsuche** – exakt, ohne Falschtreffer, einfacher als ein Bloom-Filter.
Falls später > ~500 000 Hashes: zweistufig – Bloom-Filter (~10 Bit/Eintrag, 1 % Fehlrate ⇒ 600 KB bei 500 k) nur als
Vorfilter, **jeder** Bloom-Treffer wird gegen die exakte Liste bestätigt; Bloom allein erzeugt nie einen Fund.
Der Sortier-Index wird beim Update einmal gebaut und app-privat abgelegt (Prüfsumme der Quellen als Cache-Schlüssel).

### 2.4 Falschtreffer-Strategie
- **Fund (`Finding`) nur bei exaktem 256-Bit-Treffer.** Keine Präfix-, Fuzzy-, ssdeep-, Namens- oder Paketnamen-Treffer als Fund.
- Familienname nur aus der kuratierten Quelle (`family`); fehlt er, steht „Bekannte Schadsoftware (Quelle: ESET)“, nichts geraten.
- Quellen-Zitat und Commit-Kurzhash im Fund (Nachvollziehbarkeit, Lizenz-Attribution).
- Eigene Positivliste **gegen** die Hashlisten: SHA-256 bekannter Systemapps/der eigenen App wird nie als Fund gemeldet
  (Sicherheitsnetz bei vergifteten Quellen); Quelle mit Treffer auf Positivliste wird im Update verworfen und gemeldet.
- Reine Sicherheit gegen Manipulation: Pin + SHA-256 je Quelle schon vorhanden; Änderungen an Quellen nur per Review (PR).

### 2.5 UI-Einordnung (SPEC-Grundsatz 4)
- **Fund (rot):** exakter Hash-Treffer. Text: „Diese App ist als Schadsoftware **<Familie>** bekannt (Quelle …).“ Empfehlung:
  deinstallieren, Konten-Passwörter ändern. Stalkerware weiter mit eigenem Ton.
- **Hinweis (`Lead`, gelb/neutral):** unbekannte Datei, kein Treffer, aber Auffälligkeit (bereits bestehende Leads:
  Sideload + Rechte). Nie „Virus“.
- **Kein Treffer:** „Keine bekannten Spuren im geprüften Bereich“ + Zahl geprüfter Dateien + Stand der Listen („Hashlisten vom …,
  N Einträge“). Nie „sauber“, nie „geschützt“.
- Prüfstatus-Zeile „Antivirus: Listen aus 3 Quellen, Stand …“ in den Quellen-Details.

## 3. Abnahmekriterien für Schritt 1

Schritt 1 = Hashlisten-Parser + ESET-Android-Quellen + Treffer-Anzeige für installierte APKs und Dateien in Downloads.
- **A1** Jede neue Quelle: Lizenzbeleg (Link, Zitat, Abrufdatum) in diesem Dokument, Eintrag in `NOTICE`, `sources.json`-Pin mit
  SHA-256, Anzahl der Hashes dokumentiert. Test: `IocStore` lehnt Quelle außerhalb der Allow-List und falsche SHA-256 ab.
- **A2** Unit-Test: Hashlisten-Parser (gültig, kurz, Großbuchstaben, Kommentare, CRLF, leere Datei).
- **A3** Bekannter Test: eigenes harmloses Test-APK (mit EICAR-Zeichenkette, nur zu Testzwecken) wird **per Test-Fixture-Feed**
  erkannt, wenn dessen SHA-256 in einer Testliste steht (Instrumentierungstest auf dem Pixel, Download-Ordner). Kein echtes
  Schadsamples auf dem Gerät. Zusätzlich: ein Hash aus der echten ESET-Liste wird in der Hash-Tabelle gefunden (JVM-Test).
- **A4** **0 Falschtreffer auf Pixel 9/11** auf einem normalen Gerät (Systemapps + Play-Apps + Downloads): Scan meldet 0 Funde.
  Zusätzlich JVM-Test: SHA-256 der Systemapps-Referenzliste/Positivliste kollidiert mit keiner Quelle.
- **A5** Offline-Beleg: Scan mit Flugmodus, kein Socket außer Loopback (bestehender Test bleibt grün).
- **A6** Leistung: ≤ 60 s für 100 Apps + 200 Downloads auf Pixel 9; Speicher der Liste ≤ 5 MB; Abbruch jederzeit.
- **A7** Texte: kein Wort „sauber“/„sicher“ im Ergebnis ohne Fund (vorhandener Textcheck); Fund/Hinweis getrennt sichtbar.
- **A8** `./gradlew :app:testDebugUnitTest :app:assembleDebug` grün.

## 4. Risiken
- **Geringe Abdeckung** (nur kuratierte Kampagnen) → Erwartungsmanagement; keine Konkurrenz zu Play Protect behaupten.
- **Hash-Busting:** Banking-Trojaner ändern Hashes laufend; exakter Hash erkennt nur bekannte Builds.
- **Lizenzänderungen** (abuse.ch änderte Bedingungen zuletzt am 21.10.2026): Quellen vor jedem Pin-Update erneut prüfen.
- **Vergiftete/fehlerhafte Quelle** könnte legitime Apps „fundieren“ → Positivliste, Pin, Review.
- **Mischrepos (ESET):** Hashes anderer Plattformen sind harmlos, aber unnötig groß; Ordner gezielt wählen.
- **Dateizugriff:** Scoped Storage – ohne `MANAGE_EXTERNAL_STORAGE` oder Shell sind Downloads nicht lesbar; bestehende Gap-Logik nutzen.
- **Haftung/Image:** Falsch-Alarm bei Stalkerware-Opfern kann gefährlich sein (Täter bemerkt Entfernung) → Hinweise zur sicheren Reihenfolge.

## 5. Offene Fragen
1. ~~Anfrage an abuse.ch/Spamhaus~~ – beantwortet: Zustimmung mit Bedingungen, siehe Abschnitt 6.
2. Gibt es von YARAify (CC0) stabile, versionierte Android-Hashlisten, die sich in einem eigenen Repo spiegeln und pinnen lassen?
3. Koodous: Bedingungen im Browser mit eingeloggtem Konto lesen (hier nicht möglich); Weitergabe der Hashes?
4. Weitere Forscher-Repos mit klarer Lizenz (Kandidaten sichten: Lookout, Zimperium, Kaspersky-IOC-Repos, Google TAG) – je Repo Lizenzdatei öffnen.
5. Wer pflegt `sources.json` und das Quellen-Review (Takt, Verantwortung)?
6. Soll `hashlist` auch Dateien außerhalb APK (z. B. DEX/ELF in `/data/local/tmp`) abdecken? (nur mit SHELL/ROOT)
7. Familien-Namen: Alias-Zuordnung (VajraSpy ↔ Hamas-Kampagne etc.) – nur Quellenname, keine eigene Einordnung?

## 6. Umsetzungsstand Schritt 1 (2026-11-02)

Quelle: `eset/malware-ioc`, Commit `59745502656e813f9f79fc8005243edbc04f9397` (2026-10-21), BSD-2-Clause, mitgeliefert
(Lizenztext in `NOTICE`). Je Ordner nur `samples.sha256` (APK-SHA-256; SHA-1/MD5 werden nicht verglichen).

| Ordner | Familie | SHA-256 |
|---|---|---|
| aridspy | AridSpy | 15 |
| vajraspy | VajraSpy | 13 |
| spyloan | SpyLoan | 17 |
| ngate | NGate | 25 |
| promptspy | PromptSpy | 9 |

Summe 79 eindeutige Hashes (A1). Parser `hashlist`, `Category.MALWARE`, `Verdict.MALWARE`, Texte/Guidance DE+EN, Lücke
`MALWARE_COVERAGE` („nur ausgewählte Kampagnen, kein Ersatz für Play Protect“) und Hinweis in den Quelldetails der Info-Seite.
Installierte APKs (`AppAnalyzer`) und Dateien (`ShellAnalyzer.hashes`, inkl. Downloads mit Dateizugriff) werden gegen die Hashes
geprüft; nur exakte Treffer. Die Positivliste aus 2.4 ist seit Schritt 2 gebaut (Abschnitt 7).

Abnahme: A1, A2, A3 (JVM-Teil), A7 (Textcheck bleibt grün), A8 per `AntivirusTest`/`testDebugUnitTest`. Gerätetest offen: A3
(EICAR-Test-APK im Download-Ordner), A4 (0 Falschtreffer auf echtem Gerät), A5 (Flugmodus-Lauf), A6 (Leistung Pixel 9).

## 7. Schritt 2: weitere lizenzklare Quellen (2026-11-02)

Alle Lizenzdateien/READMEs an der Quelle selbst geöffnet (GitHub-API und `raw.githubusercontent.com`, Abruf 2026-11-02).

### 7.1 ESET `malware-ioc`, alle Ordner durchgegangen
Repo-Stand `5974550` (2026-10-21), BSD-2-Clause. 304 `README.adoc`/`samples.sha256` (alle ~190 Kampagnenordner) geladen und
auf „Android“, „apk“, „Google Play“, „mobile“ geprüft. Ergebnis:

| Ordner | Familie | SHA-256 | Entscheidung |
|---|---|---|---|
| aridspy, vajraspy, spyloan, ngate, promptspy | siehe Abschnitt 6 | 79 | seit Schritt 1 |
| callphantom | CallPhantom | 28 | **mitgeliefert** (README-Titel „…Tricks Android Users“, nur APK-Hashes) |
| virtual_invaders | eXotic Visit | 20 | **mitgeliefert** (Android-Kampagne) |
| prospytospy | ProSpy / ToSpy | 12 | **mitgeliefert** (UAE, Android) |
| gref | BadBazaar | 5 | **mitgeliefert** (Tabelle: nur `Android/Spy.BadBazaar.A`) |
| gravityrat | GravityRAT | 3 | **mitgeliefert** (nur `Android/Spy.Gravity.A`) |
| ghostchat | GhostChat | 2 | **mitgeliefert** (Datei ist Großbuchstaben, Parser kleinschreibt) |
| pwa_phishing | PWA-Phishing-Banker | 2 | **mitgeliefert** (beide `base.apk`, `Android/Spy.Banker`) |
| kamran | Kamran | 1 | **mitgeliefert** |
| evilvideo | EvilVideo | 1 | **mitgeliefert** (Telegram-Android-Nutzlast) |
| scarcruft | – | 14 | **weggelassen**: README mischt 6 Android- mit Windows-Hashes und weiteren Abschnitten (Dolphin …); `samples.sha256` hat 14 Zeilen, die README-Tabelle nur 9 SHA-1 – Zuordnung nicht beweisbar |
| thewizards | – | 8 | **weggelassen**: 6 Windows-, 2 Android-Zeilen; Android-Zeilen nur über die Reihenfolge zuordenbar |
| quarterly_reports/2020_Q2 | – | 24 | **weggelassen**: Sammelbericht, Android nur als Paketnamen (kein Hash-Bezug) |
| interception, evasive_panda, agrius | – | – | Treffer „mobile/apk“ sind Dateinamen/Provider, kein Android |
| alle anderen Ordner | – | – | Windows/Linux/macOS/Web, kein Android-Bezug |

Neu: 9 Ordner, **74** SHA-256, zusammen mit Schritt 1 **153**. Pin: derselbe Commit `59745502656e813f9f79fc8005243edbc04f9397`,
je Datei SHA-256 in `sources.json`. Die Dateien sind unverändert (je README-Zeile ein SHA-1, je `samples.sha256`-Zeile ein SHA-256,
Zeilenzahl gleich – so geprüft).

### 7.2 Weitere Quellen

| Quelle | Lizenz (Beleg, Abruf 2026-11-02) | Android / SHA-256 | Entscheidung |
|---|---|---|---|
| **Meta** `meta/threat-research` (GitHub) | `LICENSE`: „MIT License – Copyright (c) Meta Platforms, Inc. and its affiliates. Permission is hereby granted, free of charge … to use, copy, modify, merge, publish, distribute …“; Commit `2d664e9` (2026-08-27) | 27 CSV-Dateien; Android-Hashes nur in `2021_04_taking_action_against_hackers_in_palestine.csv`: 13 × `sha256` mit Kommentar „PSS Android Malware Hash“, daneben 1 Windows-Hash und viele Domains. `2022_malicious_mobile_apps.csv`: 355 Android-Paketnamen (kein Hash). `2023_05_malware_iocs.csv` 31 SHA-256 = NodeStealer (Windows) | **Mitgeliefert**, nur die 13 Android-Zeilen (Parser `typed-csv-android-sha256`, trennt über die Kommentarspalte; Datei unverändert gepinnt). Paketnamen **nicht** (Namens-Treffer sind laut 2.4 keine Funde). Windows-/Domain-Zeilen ignoriert |
| **Zimperium** `Zimperium/IOC` (zLabs) | GitHub-API `license: null`, kein LICENSE-File; README nur: „provided ‚as-is‘ for threat intel and defensive use only … Users assume full responsibility“ – keine Weitergaberechte | rund 40 Android-Kampagnenordner (Konfety, ClayRat, DroidLock …), 64-stellige Hex-Werte insgesamt ca. 113 800 (Regex über alle CSV/TXT, nicht je Datei auf Hashes geprüft) | **Gar nicht** eingebaut (ohne Lizenz keine Weitergabe). Nur Download-Hinweis: Nutzer könnte die Listen selbst laden; kein Pin in der App, solange die Lizenz fehlt. Rückfrage an Zimperium wäre sinnvoll (sehr gute Android-Abdeckung) |
| **Amnesty** `AmnestyTech/investigations` | **Korrektur:** README Z. 5: „These indicators are shared under the license CC-BY“ (Link auf CC BY 2.0); GitHub-API `license: null`. Die frühere Annahme „keine Lizenz“ war falsch | Android-Kampagne 2023, FinSpy u. a. als STIX, Hash-Anzahl nicht je Datei gezählt | Heute nur Download (Pin vorhanden). Mitliefern wäre mit Namensnennung zulässig, ist hier **nicht** umgesetzt (kein Hash-Zuwachs für diesen Schritt, STIX ist groß); als eigener Schritt prüfen |
| **Citizen Lab** `citizenlab/malware-indicators` | README „License“: „Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International“ (nicht in der GitHub-API erkannt). NC und SA passen nicht zu einem Apache-2.0-Paket | Android nur vereinzelt (nicht je Ordner geprüft); letzter Commit 2020-06-09 | **Nur laden** (Gerät holt gepinnt, wie bisher), **nicht mitliefern** |
| **Kaspersky** (GitHub `KasperskyLab`) | `threat-intelligence`: Apache-2.0, enthält aber nur Konnektoren (OpenCTI), **keine** IOC-Listen; securelist-IOCs stehen im Blogtext ohne Git-Repo/Lizenz | 0 | **Gar nicht** (kein Pin, keine Lizenz) |
| **Lookout** | GitHub-Organisation `lookout` hat keine IOC-Repos; Lookout-Android-Hashes stecken bereits in `mvt-indicators` (MIT) | 0 neu | **Gar nicht** (nichts Neues) |
| **Cyble** | GitHub-Organisation `cyble` ohne öffentliche Repos; IOCs nur im Blog | 0 | **Gar nicht** |
| **Check Point** | `CheckPointSW/Cyber-Research`: keine Lizenz, nur Skripte (`Malware/APT32/…`), keine Hashlisten | 0 | **Gar nicht** |
| **Trend Micro** | `trendmicro`-Organisation: Repos `tlsh`, `telfhash` u. a., keine IOC-Listen | 0 | **Gar nicht** |
| **CERT-Feeds** | `CERT-Polska` hat 92 Repos (`mwdb_iocextract`, `hsn2-*` …), keine Android-Hashliste; abuse.ch siehe Abschnitt 1 | 0 | **Gar nicht** |
| **Google Threat Intelligence** | kein öffentliches IOC-Repo gefunden | 0 | offen |

Hinweis: Die Websuche war am Abrufstag nicht erreichbar; Blog-/Webseitenbedingungen der Firmen (Kaspersky, Lookout, Cyble, Check
Point, Trend Micro) wurden daher **nicht** gelesen. Sie kommen ohne Git-Repo ohnehin nicht in Frage (kein Pin).

### 7.3 Positivliste (Konzept 2.4)
`ioc/KnownGood`: SHA-256 der leeren Datei, eines einzelnen NUL-Bytes, von `\n` und `\r\n`. `IocSet` beantwortet für diese Werte nie
eine Suche; `IocStore.load()` verwirft eine Quelle, die einen davon enthält, als Ganzes (erscheint als „fehlerhaft“ in `IocStatus`).
Test: keine mitgelieferte Quelle trifft die Liste; eine vergiftete Quelle erzeugt keinen Fund. **Ehrliche Grenze:** Hashes echter
Systembibliotheken/-apps ließen sich hier nicht erheben (kein Gerätezugriff in dieser Sitzung, die Werte sind je Android-Build
verschieden). Die Liste ist erweiterbar (`KnownGood.trivial`); eine geräteabgeleitete Liste (A4) bleibt offen.

### 7.4 Ergebnis
Hashes insgesamt (SHA-256, Kategorie Malware): **166** = ESET 153 (14 Ordner) + Meta 13. Quellen in `sources.json`: 44.
Offene Punkte: Amnesty als mitlieferbar prüfen, Zimperium-Lizenz anfragen, Systemhash-Positivliste vom Gerät.
## Gerätetest Schritt 1 (2026-11-02)
Aufbau: Emulator API 34, Debug-Build, Dateizugriff erteilt; die harmlose Test-App „Testspy“ nur als Datei
`Download/rechnung.apk` (nicht installiert); als einziger Indikator deren SHA-256 (`debug-test.tsv`, nur Debug);
**Flugmodus an** (kein Netz, `ping` → „Network is unreachable“).

| Kriterium | Ergebnis |
|---|---|
| A3 – APK in Downloads wird erkannt | ✅ 1 Fund „Datei /storage/emulated/0/Download/rechnung.apk“ (Bild `docs/shots/av-file-finding-airplane.png`); mit zusätzlich installierter Test-App 2 Funde (installierte APK + Datei) |
| A4 – 0 Falschtreffer auf echtem Gerät | ✅ Pixel 9: 703 APK-Dateien gegen 79 ESET-Hashes, 0 Treffer |
| A5 – Prüfung offline | ✅ kompletter Scan im Flugmodus, 0 Abstürze |
| A6 – Leistung | Emulator: Basis-Scan inkl. Datei-Fingerabdrücken 25 s (768 geprüft). Pixel 9 (Release, tiefe Prüfung): 58 s gesamt; der Hash-Abgleich selbst ist eine Binärsuche und fällt nicht ins Gewicht. |
| A4 nach Schritt 2 | ✅ Pixel 9: 703 APK-Dateien gegen 161 eindeutige Malware-SHA-256 (ESET 14 Kampagnen + Meta), 0 Treffer; alle 15 Quellen-Pins an der Quelle nachgerechnet |

## 6. Zustimmung abuse.ch für MalwareBazaar (2026-11-05)

Ticket ST7319719 (Spamhaus/abuse.ch), Anfrage 2026-11-02, Antwort des abuse.ch-Teams 2026-11-05. Die Zustimmung gilt nach
Ermessen von abuse.ch und kann schriftlich widerrufen werden. Bedingungen (sinngemäß, verbindlich):

| # | Bedingung | Umsetzung in Spyhound |
|---|---|---|
| 1 | Nur Android-/APK-Samples: SHA-256, Familie/Tag, Link zum MalwareBazaar-Eintrag. Keine Samples, keine weiteren Metadaten | Skript filtert Dateityp APK und schreibt nur diese drei Felder |
| 2 | Spyhound bleibt kostenlos, nicht-kommerziell, quelloffen; keine Bezahlstufe, Werbung, Monetarisierung. Sonst vorher fragen | Spenden ohne Funktionsfreischaltung bleiben so; jede Änderung daran erst nach Rückfrage bei abuse.ch |
| 3 | Bezug über authentifiziertes Konto (auth.abuse.ch) und offizielle API/Export, höchstens 1×/Woche, kein Scrapen | Auth-Key nur im Vault; Skript verweigert einen zweiten Abruf innerhalb von 7 Tagen |
| 4 | Kein Training/Anpassen von KI-Modellen mit den Daten | Daten nur für den Hash-Abgleich auf dem Gerät |
| 5 | Nicht als eigene Liste ausgeben, nicht verkaufen; Weitergabe nur als Teil von Spyhound, nicht als eigenständiger Datensatz. Forks nur unter denselben Bedingungen | Hinweis in NOTICE und README; Liste liegt im Spyhound-Repo, nicht in einem eigenen Daten-Repo |
| 6 | Einträge auf Zuruf (z. B. Falschtreffer, Takedown) in angemessener Zeit entfernen | Sperrliste im Skript + nächstes Release |

Pflicht-Hinweis (App und Repository), Wortlaut von abuse.ch:

> Malware hash data courtesy of abuse.ch MalwareBazaar (https://bazaar.abuse.ch), provided in collaboration with Spamhaus
> Technology. Used with permission. Data is provided as-is, without guarantee of accuracy; detections may include false
> positives.

Kein offene Lizenz: die Liste **nicht** als CC0 kennzeichnen, sondern „subject to the abuse.ch Terms of Use, included with
permission“. Weil die Daten nicht frei lizenziert sind, kommen sie nicht in die APK (F-Droid: NonFreeAssets), sondern werden
wie Amnesty/Citizen Lab nur auf dem Gerät geladen.

Nachtrag 2026-11-05: Der GitHub-Sponsors-Link (freiwillige Spenden, keine Gegenleistung, keine freigeschalteten
Funktionen) wurde abuse.ch im Ticket offengelegt; abuse.ch hat bestätigt, dass das mit Bedingung 2 vereinbar ist.
