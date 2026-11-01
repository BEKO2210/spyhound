package de.belkis.spyhound.scan

import kotlin.math.abs
import kotlin.math.min

enum class RadarSector(val label: String) {
    APPS("Apps"),
    DEVICE("Gerät"),
    SMS("SMS"),
    FILES("Dateien"),
    DEEP("Tiefenprüfung"),
}

enum class ElementStatus {
    CLEAN,
    LEAD,
    FINDING,
}

data class SectorRadarData(
    val sector: RadarSector,
    val examined: Int = 0,
    /** Bundled blip severities, inner index first, at most [RadarMath.MAX_BLIPS]. */
    val blips: List<ElementStatus> = emptyList(),
)

data class RadarLegendCounts(
    val clean: Int = 0,
    val lead: Int = 0,
    val finding: Int = 0,
)

data class RadarProgress(
    val activeSector: RadarSector = RadarSector.APPS,
    val deepLocked: Boolean = false,
    val sectors: List<SectorRadarData> = emptyList(),
    val legend: RadarLegendCounts = RadarLegendCounts(),
)

data class BlipLayout(
    val angleDeg: Float,
    val radiusFraction: Float,
)

object RadarMath {
    const val MAX_BLIPS = 24
    const val SECTOR_COUNT = 5

    fun mergeStatus(a: ElementStatus, b: ElementStatus): ElementStatus = when {
        a == ElementStatus.FINDING || b == ElementStatus.FINDING -> ElementStatus.FINDING
        a == ElementStatus.LEAD || b == ElementStatus.LEAD -> ElementStatus.LEAD
        else -> ElementStatus.CLEAN
    }

    fun bundleBlips(elements: List<ElementStatus>, maxBlips: Int = MAX_BLIPS): List<ElementStatus> {
        if (elements.isEmpty()) return emptyList()
        if (elements.size <= maxBlips) return elements.toList()
        val chunk = (elements.size + maxBlips - 1) / maxBlips
        return elements.chunked(chunk).map { group ->
            group.fold(ElementStatus.CLEAN, ::mergeStatus)
        }
    }

    fun layoutBlipInSector(
        sectorIndex: Int,
        blipIndex: Int,
        blipCountInSector: Int,
        sectorCount: Int = SECTOR_COUNT,
    ): BlipLayout {
        val span = 360f / sectorCount
        val start = -90f + sectorIndex * span
        val n = blipCountInSector.coerceAtLeast(1)
        val angleDeg = start + span * ((blipIndex + 0.5f) / n)
        val radiusFraction = if (n == 1) {
            0.55f
        } else {
            0.28f + 0.62f * (blipIndex.toFloat() / (n - 1))
        }
        return BlipLayout(angleDeg, radiusFraction)
    }

    fun legendFromElements(elements: List<List<ElementStatus>>): RadarLegendCounts {
        var clean = 0
        var lead = 0
        var finding = 0
        elements.forEach { list ->
            list.forEach { s ->
                when (s) {
                    ElementStatus.CLEAN -> clean++
                    ElementStatus.LEAD -> lead++
                    ElementStatus.FINDING -> finding++
                }
            }
        }
        return RadarLegendCounts(clean, lead, finding)
    }

    fun buildRadar(
        active: RadarSector,
        deepLocked: Boolean,
        sectorElements: Map<RadarSector, List<ElementStatus>>,
    ): RadarProgress {
        val sectors = RadarSector.entries.map { s ->
            val raw = sectorElements[s].orEmpty()
            SectorRadarData(s, raw.size, bundleBlips(raw))
        }
        return RadarProgress(
            activeSector = active,
            deepLocked = deepLocked,
            sectors = sectors,
            legend = legendFromElements(sectorElements.values.toList()),
        )
    }

