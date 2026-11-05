package de.belkis.spyhound.background

import android.content.Context
import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.Indicator
import de.belkis.spyhound.ioc.IocType
import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.scan.CheckResult
import de.belkis.spyhound.scan.GapFix
import de.belkis.spyhound.scan.GapKind
import de.belkis.spyhound.scan.LeadKind
import de.belkis.spyhound.scan.Report
import de.belkis.spyhound.scan.TextsDe
import org.json.JSONArray
import org.json.JSONObject

data class ScanHistoryEntry(
    val id: Long,
    val report: Report,
    val background: Boolean = false,
) {
    val timestamp: Long
        get() = if (report.finishedAt > 0L) report.finishedAt else report.startedAt
    val access: AccessLevel
        get() = report.access
    val findingCount: Int
        get() = report.findings.size
    val leadCount: Int
        get() = report.leads.size
    val gapCount: Int
        get() = report.gaps.size
}

object BackgroundReportStore {
    private const val PREFS = "spyhound_background_report"
    private const val KEY_REPORT = "latest_report_json"
    private const val KEY_HISTORY = "scan_history_json"
    const val MAX_HISTORY = 30
    private const val MAX_TEXT_LENGTH = 512

    @Synchronized
    fun save(context: Context, report: Report) {
        prefs(context).edit().putString(KEY_REPORT, encode(sanitize(report)).toString()).apply()
    }

    @Synchronized
    fun consume(context: Context): Report? {
        val raw = prefs(context).getString(KEY_REPORT, null) ?: return null
        prefs(context).edit().remove(KEY_REPORT).apply()
        return runCatching { sanitize(decode(JSONObject(raw))) }.getOrNull()
    }

    @Synchronized
    fun record(
        context: Context,
        report: Report,
        background: Boolean = false,
        maxEntries: Int = MAX_HISTORY,
    ): ScanHistoryEntry {
        require(report.finishedAt > 0L && report.finishedAt >= report.startedAt)
        val clean = sanitize(report)
        val existing = history(context)
        val baseId = clean.finishedAt
        val id = if (existing.any { it.id == baseId }) {
            maxOf(baseId + 1L, (existing.maxOfOrNull { it.id } ?: 0L) + 1L)
        } else {
            baseId
        }
        val entry = ScanHistoryEntry(id = id, report = clean, background = background)
        val updated = (listOf(entry) + existing)
            .sortedWith(compareByDescending<ScanHistoryEntry> { it.timestamp }.thenByDescending { it.id })
            .take(maxEntries.coerceIn(1, MAX_HISTORY))
        prefs(context).edit().putString(KEY_HISTORY, encodeHistory(updated).toString()).apply()
        return entry
    }

    @Synchronized
    fun history(context: Context): List<ScanHistoryEntry> {
        val raw = prefs(context).getString(KEY_HISTORY, null) ?: return emptyList()
        return decodeHistory(raw)
    }

    @Synchronized
    fun delete(context: Context, id: Long): ScanHistoryEntry? {
        val current = history(context)
        val removed = current.firstOrNull { it.id == id } ?: return null
        val remaining = current.filterNot { it.id == id }
        val edit = prefs(context).edit().putString(KEY_HISTORY, encodeHistory(remaining).toString())
        val pending = prefs(context).getString(KEY_REPORT, null)
        if (pending != null && runCatching { decode(JSONObject(pending)).finishedAt }.getOrNull() == removed.timestamp) {
            edit.remove(KEY_REPORT)
        }
        edit.apply()
        return removed
    }

    @Synchronized
    fun restore(
        context: Context,
        entry: ScanHistoryEntry,
        maxEntries: Int = MAX_HISTORY,
    ) {
        val current = history(context).filterNot { it.id == entry.id }
        val updated = (listOf(entry.copy(report = sanitize(entry.report))) + current)
            .sortedWith(compareByDescending<ScanHistoryEntry> { it.timestamp }.thenByDescending { it.id })
            .take(maxEntries.coerceIn(1, MAX_HISTORY))
        prefs(context).edit().putString(KEY_HISTORY, encodeHistory(updated).toString()).apply()
    }

