# Antivirus offline – Konzept (Roadmap Stufe 4)

Status: **Schritt 1 umgesetzt (ESET, 5 Android-Ordner); Rest Konzept.** Stand der Recherche: **2026-11-02** (alle Quellen an diesem Tag selbst geöffnet).
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
| Bereits eingebunden: MVT (MIT), ECHAP (CC BY 4.0), Toolkit-Kurationen (CC0); Amnesty/Citizen Lab (ohne Lizenz → nur Download) | STIX 2 inkl. `file:hashes.sha-256` | siehe `docs/SPEC.md` | unverändert; deckt Spyware/Stalkerware, **nicht** Banking-Trojaner/Adware/Massenmalware |

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
1. Anfrage an abuse.ch/Spamhaus: Erlaubnis, eine Android-Teilmenge aus MalwareBazaar als gepinntes Derivat (eigenes Repo) zu
   veröffentlichen? Ohne schriftliche Zusage bleibt MalwareBazaar draußen.
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
geprüft; nur exakte Treffer. Die Positivliste aus 2.4 ist noch nicht gebaut.

Abnahme: A1, A2, A3 (JVM-Teil), A7 (Textcheck bleibt grün), A8 per `AntivirusTest`/`testDebugUnitTest`. Gerätetest offen: A3
(EICAR-Test-APK im Download-Ordner), A4 (0 Falschtreffer auf echtem Gerät), A5 (Flugmodus-Lauf), A6 (Leistung Pixel 9).

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
