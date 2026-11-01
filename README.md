# Spyhound

[Deutsche Version: README.de.md](README.de.md)

Spyhound is an Android app that helps non-technical people check whether their phone carries traces of known spyware
(for example Pegasus, Predator, Graphite) or stalkerware. It runs on the phone itself, without a PC.

> **Status:** early development. The core engine is in place; the user interface, in-app guides and bug-report import
> are still being built (see `docs/ROADMAP.md`).

## What Spyhound does

It compares what it can see on your phone against **published indicators of compromise**: app package names, APK
hashes, signing certificates, domains, IP addresses and file names that security researchers have made public. It also
points out things worth a closer look, such as apps installed from outside a store that hold sensitive permissions, an
unlocked bootloader, or a very old security patch.

Spyhound distinguishes between:

- **Findings** – a published indicator matches. Stalkerware and state-grade spyware are worded differently.
- **Leads** – something worth checking, never an alarm on its own.
- **Gaps** – a check that could not run, with an explanation of what would close it.

## What Spyhound does not do

- **No real-time protection.** It checks once, when you ask it to. It does not watch your phone in the background.
- **It only finds published traces.** New or unpublished spyware, or software that leaves no known trace, will not be
  detected.
- **"Nothing found" is not a clean bill of health.** The result reads "No known traces in the checked area". It does not
  prove that your phone is safe.
- It does not remove spyware. If you are concerned, see [Getting help](#getting-help).

## Check levels

Each level adds to the one before. Without special access Spyhound still does everything that is possible.

| Level | What you need | What is checked |
|-------|---------------|-----------------|
| **Basic** | Nothing | Installed apps (package names, APK hashes, certificates, sideloaded apps with sensitive permissions); device state (system properties, security patch level, bootloader, root traces, debugging settings); links in SMS messages (if you grant access) |
| **Shizuku** | [Shizuku](https://shizuku.rikka.app/) running via wireless debugging (no PC; Android 11+). See [docs/SHIZUKU-ANLEITUNG.md](docs/SHIZUKU-ANLEITUNG.md) (German) | Everything in Basic, plus running processes, the full package list, system logs and `dumpsys` output, files and their hashes |
| **Root** | A rooted device (`su`) or Shizuku running as root | Everything in Shizuku, with the access of the root user |

Without a higher level, Android's sandbox hides other apps' data, logs and processes. Spyhound lists this as a gap
instead of pretending it looked.

## Installation (APK, no Play Store)

Spyhound is distributed only as an APK.

1. Download the APK from the project's releases page on your phone.
2. Open the file. Android asks you to allow installing from this source; allow it for this one install.
3. Install, then switch the permission off again.

Signed release APKs and checksums are planned. Until then, build from source (below) if you want to be sure what you
install.

## Privacy

- **Offline.** The scan never uses the network.
- **No telemetry**, no trackers, no analytics, no ads.
- **Network use is limited to indicator updates.** Updates are fetched only from a fixed list of sources, at fixed
  commits, and every file must match its SHA-256 checksum or it is rejected. Redirects are not followed.
- Results and reports stay on your device. You decide whether to share them.

## Getting help

If you think you are being targeted by spyware, do not rely on this app alone. Specialised organisations can help
for free:

- **Access Now Digital Security Helpline:** <https://www.accessnow.org/help/> – help@accessnow.org
- **Amnesty International Security Lab:** <https://securitylab.amnesty.org/>

If you fear for your safety, think about how a visible change (removing an app, resetting the phone) might be noticed
by the person monitoring you.

## Building from source

Requirements: JDK 17 or newer and the Android SDK (set `sdk.dir` in `local.properties`).

```
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

## Documentation

- [docs/SPEC.md](docs/SPEC.md) – specification (German)
- [docs/ROADMAP.md](docs/ROADMAP.md) – roadmap (German)
- [docs/SHIZUKU-ANLEITUNG.md](docs/SHIZUKU-ANLEITUNG.md) – set up Shizuku without a PC (German)
- [docs/FEHLERBERICHT-ANLEITUNG.md](docs/FEHLERBERICHT-ANLEITUNG.md) – create a bug report (German)

## License

Apache License 2.0, see [LICENSE](LICENSE). Spyhound reimplements the logic of the
[Zero Breach Toolkit](https://github.com/interplains/Zero-Breach-Toolkit) in Kotlin. Origin and indicator sources are
credited in [NOTICE](NOTICE).
