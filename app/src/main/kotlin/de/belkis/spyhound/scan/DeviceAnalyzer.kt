package de.belkis.spyhound.scan

import de.belkis.spyhound.ioc.IocSet
import de.belkis.spyhound.ioc.IocType
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class DeviceState(
    /** System properties as read by getprop; empty if that failed. */
    val props: Map<String, String>,
    val securityPatch: String,
    val sdk: Int,
    val adbEnabled: Boolean?,
    val adbWifiEnabled: Boolean?,
    val developerOptions: Boolean?,
    /** Apps allowed to install other apps ("unknown sources"). */
    val installSources: List<String>,
    val rootTraces: List<String>,
    val selinux: String?,
)

/** Phone-wide settings that make spying easier. All of these are leads, never findings. */
object DeviceAnalyzer {
    const val NAME = "Gerät"

    private const val WHY_SECURITY =
        "Spyware nutzt bekannte Sicherheitslücken. Ein aktueller Sicherheitspatch schließt viele davon."
    private const val WHY_DEBUG =
        "Mit USB- oder WLAN-Debugging kann jemand mit kurzem Zugriff aufs Gerät Apps unbemerkt installieren. " +
            "Für Entwickler und Shizuku ist es normal."
    private const val WHY_INTEGRITY =
        "Ein entsperrter Bootloader, Root oder abgeschaltetes SELinux schwächen den Schutz des Systems. " +
            "Wenn Sie das nicht selbst eingerichtet haben, ist das ein deutliches Warnzeichen."

    fun analyze(state: DeviceState, iocs: IocSet, today: LocalDate = LocalDate.now(), shellExpected: Boolean = false, texts: Texts = TextsDe): CheckResult {
        val r = CheckResult(NAME, texts)
        r.examined = state.props.size + 1

        // Published Android system properties (Amnesty 2023 campaign and others).
        for (name in state.props.keys) {
            iocs.lookup(IocType.ANDROID_PROP, name)?.let {
                r.hit("Systemeigenschaft $name", "Eine Systemeigenschaft, die Forscher einer Spyware zuordnen, ist gesetzt.", it)
            }
        }
        if (state.props.isEmpty()) r.gap("Die Systemeigenschaften (getprop) konnten nicht gelesen werden.", GapFix.SHIZUKU)

        runCatching { LocalDate.parse(state.securityPatch) }.getOrNull()?.let { patch ->
            val days = ChronoUnit.DAYS.between(patch, today)
            if (days > 120) {
                r.lead("Sicherheitspatch ${state.securityPatch}", "Der Sicherheitspatch ist $days Tage alt.", WHY_SECURITY)
            }
        }
        if (state.adbWifiEnabled == true) {
            r.lead("WLAN-Debugging", "WLAN-Debugging ist eingeschaltet." + if (shellExpected) " (wird für Shizuku gebraucht)" else "", WHY_DEBUG)
        } else if (state.adbEnabled == true && !shellExpected) {
            r.lead("USB-Debugging", "USB-Debugging ist eingeschaltet.", WHY_DEBUG)
        }

        val p = state.props
        when (p["ro.boot.verifiedbootstate"]) {
            "orange" -> r.lead("Bootloader", "Der Bootloader ist entsperrt (verifiedbootstate=orange).", WHY_INTEGRITY)
            "yellow", "red" -> r.lead("Bootloader", "Verified Boot meldet „${p["ro.boot.verifiedbootstate"]}“.", WHY_INTEGRITY)
        }
        if (p["ro.boot.flash.locked"] == "0" && p["ro.boot.verifiedbootstate"] != "orange") {
            r.lead("Bootloader", "Der Bootloader ist entsperrt (flash.locked=0).", WHY_INTEGRITY)
        }
        if (p["ro.debuggable"] == "1" || p["ro.build.tags"]?.contains("test-keys") == true) {
            r.lead("System-Build", "Das System ist ein Debug- oder Test-Build (ro.debuggable/test-keys).", WHY_INTEGRITY)
        }
        if (state.selinux?.lowercase() == "permissive") {
            r.lead("SELinux", "SELinux ist abgeschaltet (permissive).", WHY_INTEGRITY)
        }
        if (state.rootTraces.isNotEmpty()) {
            r.lead("Root", "Spuren von Root-Werkzeugen: " + state.rootTraces.joinToString(", "), WHY_INTEGRITY)
        }
        if (state.installSources.isNotEmpty()) {
            r.lead(
                "Unbekannte Quellen",
                "Diese Apps dürfen andere Apps installieren: " + state.installSources.joinToString(", "),
                "Damit lassen sich Apps am Store vorbei installieren. Bei Browsern und Dateimanagern ist das oft gewollt.",
            )
        }
        r.ran = true
        return r
    }
}
