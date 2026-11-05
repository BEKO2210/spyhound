# Rechtliches / Legal

Stand / Date: 2026-11-02

Die deutsche Fassung ist maßgeblich. Die englische Fassung ist eine Übersetzung.
The German version is authoritative. The English version is a translation.

- [Deutsch](#deutsch)
- [English](#english)

---

# Deutsch

## 1. Impressum

Angaben nach § 5 DDG (Digitale-Dienste-Gesetz):

Belkis Aslani
Vogelsangstraße 32
71691 Freiberg am Neckar
Deutschland

E-Mail: belkis.aslani@gmail.com

Verantwortlich für redaktionelle Inhalte nach § 18 Abs. 2 MStV, soweit solche vorhanden sind:
Belkis Aslani, Anschrift wie oben.

Spyhound ist ein privates, nicht kommerzielles Open-Source-Projekt. Es gibt keine Werbung und keine Bezahlfunktionen.
Freiwillige Unterstützung ist über GitHub Sponsors möglich; sie schaltet keine Funktionen frei.

## 2. Datenschutzerklärung

### 2.1 Verantwortlicher

Belkis Aslani, Vogelsangstraße 32, 71691 Freiberg am Neckar, Deutschland, E-Mail: belkis.aslani@gmail.com

### 2.2 Grundsatz: Alles bleibt auf Deinem Gerät

Spyhound verarbeitet alle Prüfdaten ausschließlich lokal auf Deinem Gerät. Dazu gehören die Liste der installierten
Apps und ihre Berechtigungen, Gerätedaten und Einstellungen, SMS-Links, Dateien, Systemprotokolle (nur mit dem
eingebauten Prüf-Dienst, Shizuku oder Root) sowie importierte Fehlerberichte. Der Verantwortliche erhält diese Daten
nicht und kann sie nicht einsehen.

- Es gibt keine Konten und keine Registrierung.
- Es gibt keine Telemetrie, keine Analyse- oder Tracking-Dienste, keine Werbung und keine Absturzberichte an Dritte.
- Es erfolgt keine Weitergabe von Daten an Dritte.
- Prüfergebnisse, DNS-Treffer, der Verlauf von „Wer darf was“ und die Einstellungen werden nur lokal im App-Speicher
  abgelegt. Sie bleiben dort, bis Du die App deinstallierst oder ihre Daten löschst.
- Spyhound liest Informationen auf Deinem Gerät nur aus, soweit das für die von Dir ausgelöste Prüfung unbedingt
  erforderlich ist (§ 25 Abs. 2 Nr. 2 TDDDG). Es gibt keine automatisierte Entscheidung oder Profilbildung im Sinne von
  Art. 22 DSGVO; das Prüfergebnis ist eine technische Einschätzung, die nur Du siehst.
- Du bist nicht verpflichtet, Daten bereitzustellen. Ohne eine Berechtigung prüft die App einen Bereich nicht und zeigt
  das als Lücke an.

### 2.3 Einzige Netzwerkverbindung: Update der Indikator- und Domainlisten

Der Scan selbst nutzt kein Netzwerk, auch nicht die Prüfung im Hintergrund. Nur wenn Du in der App aktiv das Laden
aktualisierter Listen auslöst (Indikatoren, Tracker- und Phishing-Domains), verbindet sich die App per HTTPS mit
`raw.githubusercontent.com` (GitHub, Inc., 88 Colin P. Kelly Jr. Street, San Francisco, CA 94107, USA; für Nutzer im
EWR auch GitHub B.V., Prins Bernhardplein 200, 1097 JB Amsterdam, Niederlande; ein Unternehmen der Microsoft-Gruppe). Es werden nur Dateien aus einer festgelegten
Liste von Quell-Repositories, von festgelegten Commits geladen. Jede Datei muss ihrer SHA-256-Prüfsumme entsprechen,
Weiterleitungen werden nicht verfolgt. Dabei wird auch die MalwareBazaar-Hashliste von abuse.ch geladen, und zwar aus
dem Spyhound-Repository auf GitHub, nicht von abuse.ch selbst. Die App sendet dabei keine Daten über Dich oder Dein Gerät, Cookies werden nicht
gesetzt.

Technisch bedingt erhält GitHub bei jeder Verbindung Deine IP-Adresse sowie Standard-HTTP-Angaben (zum Beispiel
Zeitpunkt, angeforderte Datei, User-Agent). Auf die Verarbeitung dort hat der Verantwortliche keinen Einfluss; es
gilt die Datenschutzerklärung von GitHub.

- **Rechtsgrundlage:** Art. 6 Abs. 1 lit. b DSGVO (Du löst die Funktion selbst aus, die Verbindung ist dafür
  erforderlich), hilfsweise Art. 6 Abs. 1 lit. f DSGVO (berechtigtes Interesse an aktuellen, überprüfbaren
  Indikatorlisten).
- **Drittlandübermittlung:** Die Verarbeitung kann bei einem Unternehmen mit Sitz in den USA stattfinden. GitHub gibt
  in seiner Datenschutzerklärung an, nach dem EU-US Data Privacy Framework zertifiziert zu sein
  (Angemessenheitsbeschluss der EU-Kommission vom 10.07.2023, Art. 45 DSGVO). Das Gericht der EU hat den Beschluss am
  03.09.2025 bestätigt; ein Rechtsmittel beim Gerichtshof ist anhängig (C-703/25 P).
- Wenn Du die Aktualisierung nicht auslöst, findet keine Verbindung statt. Die App enthält bereits mitgelieferte
  Indikatorlisten.

### 2.4 DNS-Wächter

Der DNS-Wächter ist optional und startet nur auf Deine Anweisung. Er nutzt die Android-VPN-Schnittstelle als **lokales**
VPN auf dem Gerät; es gibt keinen VPN-Server und keinen Tunnel ins Internet. Er prüft ausschließlich DNS-Anfragen (also
die Namen aufgerufener Domains) auf dem Gerät gegen die Indikatorlisten und leitet sie unverändert an den normalen
DNS-Server Deines Netzwerks weiter. Nur falls Android keinen DNS-Server meldet, nutzt die App ersatzweise die
öffentlichen Resolver 9.9.9.9 (Quad9) oder 2606:4700:4700::1111 (Cloudflare); diese Anbieter erhalten dann die
DNS-Anfragen und Deine IP-Adresse. Anderer Datenverkehr wird nicht angefasst. Nur Treffer auf bekannte Indikatoren
werden gespeichert, und zwar ausschließlich lokal auf dem Gerät. Unauffällige Anfragen werden nicht protokolliert.
Wählst Du für eine Kategorie „Sperren“, beantwortet der Wächter die Anfrage lokal mit „Domain nicht vorhanden“; sie
verlässt das Gerät dann nicht. Während der DNS-Wächter läuft, zeigt Android eine dauerhafte Benachrichtigung und das
VPN-Symbol.

### 2.5 Eingebauter Prüf-Dienst (WLAN-Debugging)

Für die tiefe Prüfung ohne PC kann sich Spyhound über das Android-WLAN-Debugging mit dem eigenen Gerät verbinden. Die
Verbindung läuft ausschließlich über die lokale Adresse 127.0.0.1 und verlässt das Gerät nicht. Den Kopplungscode gibst
Du selbst in eine Benachrichtigung ein. Das dafür erzeugte Schlüsselpaar liegt nur im App-Speicher. Solange
WLAN-Debugging eingeschaltet ist, erlaubt Android bereits gekoppelten Geräten im selben WLAN eine Verbindung; schalte es
nach der Prüfung wieder aus (die Karte „Handy absichern“ erinnert daran).

### 2.6 Prüfung im Hintergrund

Die wöchentliche Prüfung im Hintergrund ist optional und ab Werk ausgeschaltet. Sie prüft nur im Basis-Umfang, nutzt
kein Netzwerk und meldet sich per Benachrichtigung nur bei einem neuen Fund. Ergebnisse bleiben auf dem Gerät.

### 2.7 Berechtigungen und Zweck

| Berechtigung | Zweck |
|---|---|
| Alle installierten Apps sehen (`QUERY_ALL_PACKAGES`) | App-Liste mit Indikatoren abgleichen |
| SMS lesen (`READ_SMS`), optional | Links in SMS mit Indikatoren abgleichen; Inhalt bleibt auf dem Gerät |
| Zugriff auf alle Dateien (`MANAGE_EXTERNAL_STORAGE`), optional | Dateinamen und Prüfsummen im Speicher mit Indikatoren abgleichen |
| Internet (`INTERNET`) | Laden der Indikator- und Domainlisten (Abschnitt 2.3), Verbindung zum eigenen Gerät für die tiefe Prüfung ohne PC (2.5), Weiterleitung von DNS-Anfragen durch den DNS-Wächter (2.4) |
| VPN-Dienst, Vordergrunddienst | DNS-Wächter (Abschnitt 2.4) und dessen Statusanzeige |
| Netzwerkstatus (`ACCESS_NETWORK_STATE`) | DNS-Server des aktuellen Netzwerks für den DNS-Wächter ermitteln |
| Benachrichtigungen (`POST_NOTIFICATIONS`) | Kopplungscode (2.5), neuer Fund im Hintergrund (2.6), DNS-Wächter |
| Vibration (`VIBRATE`) | kurzes spürbares Feedback während der Prüfung |
| Wachhalten, Start nach Neustart (`WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`) | nur für die optionale wöchentliche Hintergrundprüfung (2.6): Android-Dienst WorkManager plant sie und hält den Plan über einen Neustart |

Optionale Berechtigungen werden nur auf Deinen Wunsch angefragt und können jederzeit in den Android-Einstellungen
widerrufen werden. Ohne sie läuft die Prüfung eingeschränkt weiter und die App zeigt das offen als Lücke an.
Shizuku und Root sind optional und werden nur auf Deinen Wunsch genutzt.

### 2.8 Bericht teilen und Links

Ein Prüfbericht verlässt das Gerät nur, wenn Du „Bericht teilen“ wählst. Dann übergibt die App den Text an das
Android-Teilen-Menü; Du entscheidest, an welche App oder Person er geht. Der Verantwortliche hat darauf keinen Zugriff.
Prüfe den Bericht vor dem Teilen, er kann Namen installierter Apps enthalten.

Links zu Hilfe-Seiten, zu Shizuku und zu GitHub Sponsors öffnen erst auf Deinen Tipp den Browser bzw. den Play Store.
Dann gelten die Datenschutzbestimmungen der jeweiligen Seite.

### 2.9 Deine Rechte

Soweit der Verantwortliche personenbezogene Daten von Dir verarbeitet (zum Beispiel wenn Du ihm per E-Mail schreibst),
hast Du nach der DSGVO folgende Rechte:

- Auskunft (Art. 15), Berichtigung (Art. 16), Löschung (Art. 17), Einschränkung der Verarbeitung (Art. 18)
- Datenübertragbarkeit (Art. 20)
- Widerspruch gegen Verarbeitung auf Grundlage berechtigter Interessen (Art. 21)
- Widerruf einer Einwilligung mit Wirkung für die Zukunft (Art. 7 Abs. 3)

Wende Dich dazu an belkis.aslani@gmail.com. Da die Prüfdaten nur auf Deinem Gerät liegen, kannst Du sie dort selbst
einsehen und löschen. Zur Verarbeitung bei GitHub wende Dich an GitHub.

### 2.10 Beschwerderecht

Du kannst Dich bei einer Datenschutz-Aufsichtsbehörde beschweren. Zuständig für den Verantwortlichen ist:

Der Landesbeauftragte für den Datenschutz und die Informationsfreiheit Baden-Württemberg
Heilbronner Straße 35, 70191 Stuttgart (Postanschrift: Postfach 10 29 32, 70025 Stuttgart)
<https://www.baden-wuerttemberg.datenschutz.de>

### 2.11 Kontakt per E-Mail

Schreibst Du dem Verantwortlichen, werden Deine E-Mail-Adresse und der Inhalt zur Bearbeitung Deiner Anfrage
verarbeitet (Art. 6 Abs. 1 lit. f DSGVO) und danach gelöscht, sobald sie nicht mehr erforderlich sind und keine
gesetzlichen Aufbewahrungspflichten entgegenstehen.

### 2.12 Änderungen

Diese Erklärung wird angepasst, wenn sich das Verhalten der App ändert. Maßgeblich ist der Stand oben.

## 3. Haftungsausschluss und Hinweise

- **Keine Garantie.** Spyhound erkennt nur veröffentlichte Spuren bekannter Spyware und Stalkerware. Ein Ergebnis
  ohne Funde bedeutet „keine bekannten Spuren im geprüften Bereich“, nicht „sicher“ oder „sauber“. Auch ein Fund kann
  ein Falschtreffer sein. Neue oder unveröffentlichte Schadsoftware wird nicht erkannt.
- **Keine Beratung.** Spyhound ersetzt keine Rechts-, IT-Forensik- oder Sicherheitsberatung. Wenn Du vermutest,
  gezielt überwacht zu werden, verlasse Dich nicht allein auf diese App. Kostenlose Hilfe bieten:
  - Access Now Digital Security Helpline: <https://www.accessnow.org/help/> – help@accessnow.org
  - Amnesty International Security Lab: <https://securitylab.amnesty.org/>
- **Open Source.** Spyhound steht unter der Apache License 2.0 (siehe [LICENSE](../LICENSE)). Gewährleistung und
  Haftung richten sich nach dieser Lizenz, soweit gesetzlich zulässig. Unberührt bleibt die gesetzliche Haftung bei
  Vorsatz und grober Fahrlässigkeit, bei Verletzung von Leben, Körper oder Gesundheit, bei Arglist und nach dem
  Produkthaftungsgesetz sowie bei zwingenden gesetzlichen Vorschriften.
- **Eigene Verantwortung.** Prüfe nur Geräte, die Dir gehören oder deren Besitzer der Prüfung ausdrücklich zugestimmt
  hat. Wer ohne Erlaubnis fremde Geräte ausliest, kann sich strafbar machen (zum Beispiel nach §§ 202a ff. StGB).
  Handle bei einem Fund mit Bedacht: Wer Stalkerware entfernt, kann die überwachende Person warnen. Hol Dir
  bei Gefahr Hilfe bei einer Beratungsstelle.

---

# English

This is a translation. The German version above is authoritative.

## 1. Legal notice (Impressum)

Information pursuant to § 5 DDG (German Digital Services Act):

Belkis Aslani
Vogelsangstraße 32
71691 Freiberg am Neckar
Germany

Email: belkis.aslani@gmail.com

Responsible for editorial content under § 18(2) MStV (German Interstate Media Treaty), where applicable:
Belkis Aslani, address as above.

Spyhound is a private, non-commercial open-source project. There is no advertising and there are no paid features.
Voluntary support is possible via GitHub Sponsors; it does not unlock any features.

## 2. Privacy policy

### 2.1 Controller

Belkis Aslani, Vogelsangstraße 32, 71691 Freiberg am Neckar, Germany, email: belkis.aslani@gmail.com

### 2.2 Principle: everything stays on your device

Spyhound processes all check data exclusively on your device. This includes the list of installed apps and their
permissions, device data and settings, SMS links, files, system logs (only with the built-in check service, Shizuku or
root) and imported bug reports. The controller does not
receive this data and cannot see it.

- There are no accounts and no registration.
- There is no telemetry, no analytics or tracking service, no advertising and no crash reporting to third parties.
- No data is passed on to third parties.
- Results, DNS hits, the "Who can do what" history and settings are stored only in the app's local storage. They stay
  there until you uninstall the app or clear its data.
- Spyhound reads information on your device only as far as strictly necessary for the check you start (§ 25(2) no. 2
  TDDDG). There is no automated decision-making or profiling within the meaning of Art. 22 GDPR; the result is a
  technical assessment that only you see.
- You are not obliged to provide any data. Without a permission the app does not check that area and shows it as a
  gap.

### 2.3 The only network connection: indicator and domain list updates

The scan itself uses no network, and neither does the background check. Only when you actively trigger loading of
updated lists (indicators, tracker and phishing domains) does the app connect over HTTPS to `raw.githubusercontent.com`
(GitHub, Inc., 88 Colin P. Kelly Jr. Street, San Francisco, CA 94107, USA; for users in the EEA also GitHub B.V.,
Prins Bernhardplein 200, 1097 JB Amsterdam, Netherlands; part of the Microsoft group). Only files from a fixed list of source repositories, at fixed commits,
are loaded. Every file must match its SHA-256 checksum; redirects are not followed. This includes the MalwareBazaar hash list from abuse.ch, which is loaded from the
Spyhound repository on GitHub, not from abuse.ch itself. The app sends no data about you or
your device and sets no cookies.

For technical reasons GitHub receives your IP address and standard HTTP information (for example time, requested
file, user agent) with every connection. The controller has no influence on processing there; GitHub's privacy
statement applies.

- **Legal basis:** Art. 6(1)(b) GDPR (you trigger the function yourself and the connection is necessary for it),
  alternatively Art. 6(1)(f) GDPR (legitimate interest in current, verifiable indicator lists).
- **Third-country transfer:** Processing may take place at a company based in the USA. GitHub states in its privacy
  statement that it is certified under the EU-US Data Privacy Framework (adequacy decision of the European Commission
  of 10 July 2023, Art. 45 GDPR). The EU General Court upheld the decision on 3 September 2025; an appeal to the Court
  of Justice is pending (C-703/25 P).
- If you do not trigger the update, no connection is made. The app already ships with bundled indicator lists.

### 2.4 DNS guard

The DNS guard is optional and starts only on your instruction. It uses Android's VPN interface as a **local** VPN on
the device; there is no VPN server and no tunnel to the internet. It only checks DNS queries (the names of domains
being looked up) on the device against the indicator lists and forwards them unchanged to your network's normal DNS
server. Only if Android reports no DNS server does the app fall back to the public resolvers 9.9.9.9 (Quad9) or
2606:4700:4700::1111 (Cloudflare); these providers then receive the DNS queries and your IP address. Other traffic is
not touched. Only hits on known indicators are stored, and only locally on the device. Ordinary queries are not
logged. If you choose "Block" for a category, the guard answers the query locally with "domain does not exist"; it
then does not leave the device. While the DNS guard runs, Android shows a persistent notification and the VPN icon.

### 2.5 Built-in check service (wireless debugging)

For the deep check without a PC, Spyhound can connect to its own device through Android's wireless debugging. The
connection uses only the local address 127.0.0.1 and does not leave the device. You enter the pairing code yourself in
a notification. The key pair created for it is kept only in the app's storage. While wireless debugging is on, Android
lets already paired devices on the same Wi-Fi connect; turn it off after the check (the "Secure your phone" card reminds
you).

### 2.6 Background check

The weekly background check is optional and off by default. It runs the basic check only, uses no network and notifies
you only about a new finding. Results stay on the device.

### 2.7 Permissions and purpose

| Permission | Purpose |
|---|---|
| See all installed apps (`QUERY_ALL_PACKAGES`) | Match the app list against indicators |
| Read SMS (`READ_SMS`), optional | Match links in SMS against indicators; content stays on the device |
| All files access (`MANAGE_EXTERNAL_STORAGE`), optional | Match file names and checksums in storage against indicators |
| Internet (`INTERNET`) | Loading the indicator and domain lists (section 2.3), connecting to this phone itself for the deep check without a PC (2.5), passing on DNS queries in the DNS guard (2.4) |
| VPN service, foreground service | DNS guard (section 2.4) and its status display |
| Network state (`ACCESS_NETWORK_STATE`) | Find the current network's DNS server for the DNS guard |
| Notifications (`POST_NOTIFICATIONS`) | Pairing code (2.5), new background finding (2.6), DNS guard |
| Vibration (`VIBRATE`) | Short haptic feedback during the check |
| Keep awake, start after reboot (`WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`) | Only for the optional weekly background check (2.6): Android's WorkManager schedules it and keeps the schedule across a reboot |

Optional permissions are requested only at your wish and can be revoked at any time in Android settings. Without
them the check continues with reduced scope and the app shows this openly as a gap. Shizuku and root are optional and
used only at your wish.

### 2.8 Sharing a report and links

A report leaves the device only if you choose "Share report". The app then hands the text to Android's share menu; you
decide which app or person receives it. The controller has no access. Review the report before sharing; it may contain
names of installed apps.

Links to help pages, Shizuku and GitHub Sponsors open the browser or Play Store only when you tap them. The privacy
terms of that site then apply.

### 2.9 Your rights

Where the controller processes personal data about you (for example when you write by email), you have these rights
under the GDPR:

- Access (Art. 15), rectification (Art. 16), erasure (Art. 17), restriction of processing (Art. 18)
- Data portability (Art. 20)
- Objection to processing based on legitimate interests (Art. 21)
- Withdrawal of consent with effect for the future (Art. 7(3))

Contact belkis.aslani@gmail.com. Because check data is only on your device, you can view and delete it there yourself.
For processing at GitHub, contact GitHub.

### 2.10 Right to lodge a complaint

You may lodge a complaint with a data protection supervisory authority. The competent authority for the controller is:

Der Landesbeauftragte für den Datenschutz und die Informationsfreiheit Baden-Württemberg
Heilbronner Straße 35, 70191 Stuttgart, Germany (postal address: Postfach 10 29 32, 70025 Stuttgart)
<https://www.baden-wuerttemberg.datenschutz.de>

### 2.11 Contact by email

If you write to the controller, your email address and the content are processed to handle your request
(Art. 6(1)(f) GDPR) and deleted once no longer needed and no statutory retention duty applies.

### 2.12 Changes

This policy is updated when the app's behaviour changes. The date above applies.

## 3. Disclaimer and notes

- **No guarantee.** Spyhound only detects published traces of known spyware and stalkerware. A result without
  findings means "no known traces in the areas checked", not "safe" or "clean". A finding may also be a false
  positive. New or unpublished malware is not detected.
- **No advice.** Spyhound is no substitute for legal, forensic or security advice. If you suspect targeted
  surveillance, do not rely on this app alone. Free help is available from:
  - Access Now Digital Security Helpline: <https://www.accessnow.org/help/> – help@accessnow.org
  - Amnesty International Security Lab: <https://securitylab.amnesty.org/>
- **Open source.** Spyhound is licensed under the Apache License 2.0 (see [LICENSE](../LICENSE)). Warranty and
  liability are as stated in that licence, to the extent permitted by law. Statutory liability remains unaffected in
  cases of intent and gross negligence, injury to life, body or health, fraudulent concealment, under the German
  Product Liability Act, and where mandatory law applies.
- **Your responsibility.** Check only devices that you own or whose owner has expressly allowed it. Reading someone
  else's device without permission can be a criminal offence (for example under §§ 202a ff. of the German Criminal
  Code). Act with care
  after a finding: removing stalkerware may alert the person monitoring you. In case of danger, seek help from a
  support organisation.
