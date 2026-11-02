# Shizuku ohne PC einrichten

Mit Shizuku kann Spyhound tiefer prüfen (laufende Prozesse, Systemprotokolle, alle Pakete). Das geht ohne PC und ohne
Root, braucht aber **Android 11 oder neuer** und eine WLAN-Verbindung. Dauer: etwa 10 Minuten.

Grundlage ist die offizielle Anleitung: <https://shizuku.rikka.app/guide/setup/>. Bezeichnungen in den Menüs können je
nach Hersteller leicht abweichen.

**Gut zu wissen:** Shizuku gibt Spyhound nur Rechte, die Android für „ADB“ vorsieht. Nach jedem Neustart des Telefons
muss Shizuku erneut gestartet werden (Schritt 4); das Koppeln (Schritt 3) ist nur einmal nötig.

## 1. Entwickleroptionen aktivieren

1. Öffnen Sie **Einstellungen → Über das Telefon**.
2. Tippen Sie siebenmal schnell auf **Build-Nummer**. Android meldet, dass Sie nun Entwickler sind
   (evtl. fragt es nach Ihrer PIN).
3. In den Einstellungen gibt es jetzt **Entwickleroptionen** (je nach Gerät unter „System“ oder ganz unten).
4. Öffnen Sie sie, schalten Sie die Entwickleroptionen ein und aktivieren Sie **USB-Debugging**.

## 2. Shizuku installieren

1. Installieren Sie die App **Shizuku** (Quelle: <https://shizuku.rikka.app/download/>).
2. Öffnen Sie Shizuku einmal. Es zeigt „Shizuku läuft nicht“; das ist normal.
3. Erlauben Sie Shizuku, **im Hintergrund zu laufen** (Akku-Einstellungen: „Nicht optimieren“ bzw. „Uneingeschränkt“).
   Sonst bricht das Koppeln oft ab.

## 3. Koppeln über WLAN-Debugging (nur beim ersten Mal)

1. Verbinden Sie das Telefon mit Ihrem WLAN.
2. Öffnen Sie in den **Entwickleroptionen** den Punkt **Debugging über WLAN** und schalten Sie ihn ein
   (Hinweis zum Netzwerk mit „Zulassen“ bestätigen).
3. Öffnen Sie Shizuku und wählen Sie **Über WLAN-Debugging starten → Koppeln**.
4. Tippen Sie in den Einstellungen unter „Debugging über WLAN“ auf **Gerät über einen Kopplungscode koppeln**. Es erscheint ein
   sechsstelliger Code.
5. Geben Sie den Code in der **Benachrichtigung von Shizuku** ein (Antwortfeld in der Benachrichtigung).
   Wenn die Einstellungen dabei verschwinden, hilft der Mehrfenster-Modus (Einstellungen und Benachrichtigung
   gleichzeitig sichtbar).

## 4. Shizuku starten

1. Öffnen Sie Shizuku und tippen Sie auf **Starten** (bei WLAN-Debugging).
2. Oben zeigt Shizuku nun „Shizuku läuft“. Falls nicht: WLAN-Debugging aus- und wieder einschalten, dann erneut starten.

## 5. In Spyhound verbinden

1. Öffnen Sie Spyhound und wählen Sie die Stufe **Shizuku** (Verbinden).
2. Android fragt, ob Spyhound Shizuku nutzen darf. Tippen Sie auf **Erlauben**.
3. Spyhound zeigt die Stufe Shizuku als aktiv. Starten Sie die Prüfung.

## 6. WLAN-Debugging wieder ausschalten

Nach dem Start läuft Shizuku weiter; WLAN-Debugging wird dafür nicht mehr gebraucht. Schalten Sie es in den
Entwickleroptionen wieder aus, das ist sicherer. Beachten Sie die Hinweise unten, sonst kann Shizuku sich beenden.

Nach einem Neustart: WLAN-Debugging einschalten und Schritt 4 wiederholen, danach wieder ausschalten.

## Wenn etwas nicht klappt

- **„Suche nach Kopplungsdienst“ bleibt stehen:** Shizuku Hintergrundbetrieb erlauben (Schritt 2.3).
- **Shizuku stoppt von selbst:** Entwickleroptionen und USB-Debugging nicht ausschalten; unter Entwickleroptionen
  „Standard-USB-Konfiguration“ auf „Keine Datenübertragung“ stellen; ab Android 11 „ADB-Autorisierungs-Timeout
  deaktivieren“ aktivieren.
- Stoppt Shizuku nach dem Ausschalten des WLAN-Debuggings, lassen Sie WLAN-Debugging nur während der Prüfung an und
  schalten es danach aus.

## Samsung (One UI)

- Build-Nummer: **Einstellungen → Telefoninfo → Softwareinformationen → Build-Nummer** (siebenmal tippen).
  Die Entwickleroptionen stehen ganz unten in den Einstellungen.
- **Auto Blocker** (One UI 6.1 und neuer) kann Debugging-Funktionen und Installationen aus unbekannten Quellen
  blockieren. Ist WLAN-Debugging nicht wählbar oder die APK-Installation gesperrt, schalten Sie Auto Blocker unter
  **Einstellungen → Sicherheit und Datenschutz → Auto Blocker** vorübergehend aus und danach wieder ein.
- Shizuku braucht Hintergrundbetrieb: **Einstellungen → Apps → Shizuku → Akku → Uneingeschränkt**; die App außerdem aus
  „Schlafende Apps“ entfernen.
- Samsung beendet WLAN-Debugging gelegentlich, wenn das WLAN wechselt. Bleiben Sie im selben WLAN.

## Pixel (Android pur)

- Build-Nummer: **Einstellungen → Über das Telefon → Build-Nummer** (siebenmal tippen).
- Entwickleroptionen: **Einstellungen → System → Entwickleroptionen**.
- WLAN-Debugging: im Menü aktivieren; **Gerät über einen Kopplungscode koppeln**. Lassen Sie den Kopplungsdialog geöffnet, bis
  Sie den Code in Shizuku eingegeben haben, sonst wird der Code ungültig.
- Akku: **Einstellungen → Apps → Shizuku → Akkunutzung → Uneingeschränkt**.
- Ist ein Pixel mit GrapheneOS im Einsatz, muss laut Shizuku-Doku unter „Sicherheit“ ggf. „Secure app spawning“
  deaktiviert werden.