    fun statusesFromCheck(result: CheckResult): List<ElementStatus> {
        val n = result.examined
        if (n <= 0) return emptyList()
        val statuses = MutableList(n) { ElementStatus.CLEAN }
        result.findings.forEachIndexed { i, _ ->
            statuses[min(i, n - 1)] = ElementStatus.FINDING
        }
        result.leads.forEach { l ->
            val idx = abs(l.where.hashCode()) % n
            if (statuses[idx] != ElementStatus.FINDING) statuses[idx] = ElementStatus.LEAD
        }
        return statuses
    }

    fun appStatuses(apps: List<AppRecord>, result: CheckResult, ownPackage: String): List<ElementStatus> {
        return apps.filter { it.packageName != ownPackage }.map { app ->
            val pkg = app.packageName
            when {
                result.findings.any { pkg in it.where } -> ElementStatus.FINDING
                result.leads.any { pkg in it.where } -> ElementStatus.LEAD
                else -> ElementStatus.CLEAN
            }
        }
    }

    fun smsStatuses(messages: List<Message>, result: CheckResult): List<ElementStatus> =
        messages.map { msg ->
            if (result.findings.any { msg.sender in it.where }) ElementStatus.FINDING else ElementStatus.CLEAN
        }

    fun filePathStatuses(paths: List<String>, vararg results: CheckResult): List<ElementStatus> =
        paths.map { path ->
            when {
                results.any { r -> r.findings.any { path in it.where } } -> ElementStatus.FINDING
                else -> ElementStatus.CLEAN
            }
        }

    fun sectorForCheck(name: String): RadarSector? = when (name) {
        AppAnalyzer.NAME -> RadarSector.APPS
        DeviceAnalyzer.NAME -> RadarSector.DEVICE
        MessageAnalyzer.NAME, "SMS" -> RadarSector.SMS
        ShellAnalyzer.FILES, "Dateien" -> RadarSector.FILES
        ShellAnalyzer.PROCESSES, ShellAnalyzer.PACKAGES, ShellAnalyzer.LOGS, ShellAnalyzer.HASHES,
        "Tiefenprüfung", "Root-Prüfung", "Prozesse", "Pakete", "Systemprotokolle",
        "Fingerabdrücke", "Datei-Fingerabdrücke", "Laufende Prozesse", "Alle Pakete",
        -> RadarSector.DEEP
        else -> null
    }

    fun fromReport(report: Report): RadarProgress {
        val deepLocked = report.access == AccessLevel.APP &&
            report.checks.any { it.name == "Tiefenprüfung" && it.gaps.isNotEmpty() }
        val map = linkedMapOf<RadarSector, MutableList<ElementStatus>>()
        RadarSector.entries.forEach { map[it] = mutableListOf() }
        for (check in report.checks) {
            val sector = sectorForCheck(check.name) ?: continue
            if (sector == RadarSector.DEEP && deepLocked && check.examined == 0) continue
            map.getValue(sector).addAll(statusesFromCheck(check))
        }
        return buildRadar(RadarSector.DEEP, deepLocked, map)
    }
}

class RadarTracker(private val deepLocked: Boolean) {
    private val elements = Array(RadarSector.entries.size) { mutableListOf<ElementStatus>() }
    private var active = RadarSector.APPS

    fun setActive(sector: RadarSector) {
        active = sector
    }

    fun appendClean(sector: RadarSector) {
        elements[sector.ordinal].add(ElementStatus.CLEAN)
    }

    fun replaceSector(sector: RadarSector, statuses: List<ElementStatus>) {
        elements[sector.ordinal].clear()
        elements[sector.ordinal].addAll(statuses)
    }

    fun appendSector(sector: RadarSector, statuses: List<ElementStatus>) {
        elements[sector.ordinal].addAll(statuses)
    }

    fun progress(step: String, detail: String, fraction: Float): Progress {
        val map = RadarSector.entries.associateWith { s -> elements[s.ordinal].toList() }
        return Progress(step, detail, fraction, RadarMath.buildRadar(active, deepLocked, map))
    }
}
