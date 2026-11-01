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
| Emulator pixel_6 | 17 (37) | Debug | Basis | ⏳ mit `-gpu swiftshader_indirect` bootet das Image nicht (Dienst `window` fehlt, `screencap` stürzt in `GoldfishMapper::readFromHost` ab); mit `-gpu host` bootet es in 15 s | siehe unten |
| Emulator pixel_6 | 14 (34) | Debug | Shizuku | ❌ Verbindung im Emulator nicht hergestellt, Scan lief als Basis | `api34-shizuku-*.png` |
| Emulator pixel_6 | 14 (34) | Debug | Root (Magisk v31.1 via rootAVD @613caa4) | ❌ Root nicht erreicht, Scan lief als Basis | `api34-root-*.png` |

## Fremd-Dateien (nur für Tests, nicht im Repo)
- Magisk v31.1 `Magisk-v31.1.apk` SHA-256 `89cefac0a318afa39fb3880c45c87b036c05bc1ebf02a7a715b7f8d7c26ac6e7` (= GitHub-Digest)
- rootAVD Commit `613caa44371f`
- Shizuku v13.6.0 SHA-256 `6e273ab0e991c4e79bc8b1bbb9b9dd739ccac1a8712a541a214078886b7b790f`

## Offen
- Shizuku und Root im Emulator: Ursache noch unklar (Freigabe-Dialoge/Startreihenfolge). Auf echtem Gerät ist Shizuku belegt.
- Samsung One UI lässt sich nicht emulieren.
