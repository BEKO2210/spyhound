package de.belkis.spyhound.ui

import android.content.Context
import de.belkis.spyhound.R
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
    fun withFinding(context: Context): Report {
        fun text(id: Int) = context.getString(id)
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
                where = text(R.string.demo_where),
                detail = text(R.string.demo_finding),
                indicator = ind,
            )
        }
        val device = CheckResult(text(R.string.demo_device_check)).apply {
            ran = true
            examined = 38
            leads += Lead(
                check = name,
                where = "Bootloader",
                detail = text(R.string.demo_lead),
                why = text(R.string.demo_why),
            )
        }
        val sms = CheckResult("SMS").apply {
            ran = false
            gap(text(R.string.demo_sms_gap), GapFix.PERMISSION_SMS)
        }
        val files = CheckResult(text(R.string.demo_files)).apply { ran = true; examined = 1204 }
        val deep = CheckResult(text(R.string.demo_deep)).apply {
            gap(
                text(R.string.demo_deep_gap),
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
            device = text(R.string.demo_device),
        )
    }
}
