package de.belkis.spyhound.ui.screens

import de.belkis.spyhound.R

internal fun guidanceStringId(key: String): Int = when (key) {
    "guide_finding_app_mercenary_meaning" -> R.string.guide_finding_app_mercenary_meaning
    "guide_finding_app_mercenary_steps" -> R.string.guide_finding_app_mercenary_steps
    "guide_finding_app_stalkerware_meaning" -> R.string.guide_finding_app_stalkerware_meaning
    "guide_finding_app_stalkerware_steps" -> R.string.guide_finding_app_stalkerware_steps
    "guide_finding_file_mercenary_meaning" -> R.string.guide_finding_file_mercenary_meaning
    "guide_finding_file_mercenary_steps" -> R.string.guide_finding_file_mercenary_steps
    "guide_finding_file_stalkerware_meaning" -> R.string.guide_finding_file_stalkerware_meaning
    "guide_finding_file_stalkerware_steps" -> R.string.guide_finding_file_stalkerware_steps
    "guide_finding_process_mercenary_meaning" -> R.string.guide_finding_process_mercenary_meaning
    "guide_finding_process_mercenary_steps" -> R.string.guide_finding_process_mercenary_steps
    "guide_finding_process_stalkerware_meaning" -> R.string.guide_finding_process_stalkerware_meaning
    "guide_finding_process_stalkerware_steps" -> R.string.guide_finding_process_stalkerware_steps
    "guide_finding_prop_mercenary_meaning" -> R.string.guide_finding_prop_mercenary_meaning
    "guide_finding_prop_mercenary_steps" -> R.string.guide_finding_prop_mercenary_steps
    "guide_finding_prop_stalkerware_meaning" -> R.string.guide_finding_prop_stalkerware_meaning
    "guide_finding_prop_stalkerware_steps" -> R.string.guide_finding_prop_stalkerware_steps
    "guide_finding_link_mercenary_meaning" -> R.string.guide_finding_link_mercenary_meaning
    "guide_finding_link_mercenary_steps" -> R.string.guide_finding_link_mercenary_steps
    "guide_finding_link_stalkerware_meaning" -> R.string.guide_finding_link_stalkerware_meaning
    "guide_finding_link_stalkerware_steps" -> R.string.guide_finding_link_stalkerware_steps
    "guide_lead_sideloaded_app_meaning" -> R.string.guide_lead_sideloaded_app_meaning
    "guide_lead_sideloaded_app_steps" -> R.string.guide_lead_sideloaded_app_steps
    "guide_lead_app_powers_meaning" -> R.string.guide_lead_app_powers_meaning
    "guide_lead_app_powers_steps" -> R.string.guide_lead_app_powers_steps
    "guide_lead_old_patch_meaning" -> R.string.guide_lead_old_patch_meaning
    "guide_lead_old_patch_steps" -> R.string.guide_lead_old_patch_steps
    "guide_lead_debugging_meaning" -> R.string.guide_lead_debugging_meaning
    "guide_lead_debugging_steps" -> R.string.guide_lead_debugging_steps
    "guide_lead_bootloader_meaning" -> R.string.guide_lead_bootloader_meaning
    "guide_lead_bootloader_steps" -> R.string.guide_lead_bootloader_steps
    "guide_lead_system_build_meaning" -> R.string.guide_lead_system_build_meaning
    "guide_lead_system_build_steps" -> R.string.guide_lead_system_build_steps
    "guide_lead_selinux_meaning" -> R.string.guide_lead_selinux_meaning
    "guide_lead_selinux_steps" -> R.string.guide_lead_selinux_steps
    "guide_lead_root_meaning" -> R.string.guide_lead_root_meaning
    "guide_lead_root_steps" -> R.string.guide_lead_root_steps
    "guide_lead_unknown_sources_meaning" -> R.string.guide_lead_unknown_sources_meaning
    "guide_lead_unknown_sources_steps" -> R.string.guide_lead_unknown_sources_steps
    "guide_lead_old_bugreport_meaning" -> R.string.guide_lead_old_bugreport_meaning
    "guide_lead_old_bugreport_steps" -> R.string.guide_lead_old_bugreport_steps
    "guide_lead_other_meaning" -> R.string.guide_lead_other_meaning
    "guide_lead_other_steps" -> R.string.guide_lead_other_steps
    "guide_gap_permission_sms_meaning" -> R.string.guide_gap_permission_sms_meaning
    "guide_gap_permission_sms_steps" -> R.string.guide_gap_permission_sms_steps
    "guide_gap_permission_files_meaning" -> R.string.guide_gap_permission_files_meaning
    "guide_gap_permission_files_steps" -> R.string.guide_gap_permission_files_steps
    "guide_gap_shizuku_meaning" -> R.string.guide_gap_shizuku_meaning
    "guide_gap_shizuku_steps" -> R.string.guide_gap_shizuku_steps
    "guide_gap_root_meaning" -> R.string.guide_gap_root_meaning
    "guide_gap_root_steps" -> R.string.guide_gap_root_steps
    "guide_gap_update_indicators_meaning" -> R.string.guide_gap_update_indicators_meaning
    "guide_gap_update_indicators_steps" -> R.string.guide_gap_update_indicators_steps
    "guide_gap_app_files_meaning" -> R.string.guide_gap_app_files_meaning
    "guide_gap_app_files_steps" -> R.string.guide_gap_app_files_steps
    "guide_gap_certificates_meaning" -> R.string.guide_gap_certificates_meaning
    "guide_gap_certificates_steps" -> R.string.guide_gap_certificates_steps
    "guide_gap_read_failed_meaning" -> R.string.guide_gap_read_failed_meaning
    "guide_gap_read_failed_steps" -> R.string.guide_gap_read_failed_steps
    "guide_gap_bugreport_part_meaning" -> R.string.guide_gap_bugreport_part_meaning
    "guide_gap_bugreport_part_steps" -> R.string.guide_gap_bugreport_part_steps
    "guide_gap_large_files_meaning" -> R.string.guide_gap_large_files_meaning
    "guide_gap_large_files_steps" -> R.string.guide_gap_large_files_steps
    "guide_gap_other_meaning" -> R.string.guide_gap_other_meaning
    "guide_gap_other_steps" -> R.string.guide_gap_other_steps
    else -> error("Unknown guidance key: $key")
}
