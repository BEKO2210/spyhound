# Testmatrix

Stand 2026-11-01. Belege: Screenshots unter `docs/shots/matrix/`, Gerätetests auf dem echten Pixel siehe Roadmap.

| Gerät | Android (API) | Build | Stufe | Ergebnis | Beleg |
|---|---|---|---|---|---|
| Pixel 11 Pro XL (echt) | 17 | Debug | Basis | ✅ 16 s, 15.208 geprüft, 0 Funde, 5 Hinweise | Roadmap Stufe 1 |
| Pixel 11 Pro XL (echt) | 17 | Debug | Shizuku | ✅ 43 s, 18.228 geprüft, 0 Funde (nach Fix Falschtreffer `queriesPackages`) | Roadmap Stufe 1 |
| Emulator pixel_6 | 10 (29) | Debug | Basis | ✅ Testspy rot; Absturz `isExternalStorageManager` < API 30 gefunden und behoben (f668f44) | `api29-*.png` |
| Emulator pixel_6 | 12 (31) | Debug | Basis | ✅ Testspy rot | `api31-*.png` |
| Emulator pixel_6 | 14 (34) | Debug | Basis | ✅ Testspy rot, 756 geprüft, 11 s | `api34-*.png` |
| Emulator pixel_6 | 14 (34) | **Release (R8, signiert)** | Basis | ✅ läuft, keine Abstürze; 0 Funde korrekt (Test-Indikatoren nur im Debug-Build) | `api34-release-result.png` |
| Emulator pixel_tablet | 16 (36) | Debug | Basis | ✅ Spalte zentriert ≤ 640 dp; Querformat wirkt leer → UI/UX-Roadmap | `api36-tablet-*.png` |
| Emulator pixel_6 | 17 (37) | Debug | Basis | ⚠️ Emulator-Image auf pop-os instabil: `swiftshader_indirect` → Grafiktreiber-Absturz (`GoldfishMapper::readFromHost`); `-gpu host` → gfxstream-Fehler, Systemdienste (`activity`) fehlen. Kein App-Fehler. **Android 17 auf echtem Pixel 11 Pro XL belegt** (Basis + Shizuku). | – |
| Emulator pixel_6 | 14 (34) | Debug | Shizuku | ❌ Verbindung im Emulator nicht hergestellt, Scan lief als Basis | `api34-shizuku-*.png` |
| Emulator pixel_6 | 14 (34) | Debug | Root (Magisk v31.1 via rootAVD @613caa4) | ✅ „Full (root)“, 22 s, 5.764 geprüft, Testspy rot. Hinweis: Magisk zeigte keinen Freigabe-Dialog (`su: request rejected`), Freigabe für den Test direkt als Magisk-Policy gesetzt (= „Grant“). Magisk-`su` liegt unter `/system_ext/bin/su` | `api34-root-*.png` |

### Wiederholung 2026-11-03 (Stand mit Assistent und eingebauter Kopplung, Debug)
| Emulator | Android (API) | Stufe | Ergebnis | Beleg |
|---|---|---|---|---|
| pixel_6 | 10 (29) | Basis | ✅ Testspy rot, 496 geprüft, 0 Abstürze | `api29-*.png` |
| pixel_6 | 12 (31) | Basis | ✅ Testspy rot, 620 geprüft | `api31-*.png` |
| pixel_6 | 14 (34) | Basis | ✅ Testspy rot, 755 geprüft | `api34-*.png` |
| pixel_6 | 14 (34) | Root (Magisk) | ✅ Testspy rot | `api34-root-*.png` |
| pixel_6 | 14 (34) | Shizuku (über den Assistenten) | ✅ Tief, 1.360 geprüft, Testspy rot, 0 Abstürze | `api34-shizuku-*.png` |
| pixel_tablet | 16 (36) | Basis | ✅ Spalte 868 px ≤ 1392 px (640 dp), Testspy rot | `api36-tablet-*.png` |

## Echte Geräte (2026-11-02)
| Gerät | Android | Build | Tiefe | Ergebnis |
|---|---|---|---|---|
| Pixel 9 | 17 (API 37) | Debug | Tief (eingebaut, ohne PC) | Scan 73 s, 0 Funde, 4 Hinweise, 0 Abstürze |
| Pixel 9 | 17 (API 37) | **Release (R8, signiert)** | Tief (eingebaut) | Frisch installiert, Kopplung per Benachrichtigung, Scan 58 s, 0 Abstürze; nach Update sofort wieder Tief |
| Pixel 11 Pro XL | 17 | Debug | Tief (Shizuku, später eingebaut) | Abnahme Belkis: Kopplung + Scan, UI „läuft alles“ |

## Fremd-Dateien (nur für Tests, nicht im Repo)
- Magisk v31.1 `Magisk-v31.1.apk` SHA-256 `89cefac0a318afa39fb3880c45c87b036c05bc1ebf02a7a715b7f8d7c26ac6e7` (= GitHub-Digest)
- rootAVD Commit `613caa44371f`
- Shizuku v13.6.0 SHA-256 `6e273ab0e991c4e79bc8b1bbb9b9dd739ccac1a8712a541a214078886b7b790f`

## Offen
- Shizuku im Emulator: seit 2026-11-03 belegt (Ursache war der Testablauf, siehe Roadmap).
- Magisk-Freigabedialog erschien im Emulator nicht – auf echtem Root-Gerät prüfen.
- Samsung One UI lässt sich nicht emulieren.