    @Synchronized
    fun clearHistory(context: Context) {
        prefs(context).edit().remove(KEY_HISTORY).remove(KEY_REPORT).apply()
    }

    internal fun sanitize(report: Report): Report {
        val cleanChecks = report.checks.map { check ->
            CheckResult(cleanText(check.id), TextsDe).also { out ->
                out.examined = check.examined.coerceAtLeast(0)
                out.ran = check.ran
                check.findings.forEach { finding ->
                    out.hit(
                        // Keep the published indicator, never a sender, private URL or local path.
                        where = cleanText(finding.indicator.value),
                        detail = "",
                        ind = finding.indicator.copy(
                            family = cleanText(finding.indicator.family),
                            sourceId = cleanText(finding.indicator.sourceId),
                        ),
                    )
                }
                check.leads.forEach { lead ->
                    out.lead(
                        where = if (lead.kind == LeadKind.OTHER) "" else cleanText(lead.where),
                        detail = if (lead.kind == LeadKind.OTHER) "" else cleanText(lead.detail),
                        why = if (lead.kind == LeadKind.OTHER) "" else cleanText(lead.why),
                        kind = lead.kind,
                    )
                }
                check.gaps.forEach { gap ->
                    out.gap(
                        detail = if (gap.kind in setOf(GapKind.READ_FAILED, GapKind.BUGREPORT_PART)) "" else cleanText(gap.detail),
                        fix = gap.fix,
                        kind = gap.kind,
                    )
                }
            }
        }
        return report.copy(
            checks = cleanChecks,
            device = cleanText(report.device),
            title = null,
        )
    }

    private fun cleanText(value: String): String =
        value.replace("\u0000", "").take(MAX_TEXT_LENGTH)

    internal fun encodeHistory(entries: List<ScanHistoryEntry>): JSONArray = JSONArray().apply {
        entries.forEach { entry ->
            put(JSONObject().apply {
                put("version", 1)
                put("id", entry.id)
                put("background", entry.background)
                put("report", encode(entry.report))
            })
        }
    }

    internal fun decodeHistory(raw: String): List<ScanHistoryEntry> {
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        val entries = mutableListOf<ScanHistoryEntry>()
        repeat(array.length()) { index ->
            val decoded = runCatching {
                val item = array.getJSONObject(index)
                decodeEntry(item)
            }.getOrNull()
            if (decoded != null) {
                entries += decoded
            }
        }
        return entries.sortedWith(
            compareByDescending<ScanHistoryEntry> { it.timestamp }.thenByDescending { it.id },
        ).distinctBy { it.id }.take(MAX_HISTORY)
    }

    private fun decodeEntry(item: JSONObject): ScanHistoryEntry {
        require(item.getInt("version") == 1) { "Unsupported history version" }
        val report = sanitize(decode(item.getJSONObject("report")))
        val id = item.getLong("id")
        require(id > 0L) { "Invalid history entry id" }
        return ScanHistoryEntry(id, report, item.getBoolean("background"))
    }

    private fun encode(report: Report): JSONObject = JSONObject().apply {
        put("startedAt", report.startedAt)
        put("finishedAt", report.finishedAt)
        put("access", report.access.name)
        put("indicatorCount", report.indicatorCount)
        put("indicatorSources", report.indicatorSources)
        put("indicatorSourcesMissing", report.indicatorSourcesMissing)
        put("device", report.device)
        put("title", report.title)
        put("checks", JSONArray().apply {
            report.checks.forEach { check ->
                put(JSONObject().apply {
                    put("id", check.id)
                    put("examined", check.examined)
                    put("ran", check.ran)
                    put("findings", JSONArray().apply {
                        check.findings.forEach { finding ->
                            put(JSONObject().apply {
                                put("where", finding.where)
                                put("detail", finding.detail)
                                put("indicator", JSONObject().apply {
                                    put("value", finding.indicator.value)
                                    put("type", finding.indicator.type.name)
                                    put("category", finding.indicator.category.name)
                                    put("family", finding.indicator.family)
                                    put("sourceId", finding.indicator.sourceId)
                                    put("androidArtifacts", finding.indicator.androidArtifacts)
                                })
                            })
                        }
                    })
                    put("leads", JSONArray().apply {
                        check.leads.forEach { lead ->
                            put(JSONObject().apply {
                                put("where", lead.where)
                                put("detail", lead.detail)
                                put("why", lead.why)
                                put("kind", lead.kind.name)
                            })
                        }
                    })
                    put("gaps", JSONArray().apply {
                        check.gaps.forEach { gap ->
                            put(JSONObject().apply {
                                put("detail", gap.detail)
                                put("fix", gap.fix.name)
                                put("kind", gap.kind.name)
                            })
                        }
                    })
                })
            }
        })
        put("oldestEvidence", JSONObject().apply {
            report.oldestEvidence.forEach { (k, v) -> put(k, v) }
        })
    }

