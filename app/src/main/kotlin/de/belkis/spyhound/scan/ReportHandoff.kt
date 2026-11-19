package de.belkis.spyhound.scan

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * A minimal summary of a check that the person can hand to another app through the share sheet,
 * for example one that wants to warn before restoring keys on a phone with findings.
 *
 * Format proposed by Damadori (IA-DAMA) in issue #3. It holds what a receiver needs and nothing more:
 * version, time, check depth, separate counts, the published name and source of each matched indicator,
 * and every gap. It never holds the installed apps, the device name, paths, logs, or the matched value
 * itself (package name, domain, hash), so a shared summary is not an inventory of the phone.
 *
 * It is unsigned and only a snapshot: the time is the phone's own clock, and the text can be edited,
 * replayed or invented. A receiver must treat it as advice, never as proof, and "no findings" never
 * means a clean phone. The `notice` field says so in every copy.
 */
object ReportHandoff {
    const val FORMAT = "spyhound-local-handoff"
    const val FORMAT_VERSION = 1
    const val NOTICE = "Unsigned user-shared snapshot, not device attestation. " +
        "No findings never means a clean device."

    private val json = Json { prettyPrint = true; prettyPrintIndent = "  " }

    fun render(r: Report, version: String): String = json.encodeToString(JsonObject.serializer(), build(r, version))

    fun build(r: Report, version: String): JsonObject = buildJsonObject {
        put("format", FORMAT)
        put("format_version", FORMAT_VERSION)
        put("spyhound_version", version)
        put("checked_at_utc", Instant.ofEpochMilli(r.startedAt).truncatedTo(ChronoUnit.SECONDS).toString())
        put("check_depth", depth(r.access))
        putJsonObject("counts") {
            put("findings", r.findings.size)
            put("leads", r.leads.size)
            put("gaps", r.gaps.size)
        }
        // One entry per published label: two matches of the same app (package name and certificate)
        // are one entry here, the raw count stays in `counts`.
        putJsonArray("findings") {
            r.findings.map { indicatorName(it) to it.indicator.sourceId }.distinct().forEach { (name, source) ->
                addJsonObject {
                    put("indicator_name", name)
                    put("indicator_source", source)
                }
            }
        }
        // Stable codes instead of the gap text: the text can name a file or folder on the phone.
        putJsonArray("gaps") {
            r.checks.flatMap { c -> c.gaps.map { "${slug(TextsEn.text(c.id))}.${reason(it)}" } }
                .distinct().forEach { add(it) }
        }
        put("notice", NOTICE)
    }

    fun depth(level: AccessLevel) = when (level) {
        AccessLevel.APP -> "basic"
        AccessLevel.SHELL -> "extended"
        AccessLevel.ROOT -> "root"
    }

    /** The family named by the publisher, or the kind of list when a source names none. */
    private fun indicatorName(f: Finding) = f.indicator.family.trim().ifBlank {
        when {
            f.isStalkerware -> "Monitoring app (unnamed in source)"
            f.isMalware -> "Malware (unnamed in source)"
            else -> "Spyware (unnamed in source)"
        }
    }

    private fun reason(g: Gap) =
        if (g.fix != GapFix.NONE) "needs_" + g.fix.name.lowercase() else g.kind.name.lowercase()

    internal fun slug(s: String) =
        s.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "check" }
}
