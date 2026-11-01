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
    /** Language-independent type, used to pick the explanation. */
    val kind: LeadKind = LeadKind.OTHER,
)

/** A check that could not run (fully). A gap is never a clean result. */
data class Gap(
    val check: String,
    val detail: String,
    /** What would close it: a permission, Shizuku or root. */
    val fix: GapFix = GapFix.NONE,
    /** Language-independent type for gaps without a [fix]. */
    val kind: GapKind = GapKind.OTHER,
)

enum class LeadKind {
    OTHER, SIDELOADED_APP, APP_POWERS, OLD_PATCH, DEBUGGING, BOOTLOADER, SYSTEM_BUILD, SELINUX, ROOT,
    UNKNOWN_SOURCES, OLD_BUGREPORT,
}

enum class GapKind { OTHER, APP_FILES, CERTIFICATES, READ_FAILED, BUGREPORT_PART, LARGE_FILES }

enum class GapFix { NONE, PERMISSION_SMS, PERMISSION_FILES, SHIZUKU, ROOT, UPDATE_INDICATORS }

data class CheckSummary(val name: String, val examined: Int, val ran: Boolean)

class CheckResult(sourceName: String, private val texts: Texts = TextsDe) {
    /** Untranslated check id, for comparisons that must not depend on the UI language. */
    val id = sourceName
    val name = texts.text(sourceName)
    val findings = ArrayList<Finding>()
    val leads = ArrayList<Lead>()
    val gaps = ArrayList<Gap>()
    var examined = 0
    var ran = false

    fun hit(where: String, detail: String, ind: Indicator) {
        findings += Finding(name, texts.text(where), texts.text(detail), ind)
    }

    fun lead(where: String, detail: String, why: String, kind: LeadKind = LeadKind.OTHER) {
        leads += Lead(name, texts.text(where), texts.text(detail), texts.text(why), kind)
    }

    fun gap(detail: String, fix: GapFix = GapFix.NONE, kind: GapKind = GapKind.OTHER) {
        gaps += Gap(name, texts.text(detail), fix, kind)
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
    /** When set, shown as report kind (e.g. Fehlerbericht-only run). */
    val title: String? = null,
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
