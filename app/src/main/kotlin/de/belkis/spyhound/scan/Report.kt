package de.belkis.spyhound.scan

import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.Indicator

/** How much access the scan had. Each level adds checks; none removes any. */
enum class AccessLevel { APP, SHELL, ROOT }

/** A published indicator matched something on the device. */
data class Finding(
    val check: String,
    val where: String,
    val detail: String,
    val indicator: Indicator,
) {
    val isStalkerware: Boolean get() = indicator.category == Category.STALKERWARE
}

/** Worth a look: a combination that spyware needs, but harmless apps often have too. Never a finding. */
data class Lead(
    val check: String,
    val where: String,
    val detail: String,
    val why: String,
)

/** A check that could not run (fully). A gap is never a clean result. */
data class Gap(
    val check: String,
    val detail: String,
    /** What would close it: a permission, Shizuku or root. */
    val fix: GapFix = GapFix.NONE,
)

enum class GapFix { NONE, PERMISSION_SMS, PERMISSION_FILES, SHIZUKU, ROOT, UPDATE_INDICATORS }

data class CheckSummary(val name: String, val examined: Int, val ran: Boolean)

class CheckResult(val name: String) {
    val findings = ArrayList<Finding>()
    val leads = ArrayList<Lead>()
    val gaps = ArrayList<Gap>()
    var examined = 0
    var ran = false

    fun hit(where: String, detail: String, ind: Indicator) {
        findings += Finding(name, where, detail, ind)
    }

    fun lead(where: String, detail: String, why: String) {
        leads += Lead(name, where, detail, why)
    }

    fun gap(detail: String, fix: GapFix = GapFix.NONE) {
        gaps += Gap(name, detail, fix)
    }
}

enum class Verdict {
    /** At least one indicator from a mercenary-spyware report matched. */
    SPYWARE_INDICATOR,
    /** A known stalkerware / monitoring app matched. */
    STALKERWARE,
    /** Nothing matched, but there are things worth a look. */
    LEADS_ONLY,
    /** Nothing matched in what could be checked. Never "clean". */
    NOTHING_FOUND,
}

data class Report(
    val startedAt: Long,
    val finishedAt: Long,
    val access: AccessLevel,
    val indicatorCount: Int,
    val indicatorSources: Int,
    val indicatorSourcesMissing: Int,
    val checks: List<CheckResult>,
    val device: String,
    val oldestEvidence: Map<String, Long> = emptyMap(),
) {
    val findings get() = checks.flatMap { it.findings }
    val leads get() = checks.flatMap { it.leads }
    val gaps get() = checks.flatMap { it.gaps }

    val verdict: Verdict
        get() = when {
            findings.any { !it.isStalkerware } -> Verdict.SPYWARE_INDICATOR
            findings.isNotEmpty() -> Verdict.STALKERWARE
            leads.isNotEmpty() -> Verdict.LEADS_ONLY
            else -> Verdict.NOTHING_FOUND
        }
}
