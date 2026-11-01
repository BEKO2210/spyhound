# Spyhound – Design-Maßstab (verbindlich)

Messlatte: Apps, die man zeigen will – **Linear, Arc, Things 3, Nothing OS, Revolut, 1Password 8**. Ruhig, präzise,
hochwertig. Jede Fläche wirkt gestaltet, nicht zusammengesteckt. Zielgruppe: Menschen ohne Technikwissen, die unter
Druck stehen – also **Klarheit vor Spielerei**, aber mit spürbarer Qualität in jedem Detail.

## 1. Grundsätze
1. **Ein Blick genügt.** Jeder Bildschirm beantwortet eine Frage: Start „Bin ich bereit?“, Scan „Was passiert gerade?“,
   Ergebnis „Bin ich betroffen – und was tue ich jetzt?“.
2. **Sichtbar, was genutzt wird.** Prüftiefe, Rechte, Quellen, Wächter – als Statusleiste mit Symbolen, nie als Fließtext.
3. **Nichts springt.** Wechselnde Inhalte in festen Slots; Animationen nur über Zeichnen/`graphicsLayer`, nie über
   animierte Layoutgrößen. Beleg per Pixelvergleich.
4. **Bewegung mit Bedeutung.** Jede Animation erklärt etwas (Fortschritt, Ursache, Zustand). Keine Deko-Bewegung ohne Sinn.
5. **Einfache Sprache.** Kurze Sätze, Du/„you“, keine Fachwörter ohne Erklärung. Jeder Fund/Hinweis: „Was bedeutet
   das?“ + „Was kann ich tun?“.
6. **Barrierefrei.** Kontrast ≥ WCAG AA, Tippflächen ≥ 48 dp, TalkBack-Labels, Schrift bis 1,3× ohne Bruch, reduzierte
   Bewegung (Animator-Skala 0) → alles statisch und vollständig.

## 2. Tokens
- **Farben:** Tinte `#0A0E14` (Grund), Fläche `#111723` / erhöht `#18202E`, Kontur `#243042` (1 dp), Elfenbein `#F2EBE0`
  (Text), Elfenbein gedämpft 70 %, Bernstein `#FFB547` (einziger Akzent), Teal `#3FC9A8` (gut/aktiv), Rot `#FF5A5F`
  (nur Funde). Verläufe nur subtil (≤ 8 % Helligkeitsdifferenz), Glow nur Bernstein/Rot, max. 24 dp Radius.
- **Typo:** Space Grotesk (Display, Zahlen in Tabellenziffern), Inter (Text). Skala 12/14/16/20/28/40/56. Laufweite
  Display −2 %, Versalien-Labels +12 %.
- **Raster:** 4-dp-Basis; Seitenrand 20 dp (Telefon) / 32 dp (Tablet); Abschnittsabstand 28 dp; Radius 12 (Karten),
  24 (Pillen), Kreis.
- **Tiefe:** keine Material-Schatten; Ebenen über Flächenfarbe + 1-dp-Kontur + weiche Innenaufhellung oben (1 dp, 6 %).

## 3. Bewegung (Motion-System)
- Standard-Kurve: `CubicBezier(0.2, 0, 0, 1)` (emphasized decelerate); Ausblenden `CubicBezier(0.3, 0, 1, 1)`.
- Dauern: Mikro 120 ms, Element 220 ms, Bildschirm 380 ms, Hero 600 ms. Federn: `dampingRatio 0.8, stiffness 380`.
- **Bildschirmwechsel:** gemeinsame Elemente (Radar Start → Scan → Ergebnis-Hero) wandern per SharedTransition; sonst
  Überblendung + 16 dp Versatz.
- **Gestaffeltes Einblenden** von Listen: 40 ms je Element, max. 8 Elemente gestaffelt.
- **Zähler** zählen hoch (Tabellenziffern, feste Breite). **Fortschritt** stufenlos, nie rückwärts.
- **Haptik:** leichtes Tick bei Scan-Start, je abgeschlossenem Bereich, kräftiger bei Fund (HapticFeedback/
  VibrationEffect, abschaltbar über Systemeinstellung).
- **Effekte, wo sie Sinn ergeben:** Radar-Sweep mit Nachleuchten, aufleuchtende Punkte (= echte Elemente), weicher
  Puls am Startknopf, Ergebnis-Hero mit kurzer „Entwarnungs“-Welle (Teal) bzw. ruhigem Rot-Glühen, Shimmer statt
  Ladeflächen, AGSL-RuntimeShader (API 33+) für Radar-Glow/Rauschen mit Fallback darunter.

## 4. Bildschirme – Abnahmekriterien
- **Start:** Kopf (DE|EN, Wortmarke, ⓘ) · Radar-Knopf als Held · **Statusleiste** (Prüftiefe, SMS, Dateien, Quellen x/29,
  DNS-Wächter – Symbol + Kurzwort, Tippen erklärt/öffnet Einstellung) · darunter ruhige Liste. Scroll-Kante sichtbar.
- **Scan:** Radar mit Sektoren, Legende, Prozent, aktueller Schritt; Schrittliste mit Häkchen in echter Reihenfolge;
  Abbrechen. Kein Element verschiebt sich.
- **Ergebnis:** Hero (Symbol, Urteil, eine Zeile Kontext), Zähler, **Statusleiste „Geprüft mit“**, Gruppen
  Funde/Hinweise/Lücken, je Eintrag „Was bedeutet das?“/„Was kann ich tun?“ + passende Aktion (App-Info öffnen,
  Einstellung öffnen, Hilfe). Unten fest: Teilen + Neu prüfen.
- **Assistent / Info / Einführung:** gleiche Sprache, Illustrationen im v2-Stil, Seitenwechsel mit Bewegung.
- **Tablet/Querformat:** zwei Spalten (links Radar + Status, rechts Liste/Details), keine leeren Flächen.

## 5. Abnahme jeder Runde
1. `./gradlew :app:testDebugUnitTest :app:assembleDebug` grün.
2. Screenshots auf dem **Pixel 11 Pro XL** (DE und EN) + Emulator-Tablet; Pixelvergleich bei Animationen (kein Springen).
3. Kein Fehler im logcat (FATAL/ANR), flüssig (keine sichtbaren Ruckler beim Scan).
4. Belkis' Abnahme auf dem Pixel.
