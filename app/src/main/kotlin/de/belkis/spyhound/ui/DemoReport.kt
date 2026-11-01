package de.belkis.spyhound.ui

import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.IocType
import de.belkis.spyhound.ioc.Indicator
import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.scan.CheckResult
import de.belkis.spyhound.scan.Finding
import de.belkis.spyhound.scan.Gap
import de.belkis.spyhound.scan.GapFix
import de.belkis.spyhound.scan.Lead
import de.belkis.spyhound.scan.Report

/** Fixed demo report for debug UI screenshots (no real device data). */
object DemoReport {
    fun withFinding(): Report {
        val now = System.currentTimeMillis()
        val ind = Indicator(
            value = "evil.example",
            type = IocType.DOMAIN,
            category = Category.MERCENARY,
            family = "Predator",
            sourceId = "mvt-android",
        )
        val apps = CheckResult("Apps").apply {
            ran = true
            examined = 142
            findings += Finding(
                check = name,
                where = "DNS-Wächter / System",
                detail = "Anfrage zu bekannter Indikator-Domain (nur Demo).",
                indicator = ind,
            )
        }
        val device = CheckResult("Gerät").apply {
            ran = true
            examined = 38
            leads += Lead(
                check = name,
                where = "Bootloader",
                detail = "Bootloader-Status konnte nicht ausgelesen werden.",
                why = "Ein offener Bootloader erleichtert persistente Spyware — hier nur ein Demo-Hinweis.",
            )
        }
        val sms = CheckResult("SMS").apply {
            ran = false
            gap("SMS-Berechtigung fehlt — Links in Nachrichten wurden nicht gelesen.", GapFix.PERMISSION_SMS)
        }
        val files = CheckResult("Dateien").apply { ran = true; examined = 1204 }
        val deep = CheckResult("Tiefenprüfung").apply {
            gap(
                "Prozesse und Systemprotokolle ohne Shizuku nicht geprüft (Demo-Lücke).",
                GapFix.SHIZUKU,
            )
        }
        return Report(
            startedAt = now - 47_000,
            finishedAt = now,
            access = AccessLevel.APP,
            indicatorCount = 7850,
            indicatorSources = 26,
            indicatorSourcesMissing = 3,
            checks = listOf(apps, device, sms, files, deep),
            device = "Demo-Gerät, Android 14 (Patch 2024-09-05)",
        )
    }
}
