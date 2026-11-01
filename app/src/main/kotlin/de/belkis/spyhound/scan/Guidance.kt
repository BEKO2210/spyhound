package de.belkis.spyhound.scan

import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.IocType

/** One optional button under the explanation. The UI decides how to open each target. */
enum class GuidanceAction {
    APP_INFO,
    SETTINGS_SECURITY,
    SETTINGS_DEVELOPER,
    SETTINGS_ACCESSIBILITY,
    SETTINGS_UNKNOWN_SOURCES,
    PERMISSION_SMS,
    PERMISSION_FILES,
    SETUP_DEEP_CHECK,
    UPDATE_INDICATORS,
    GET_HELP,
}

/**
 * Plain-language help for one finding, lead or gap. The keys name string resources
 * ("What does this mean?" and "What can I do?"); this file has no Android dependency.
 */
data class Guidance(val meaningKey: String, val stepsKey: String, val action: GuidanceAction?)

private fun guide(id: String, action: GuidanceAction?) = Guidance("guide_${id}_meaning", "guide_${id}_steps", action)

fun guidanceFor(finding: Finding): Guidance {
    val who = if (finding.indicator.category == Category.STALKERWARE) "stalkerware" else "mercenary"
    val what = when (finding.indicator.type) {
        IocType.APP_ID, IocType.APP_CERT -> "app"
        IocType.SHA256, IocType.FILE_PATH, IocType.FILE_NAME -> "file"
        IocType.PROCESS -> "process"
        IocType.ANDROID_PROP -> "prop"
        IocType.DOMAIN, IocType.IP, IocType.URL, IocType.EMAIL -> "link"
    }
    return guide("finding_${what}_$who", GuidanceAction.GET_HELP)
}

fun guidanceFor(lead: Lead): Guidance = when (lead.kind) {
    LeadKind.SIDELOADED_APP -> guide("lead_sideloaded_app", GuidanceAction.APP_INFO)
    LeadKind.APP_POWERS -> guide("lead_app_powers", GuidanceAction.SETTINGS_ACCESSIBILITY)
    LeadKind.OLD_PATCH -> guide("lead_old_patch", GuidanceAction.SETTINGS_SECURITY)
    LeadKind.DEBUGGING -> guide("lead_debugging", GuidanceAction.SETTINGS_DEVELOPER)
    LeadKind.BOOTLOADER -> guide("lead_bootloader", null)
    LeadKind.SYSTEM_BUILD -> guide("lead_system_build", null)
    LeadKind.SELINUX -> guide("lead_selinux", null)
    LeadKind.ROOT -> guide("lead_root", null)
    LeadKind.UNKNOWN_SOURCES -> guide("lead_unknown_sources", GuidanceAction.SETTINGS_UNKNOWN_SOURCES)
    LeadKind.OLD_BUGREPORT -> guide("lead_old_bugreport", null)
    LeadKind.OTHER -> guide("lead_other", null)
}

fun guidanceFor(gap: Gap): Guidance = when (gap.fix) {
    GapFix.PERMISSION_SMS -> guide("gap_permission_sms", GuidanceAction.PERMISSION_SMS)
    GapFix.PERMISSION_FILES -> guide("gap_permission_files", GuidanceAction.PERMISSION_FILES)
    GapFix.SHIZUKU -> guide("gap_shizuku", GuidanceAction.SETUP_DEEP_CHECK)
    GapFix.ROOT -> guide("gap_root", GuidanceAction.SETUP_DEEP_CHECK)
    GapFix.UPDATE_INDICATORS -> guide("gap_update_indicators", GuidanceAction.UPDATE_INDICATORS)
    GapFix.NONE -> when (gap.kind) {
        GapKind.APP_FILES -> guide("gap_app_files", null)
        GapKind.CERTIFICATES -> guide("gap_certificates", null)
        GapKind.READ_FAILED -> guide("gap_read_failed", GuidanceAction.SETUP_DEEP_CHECK)
        GapKind.BUGREPORT_PART -> guide("gap_bugreport_part", null)
        GapKind.LARGE_FILES -> guide("gap_large_files", null)
        GapKind.OTHER -> guide("gap_other", null)
    }
}
