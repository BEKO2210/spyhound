# Prüfverlauf

Branch: `team/history`. Lokale Speicherung der letzten 30 abgeschlossenen Prüfungen, manuell und im Hintergrund.
Vorhandener JSON-Serializer erweitert, Zugriffe synchronisiert, Versionen und beschädigte Einträge geprüft.
SMS-Absender, private Treffer-URLs, lokale Fundpfade, freie Funddetails und Importtitel werden nicht persistiert;
veröffentlichte Indikatoren, Zähler, Tiefe, Zeit und für die Erklärung benötigte Hinweise bleiben erhalten.
Fehlermeldungen aus Leseversuchen/Importteilen werden entfernt. Die Live-Ergebnisse bleiben unverändert.
`allowBackup=false` bleibt gesetzt; explizite Ausschlüsse verhindern Cloud-Backup und Geräteübertragung.

Start → Verlauf → Ergebnis mit Datum und Zurück. Historische Ergebnisse lassen keine Einstellungsaktionen zu;
Teilen und Neu prüfen bleiben verfügbar. Einzel- und Gesamtlöschung erfordern Bestätigung; einzelne Löschung
kann unmittelbar rückgängig gemacht werden. DE/EN-Datenschutzhinweise ergänzt.

## Prüfung

Ausgeführt: `git diff --check`, XML-Parsing und statische Prüfung der Ressourcenreferenzen/Namenseindeutigkeit.
Keine Gradle-Aufrufe ausgeführt: Vorgabe wegen gesperrter Sockets. Kein erfolgreicher Build oder Testlauf behauptet.

Noch ausführen:

```sh
./gradlew :app:testDebugUnitTest
./gradlew --max-workers=2 recordRoborazziDebug
./gradlew --max-workers=2 verifyRoborazziDebug
./gradlew :app:assembleDebug
```

Neue Testfälle: `ScanHistoryTest`, `HistoryScreenTest`, `ScreenshotTest.historyList/historyEmpty/historyResult`.
Aufzeichnung erzeugt unter `app/src/test/screenshots/` jeweils DE/EN auf Telefon und Tablet:

- `history_list_de_phone.png`, `history_list_en_phone.png`
- `history_empty_de_phone.png`, `history_empty_en_phone.png`
- `history_result_de_phone.png`, `history_result_en_phone.png`
- entsprechende Dateien mit `_tablet.png`

Diese PNGs existieren noch nicht und wurden nicht angesehen. Nach Aufzeichnung öffnen und prüfen, dann committen.
Auch `start_lower_*` ändert sich durch den neuen Einstieg. Auf dem Gerät: manueller und Hintergrundlauf,
Neustart der App, Detailansicht/Zurück/Neu prüfen, Einzel-/Gesamtlöschung, Rotation und TalkBack prüfen.
