# Spyhound

[English version: README.md](README.md)

Spyhound ist eine Android-App, mit der auch Nicht-Techniker prüfen können, ob ihr Telefon Spuren bekannter
Spionage-Software (zum Beispiel Pegasus, Predator, Graphite) oder Stalkerware trägt. Die Prüfung läuft auf dem Telefon
selbst, ohne PC.

> **Stand:** frühe Entwicklung. Die Prüf-Engine steht; Oberfläche, Anleitungen in der App und der Import von
> Fehlerberichten entstehen noch (siehe `docs/ROADMAP.md`).

## Was Spyhound kann

Spyhound vergleicht, was es auf Ihrem Telefon sehen kann, mit **veröffentlichten Indikatoren**: Paketnamen von Apps,
APK-Prüfsummen, Signaturzertifikate, Domains, IP-Adressen und Dateinamen, die Sicherheitsforscher öffentlich gemacht
haben. Außerdem weist es auf Dinge hin, die einen genaueren Blick lohnen, etwa Apps von außerhalb eines Stores mit
sensiblen Rechten, einen offenen Bootloader oder einen sehr alten Sicherheitspatch.

Spyhound unterscheidet:

- **Funde** – ein veröffentlichter Indikator passt. Stalkerware und staatliche Spyware werden unterschiedlich
  formuliert.
- **Hinweise** – etwas Prüfenswertes, für sich nie ein Alarm.
- **Lücken** – eine Prüfung, die nicht laufen konnte, samt Erklärung, was sie schließen würde.

## Was Spyhound nicht kann

- **Kein Echtzeit-Schutz.** Es prüft einmalig, wenn Sie es starten. Es überwacht Ihr Telefon nicht im Hintergrund.
- **Es findet nur veröffentlichte Spuren.** Neue, unveröffentlichte oder spurlose Spionage-Software erkennt es nicht.
- **„Nichts gefunden“ ist keine Unbedenklichkeit.** Das Ergebnis lautet „Keine bekannten Spuren im geprüften Bereich“.
  Es beweist nicht, dass Ihr Telefon sicher ist.
- Es entfernt keine Spionage-Software. Bei Sorge siehe [Hilfe](#hilfe).

## Prüfstufen

Jede Stufe ergänzt die vorherige. Auch ohne Sonderrechte prüft Spyhound alles, was möglich ist.

| Stufe | Was Sie brauchen | Was geprüft wird |
|-------|------------------|------------------|
| **Basis** | Nichts | Installierte Apps (Paketnamen, APK-Prüfsummen, Zertifikate, Apps von außerhalb des Stores mit sensiblen Rechten); Gerätezustand (Systemeigenschaften, Sicherheitspatch, Bootloader, Root-Spuren, Debugging-Einstellungen); Links in SMS (wenn Sie den Zugriff erlauben) |
| **Shizuku** | [Shizuku](https://shizuku.rikka.app/), per WLAN-Debugging gestartet (ohne PC; Android 11+). Siehe [docs/SHIZUKU-ANLEITUNG.md](docs/SHIZUKU-ANLEITUNG.md) | Alles aus Basis, zusätzlich laufende Prozesse, vollständige Paketliste, Systemprotokolle und `dumpsys`-Ausgaben, Dateien und deren Prüfsummen |
| **Root** | Gerät mit Root (`su`) oder Shizuku als root | Alles aus Shizuku, mit den Rechten des Root-Nutzers |

Ohne höhere Stufe verbirgt die Android-Sandbox Daten, Protokolle und Prozesse anderer Apps. Spyhound zeigt das als
Lücke an, statt so zu tun, als hätte es hingesehen.

## Installation (APK, kein Play Store)

Spyhound wird nur als APK verteilt.

1. Laden Sie die APK auf Ihrem Telefon von der Release-Seite des Projekts herunter.
2. Öffnen Sie die Datei. Android fragt, ob Sie die Installation aus dieser Quelle erlauben; erlauben Sie das nur für
   diese eine Installation.
3. Installieren Sie die App und schalten Sie die Berechtigung danach wieder aus.

Signierte Release-APKs mit Prüfsummen sind geplant. Bis dahin können Sie die App selbst bauen (siehe unten), wenn Sie
sicher sein möchten, was Sie installieren.

## Datenschutz

- **Offline.** Die Prüfung nutzt kein Netzwerk.
- **Keine Telemetrie**, keine Tracker, keine Analytics, keine Werbung.
- **Netzwerk nur für Indikator-Updates.** Sie kommen ausschließlich aus einer festgelegten Quellenliste, von
  festgelegten Commits, und jede Datei muss ihrer SHA-256-Prüfsumme entsprechen, sonst wird sie abgelehnt.
  Weiterleitungen werden nicht verfolgt.
- Ergebnisse und Berichte bleiben auf Ihrem Gerät. Ob Sie sie teilen, entscheiden Sie.
- Die Verbindung zu `raw.githubusercontent.com` (GitHub, USA) übermittelt dabei technisch Ihre IP-Adresse.

Impressum, vollständige Datenschutzerklärung und Haftungshinweise: [docs/LEGAL.md](docs/LEGAL.md) (auch in der App unter
„Über Spyhound“). Spyhound erkennt nur veröffentlichte Spuren; kein Ergebnis ist eine Garantie. Verantwortlicher:
Belkis Aslani, Vogelsangstraße 32, 71691 Freiberg am Neckar, belkis.aslani@gmail.com.

## Hilfe

Wenn Sie vermuten, gezielt überwacht zu werden, verlassen Sie sich nicht allein auf diese App. Spezialisierte
Organisationen helfen kostenlos:

- **Access Now Digital Security Helpline:** <https://www.accessnow.org/help/> – help@accessnow.org
- **Amnesty International Security Lab:** <https://securitylab.amnesty.org/>

Wenn Sie um Ihre Sicherheit fürchten, überlegen Sie, ob eine sichtbare Veränderung (App löschen, Telefon
zurücksetzen) von der überwachenden Person bemerkt werden könnte.

## Aus dem Quellcode bauen

Voraussetzungen: JDK 17 oder neuer und das Android SDK (`sdk.dir` in `local.properties`).

```
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Die Debug-APK liegt danach in `app/build/outputs/apk/debug/`.

## Dokumentation

- [docs/SPEC.md](docs/SPEC.md) – Spezifikation
- [docs/ROADMAP.md](docs/ROADMAP.md) – Roadmap
- [docs/SHIZUKU-ANLEITUNG.md](docs/SHIZUKU-ANLEITUNG.md) – Shizuku ohne PC einrichten
- [docs/FEHLERBERICHT-ANLEITUNG.md](docs/FEHLERBERICHT-ANLEITUNG.md) – Fehlerbericht erstellen

## Lizenz

Apache License 2.0, siehe [LICENSE](LICENSE). Spyhound ist ein Kotlin-Nachbau der Logik des
[Zero Breach Toolkit](https://github.com/interplains/Zero-Breach-Toolkit). Herkunft und Indikatorquellen stehen in
[NOTICE](NOTICE).
