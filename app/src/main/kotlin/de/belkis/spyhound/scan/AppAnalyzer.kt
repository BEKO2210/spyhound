package de.belkis.spyhound.scan

import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.Indicator
import de.belkis.spyhound.ioc.IocSet
import de.belkis.spyhound.ioc.IocType

data class ApkFile(val path: String, val sha256: String?)

/** Everything the scan knows about one installed app, collected on the device. */
data class AppRecord(
    val packageName: String,
    val label: String = packageName,
    val apks: List<ApkFile> = emptyList(),
    val certSha1: List<String> = emptyList(),
    val certSha256: List<String> = emptyList(),
    val installer: String? = null,
    val isSystem: Boolean = false,
    val firstInstall: Long = 0,
    val grantedPermissions: Set<String> = emptySet(),
    val accessibility: Boolean = false,
    val notificationAccess: Boolean = false,
    val deviceAdmin: Boolean = false,
    /** Hidden from the launcher: no icon in the app drawer. */
    val hiddenFromLauncher: Boolean = false,
)

/**
 * Compares installed apps with the published app ids, APK fingerprints and signing
 * certificates, and flags apps installed outside any store together with what they
 * are allowed to do - the pattern stalkerware needs.
 */
object AppAnalyzer {
    const val NAME = "Apps"

    /** Installers that are app stores. Anything else installed a third-party app by hand. */
    val STORES = setOf(
        "com.android.vending", "app.grapheneos.apps", "org.fdroid.fdroid", "org.fdroid.basic",
        "com.aurora.store", "com.amazon.venezia", "com.sec.android.app.samsungapps",
        "com.huawei.appmarket", "com.xiaomi.market", "com.xiaomi.mipicks", "com.heytap.market",
        "com.oppo.market", "com.vivo.appstore", "com.looker.droidify", "com.machiav3lli.fdroid",
        "dev.imranr.obtainium", "com.google.android.packageinstaller.system",
        // Samsung's own update paths: the preinstalled app updater (Clock, Calendar …) and the Galaxy Buds plugin
        // installer. Both are system apps, so no other app can carry their names.
        "com.samsung.android.app.updatecenter", "com.samsung.accessory.budsunitemgr",
    )

    val POWERS = linkedMapOf(
        "android.permission.READ_SMS" to "SMS lesen",
        "android.permission.RECEIVE_SMS" to "SMS mitlesen",
        "android.permission.RECORD_AUDIO" to "Mikrofon",
        "android.permission.CAMERA" to "Kamera",
        "android.permission.ACCESS_BACKGROUND_LOCATION" to "Standort im Hintergrund",
        "android.permission.ACCESS_FINE_LOCATION" to "genauer Standort",
        "android.permission.READ_CALL_LOG" to "Anrufliste",
        "android.permission.READ_CONTACTS" to "Kontakte",
        "android.permission.REQUEST_INSTALL_PACKAGES" to "Apps installieren",
        "android.permission.READ_PHONE_STATE" to "Telefonstatus",
        "android.permission.SYSTEM_ALERT_WINDOW" to "über anderen Apps zeichnen",
        "android.permission.MANAGE_EXTERNAL_STORAGE" to "alle Dateien",
    )

    private const val WHY_SIDELOAD =
        "Überwachungs-Apps werden fast immer per Hand installiert, nicht aus einem App-Store. " +
            "Viele harmlose Apps kommen aber auch so aufs Gerät (z. B. von der Herstellerseite)."
    private const val WHY_POWER =
        "Bedienungshilfe, Benachrichtigungszugriff und Geräteadministrator erlauben, " +
            "den Bildschirm mitzulesen oder das Löschen zu verhindern. Seriöse Apps wie Passwortmanager nutzen sie auch."

