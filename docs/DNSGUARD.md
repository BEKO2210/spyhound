# DNS-Wächter (Stufe 2)

Lokales `VpnService` ohne Remote-Server: Das System sendet DNS nur an virtuelle Adressen im Tunnel;
der Dienst leitet Anfragen an die normalen Resolver des Geräts weiter und vergleicht QNAMEs mit `IocSet.lookupHost`.
Bei Treffer: Benachrichtigung (Kanal **Warnungen**) und Eintrag in `filesDir/dns_guard_hits.json` (max. 500).
Harmlose Domains werden nicht geloggt. Standard: **nur warnen**, nicht blockieren (`DnsGuardService.BLOCK_ON_HIT = false`).

## API

```kotlin
DnsGuard.start(activity)          // ggf. VPN-Freigabe-Dialog
DnsGuard.stop(context)
DnsGuard.isRunning
DnsGuard.recentHits(context)      // Liste der letzten Treffer
```

Nach VPN-Freigabe `start()` erneut aufrufen (z. B. im Activity-Result).

## Manifest (anderer Branch — hier nur Snippet)

In `AndroidManifest.xml` unter `<application>`:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

<service
    android:name="de.belkis.spyhound.dns.DnsGuardService"
    android:permission="android.permission.BIND_VPN_SERVICE"
    android:exported="false"
    android:foregroundServiceType="specialUse">
    <intent-filter>
        <action android:name="android.net.VpnService" />
    </intent-filter>
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="dns_monitoring" />
</service>
```

**Erklärung:** `BIND_VPN_SERVICE` schützt den Dienst vor fremden App-Starts; `VpnService`-Intent-Filter ist für die System-VPN-Freigabe nötig. Foreground mit `specialUse` und der Property beschreibt den Zweck (DNS-Beobachtung ohne allgemeinen Datentunnel) für Android 14+.

## Paket

| Datei | Rolle |
|-------|--------|
| `dns/DnsPacket.kt` | IPv4/IPv6 + UDP + DNS-Namen (JVM-testbar) |
| `dns/DnsGuardService.kt` | TUN, Weiterleitung, Treffer |
| `dns/DnsGuard.kt` | Öffentliche Steuerung |
| `dns/DnsHitStore.kt` | Lokale Trefferliste |

Virtuelle DNS: `10.111.222.1/32`, `fd00:5ec::1/128` — Routen nur dorthin, übriger Verkehr unverändert.
