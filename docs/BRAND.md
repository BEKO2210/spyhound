# Spyhound – Marke

## Zeichen

Ein Spürhundkopf im Profil, nach rechts. Geometrisch, wenige Flächen: langes Ohr, stumpfe Nase, Auge als Aussparung. Vor der Nase zwei Bögen. Das ist gleichzeitig Witterung und Radar. Ohne die Bögen bleibt nur ein Kopf; sie gehören zum Zeichen.

Das Zeichen liegt in `drawable/hound_mark.xml` (24×24). Elfenbein auf Tinte. Bernstein nur auf den Bögen.

## Farben

| Rolle | Hex | Verwendung |
| --- | --- | --- |
| Tinte | `#0A0E14` | Grund, Icon-Hintergrund |
| Elfenbein | `#F2EBE0` | Zeichen, stille Bögen, Text (eine Farbe für Oberfläche und Marke) |
| Bernstein | `#FFB547` | Einziger Akzent: der Bogen, ein Fund |

Die Radar-Ringe im Icon-Hintergrund nutzen `#1A2230`, dieselbe Fläche wie die erhöhte Oberfläche. Sie bleiben leise und sind kein zweiter Akzent.

## Schutzzone

Das Adaptive Icon ist 108×108 dp. Das Motiv sitzt in der inneren Zone von 66 dp (Kreis um die Mitte, 21 dp Rand). Dort bleiben Kopf und Bögen vollständig, auch unter runder Maske. Außerhalb nur der dunkle Grund mit den unterbrochenen Ringen. Kein Text im Icon.

Die einfarbige Ebene (`ic_launcher_monochrome`) ist dasselbe Zeichen in Weiß. Android färbt sie für helle und dunkle Themenicons.

## Spot-Bilder

Vier Bilder, Viewport 160×160, dasselbe Profil:

- `illu_shizuku` – Verbindung: zwei Blöcke, die Bernstein-Brücke ist die Freigabe ohne Root.
- `illu_root` – Kern: Rahmen mit Bernstein im Inneren, die tiefste Stufe.
- `illu_clean` – keine Spuren: Bögen und Ring in Elfenbein, kein Bernstein.
- `illu_alert` – Fund: Bögen in Bernstein, Bernstein-Blip auf dem Ring.

## Do

- Elfenbein auf Tinte setzen.
- Bernstein nur als Signal verwenden.
- Die Proportion halten, nicht strecken und nicht spiegeln.

## Don't

- Nicht auf hellem Grund – das Zeichen ist hell und verschwindet.
- Keine zweite Akzentfarbe, kein Verlauf, keine Schnauze mit Details.
- Kein Comic, kein Clipart, keine Schrift im Icon.
- Die Bögen nicht vom Kopf trennen und nicht durch ein anderes Symbol ersetzen.
