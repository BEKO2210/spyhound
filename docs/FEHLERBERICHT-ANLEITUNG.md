# Fehlerbericht erstellen und mit Spyhound teilen

Ein Android-Fehlerbericht enthält Protokolle, Prozess- und Paketlisten sowie Systemzustand. Spyhound kann ihn lesen
und so auch ohne Root und ohne Shizuku tiefer prüfen.

> **Stand:** Der Import in Spyhound ist in Arbeit (siehe `ROADMAP.md`). Sie können den Bericht schon erstellen; die
> Menüführung in der App kann sich noch ändern.

## Datenschutz – bitte zuerst lesen

Der Fehlerbericht enthält **private Daten**, zum Beispiel Namen installierter Apps, Gerätekennungen, WLAN-Namen,
Kontaktnamen und Teile von Nachrichten oder Benachrichtigungen. Spyhound liest ihn **nur auf Ihrem Gerät**; er wird
nicht hochgeladen und verlässt das Telefon nicht.

Teilen Sie den Bericht deshalb nur mit Spyhound und mit Personen, denen Sie vertrauen. Löschen Sie die Datei danach.

## Schritte

1. Aktivieren Sie die Entwickleroptionen (siehe [SHIZUKU-ANLEITUNG.md](SHIZUKU-ANLEITUNG.md), Schritt 1).
2. Öffnen Sie **Einstellungen → Entwickleroptionen → Fehlerbericht erstellen** (je nach Gerät „Fehlerbericht
   melden“ oder „Bugreport“).
3. Wählen Sie **Vollständiger Bericht**. („Interaktiver Bericht“ ist unvollständig.)
4. Tippen Sie auf **Bericht**/**Starten**. Das Erstellen dauert einige Minuten. Benutzen Sie das Telefon in der Zeit
   möglichst nicht.
5. Wenn der Bericht fertig ist, erscheint eine Benachrichtigung. Tippen Sie darauf und wählen Sie **Teilen**.
6. Wählen Sie **Spyhound** in der Liste der Apps. Spyhound öffnet die Datei (`bugreport-….zip`) und prüft sie.

Wird Spyhound nicht angeboten, speichern Sie den Bericht (z. B. unter „Dateien“) und öffnen Sie ihn in Spyhound über
die Auswahl einer Datei.

## Danach

- Löschen Sie die Berichtsdatei, wenn Sie sie nicht mehr brauchen (Dateien → Download bzw. Bugreports).
- Ein Ergebnis ohne Funde bedeutet „Keine bekannten Spuren im geprüften Bereich“, keine Garantie.
- Bei Sorge um Ihre Sicherheit: Access Now Helpline (<https://www.accessnow.org/help/>) oder Amnesty Security Lab
  (<https://securitylab.amnesty.org/>).