    private fun decode(obj: JSONObject): Report {
        val startedAt = obj.getLong("startedAt")
        val finishedAt = obj.getLong("finishedAt")
        require(startedAt >= 0L && finishedAt > 0L && finishedAt >= startedAt) { "Invalid report timestamps" }
        val access = AccessLevel.valueOf(obj.getString("access"))
        val checks = mutableListOf<CheckResult>()
        val checksArray = obj.getJSONArray("checks")
        repeat(checksArray.length()) { index ->
            val checkObj = checksArray.getJSONObject(index)
            val checkId = checkObj.getString("id")
            require(checkId.isNotBlank()) { "Blank check id" }
            val check = CheckResult(checkId, TextsDe).apply {
                examined = checkObj.optInt("examined", 0)
                ran = checkObj.optBoolean("ran", false)
            }
            val findingsArray = checkObj.getJSONArray("findings")
            repeat(findingsArray.length()) { i ->
                val finding = findingsArray.getJSONObject(i)
                val ind = finding.getJSONObject("indicator")
                val indicatorValue = ind.getString("value")
                require(indicatorValue.isNotBlank()) { "Blank indicator value" }
                check.hit(
                    finding.getString("where"),
                    finding.getString("detail"),
                    Indicator(
                        value = indicatorValue,
                        type = IocType.valueOf(ind.getString("type")),
                        category = Category.valueOf(ind.getString("category")),
                        family = ind.getString("family"),
                        sourceId = ind.getString("sourceId"),
                        androidArtifacts = ind.optBoolean("androidArtifacts", true),
                    ),
                )
            }
            val leadsArray = checkObj.getJSONArray("leads")
            repeat(leadsArray.length()) { i ->
                val lead = leadsArray.getJSONObject(i)
                check.lead(
                    lead.getString("where"),
                    lead.getString("detail"),
                    lead.optString("why", ""),
                    LeadKind.valueOf(lead.getString("kind")),
                )
            }
            val gapsArray = checkObj.getJSONArray("gaps")
            repeat(gapsArray.length()) { i ->
                val gap = gapsArray.getJSONObject(i)
                check.gap(
                    gap.getString("detail"),
                    GapFix.valueOf(gap.getString("fix")),
                    GapKind.valueOf(gap.getString("kind")),
                )
            }
            checks += check
        }
        val oldest = linkedMapOf<String, Long>()
        val oldestObj = obj.optJSONObject("oldestEvidence") ?: JSONObject()
        val iter = oldestObj.keys()
        while (iter.hasNext()) {
            val key = iter.next()
            oldest[key] = oldestObj.optLong(key, 0L)
        }
        return Report(
            startedAt = startedAt,
            finishedAt = finishedAt,
            access = access,
            indicatorCount = obj.optInt("indicatorCount", 0),
            indicatorSources = obj.optInt("indicatorSources", 0),
            indicatorSourcesMissing = obj.optInt("indicatorSourcesMissing", 0),
            checks = checks,
            device = obj.optString("device"),
            oldestEvidence = oldest,
            title = obj.optString("title").takeIf { it.isNotBlank() },
        )
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
