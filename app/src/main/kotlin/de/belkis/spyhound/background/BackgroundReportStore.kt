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

object BackgroundReportStore {
    private const val PREFS = "spyhound_background_report"
    private const val KEY_REPORT = "latest_report_json"

    fun save(context: Context, report: Report) {
        prefs(context).edit().putString(KEY_REPORT, encode(report).toString()).apply()
    }

    fun consume(context: Context): Report? {
        val raw = prefs(context).getString(KEY_REPORT, null) ?: return null
        prefs(context).edit().remove(KEY_REPORT).apply()
        return runCatching { decode(JSONObject(raw)) }.getOrNull()
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
        val checks = mutableListOf<CheckResult>()
        val checksArray = obj.optJSONArray("checks") ?: JSONArray()
        repeat(checksArray.length()) { index ->
            val checkObj = checksArray.getJSONObject(index)
            val check = CheckResult(checkObj.getString("id"), TextsDe).apply {
                examined = checkObj.optInt("examined", 0)
                ran = checkObj.optBoolean("ran", false)
            }
            val findingsArray = checkObj.optJSONArray("findings") ?: JSONArray()
            repeat(findingsArray.length()) { i ->
                val finding = findingsArray.getJSONObject(i)
                val ind = finding.getJSONObject("indicator")
                check.hit(
                    finding.getString("where"),
                    finding.getString("detail"),
                    Indicator(
                        value = ind.getString("value"),
                        type = enumOrDefault(ind.getString("type"), IocType.DOMAIN),
                        category = enumOrDefault(ind.getString("category"), Category.MERCENARY),
                        family = ind.getString("family"),
                        sourceId = ind.getString("sourceId"),
                        androidArtifacts = ind.optBoolean("androidArtifacts", true),
                    ),
                )
            }
            val leadsArray = checkObj.optJSONArray("leads") ?: JSONArray()
            repeat(leadsArray.length()) { i ->
                val lead = leadsArray.getJSONObject(i)
                check.lead(
                    lead.getString("where"),
                    lead.getString("detail"),
                    lead.getString("why"),
                    enumOrDefault(lead.optString("kind"), LeadKind.OTHER),
                )
            }
            val gapsArray = checkObj.optJSONArray("gaps") ?: JSONArray()
            repeat(gapsArray.length()) { i ->
                val gap = gapsArray.getJSONObject(i)
                check.gap(
                    gap.getString("detail"),
                    enumOrDefault(gap.optString("fix"), GapFix.NONE),
                    enumOrDefault(gap.optString("kind"), GapKind.OTHER),
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
            startedAt = obj.optLong("startedAt", 0L),
            finishedAt = obj.optLong("finishedAt", 0L),
            access = enumOrDefault(obj.optString("access"), AccessLevel.APP),
            indicatorCount = obj.optInt("indicatorCount", 0),
            indicatorSources = obj.optInt("indicatorSources", 0),
            indicatorSourcesMissing = obj.optInt("indicatorSourcesMissing", 0),
            checks = checks,
            device = obj.optString("device"),
            oldestEvidence = oldest,
            title = obj.optString("title").takeIf { it.isNotBlank() },
        )
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, fallback: T): T =
        runCatching { enumValueOf<T>(raw.orEmpty()) }.getOrDefault(fallback)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