    fun analyze(apps: List<AppRecord>, iocs: IocSet, ownPackage: String, texts: Texts = TextsDe): CheckResult {
        val r = CheckResult(NAME, texts)
        var unhashed = 0
        var noCert = 0
        for (app in apps) {
            if (app.packageName == ownPackage) continue
            r.examined++
            val where = "App ${app.label} (${app.packageName})"
            iocs.lookup(IocType.APP_ID, app.packageName)?.let { ind ->
                val text = if (ind.category == Category.STALKERWARE) {
                    "Eine bekannte Überwachungs-App ist installiert${family(ind.family)}. " +
                        "Manche davon werden offen als Kinderschutz-App verkauft."
                } else {
                    "Eine App, die Forscher einer Spionagesoftware zuordnen, ist installiert${family(ind.family)}."
                }
                r.hit(where, text, ind)
            }
            for (apk in app.apks) {
                val sha = apk.sha256
                if (sha == null) { if (!app.isSystem) unhashed++; continue }
                iocs.lookup(IocType.SHA256, sha)?.let {
                    r.hit("$where – ${apk.path}", hashText(it), it)
                }
            }
            if (app.certSha1.isEmpty() && app.certSha256.isEmpty() && !app.isSystem) noCert++
            for (cert in app.certSha1 + app.certSha256) {
                iocs.lookup(IocType.APP_CERT, cert)?.let {
                    r.hit(where, "Die App ist mit einem Zertifikat signiert, das Forscher einer Spyware zuordnen${family(it.family)}.", it)
                }
            }
            if (app.isSystem) continue
            val powers = powersOf(app)
            val sideloaded = app.installer == null || app.installer !in STORES
            if (sideloaded) {
                var text = "Nicht aus einem App-Store installiert (Installiert von: ${app.installer ?: "unbekannt"})"
                if (powers.isNotEmpty()) text += ". Darf: " + powers.joinToString(", ")
                if (app.hiddenFromLauncher) text += ". Hat kein Symbol im App-Menü"
                // Like the desktop toolkit: a sideloaded app is only worth a look together with powers.
                if (powers.isNotEmpty() || app.hiddenFromLauncher) r.lead(where, text, WHY_SIDELOAD, LeadKind.SIDELOADED_APP)
            } else if (app.accessibility || app.deviceAdmin) {
                val what = listOfNotNull(
                    "Bedienungshilfe (kann den Bildschirm lesen)".takeIf { app.accessibility },
                    "Geräteadministrator (erschwert Deinstallation)".takeIf { app.deviceAdmin },
                )
                r.lead(where, "Hat " + what.joinToString(" und ") + ".", WHY_POWER, LeadKind.APP_POWERS)
            }
        }
        r.malwareCoverage = MalwareCoverage.from(iocs)
        if (unhashed > 0) r.gap("$unhashed App-Datei(en) konnten nicht gelesen und daher nicht mit den Fingerabdrücken verglichen werden.", kind = GapKind.APP_FILES)
        if (noCert > 0) r.gap("Bei $noCert App(s) war das Signatur-Zertifikat nicht lesbar.", kind = GapKind.CERTIFICATES)
        r.ran = true
        return r
    }

    fun powersOf(app: AppRecord): List<String> = buildList {
        POWERS.forEach { (perm, label) -> if (perm in app.grantedPermissions) add(label) }
        if (app.notificationAccess) add("alle Benachrichtigungen lesen")
        if (app.accessibility) add("Bedienungshilfe (kann den Bildschirm lesen)")
        if (app.deviceAdmin) add("Geräteadministrator (erschwert Deinstallation)")
    }

    /** Detail text for an APK fingerprint match; malware lists get their own wording and source. */
    fun hashText(ind: Indicator): String =
        if (ind.category == Category.MALWARE) {
            "Die App-Datei hat den Fingerabdruck einer bekannten Schadsoftware-Probe${family(ind.family)}, Quelle: ${sourceName(ind)}."
        } else {
            "Die App-Datei hat den Fingerabdruck einer veröffentlichten Spyware-Probe${family(ind.family)}."
        }

    fun sourceName(ind: Indicator) = when {
        ind.sourceId.startsWith("eset-") -> "ESET"
        ind.sourceId.startsWith("meta-") -> "Meta"
        else -> ind.sourceId
    }

    /** Honest limit of the malware hash list, shown whenever it is loaded. */
    fun malwareCoverage(iocs: IocSet): String? {
        return MalwareCoverage.from(iocs)?.reportLine(java.util.Locale.GERMAN)
    }

    private fun family(f: String) = if (f.isBlank()) "" else " ($f)"
}
