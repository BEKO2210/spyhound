package de.belkis.spyhound.scan

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class HardeningId { SCREEN_LOCK, USB_DEBUGGING, WIFI_DEBUGGING, DEVELOPER_OPTIONS, SECURITY_PATCH, UNKNOWN_APP_INSTALLERS }

/** [UNKNOWN]: Android hides the setting from apps (see DebugSwitches); shown without a check mark. */
enum class HardeningState { OK, RECOMMENDED, IMPORTANT, UNKNOWN }

/** Where the "Open" button leads; the UI turns each target into the matching system settings intent. */
enum class HardeningAction { SECURITY_SETTINGS, DEVELOPER_SETTINGS, SYSTEM_UPDATE, UNKNOWN_APPS }

data class HardeningInput(
    val screenLockSet: Boolean,
    /** Null for the three debug switches: Android does not tell apps (see DebugSwitches). */
    val usbDebuggingOn: Boolean?,
    val wifiDebuggingOn: Boolean?,
    val developerOptionsOn: Boolean?,
    /** Null when the system does not report a patch date. */
    val securityPatch: LocalDate?,
    /** Labels of non-system apps that may install other apps. */
    val unknownInstallerApps: List<String>,
)

/**
 * One checklist row. [detail] carries the number the text needs: patch age in days or installer app count.
 * [note] marks a state that is expected while the deep check runs and is no mistake.
 */
data class HardeningItem(
    val id: HardeningId,
    val state: HardeningState,
    val action: HardeningAction,
    val detail: Int = 0,
    val note: Boolean = false,
)

private fun switchState(on: Boolean?) = when (on) {
    true -> HardeningState.RECOMMENDED
    false -> HardeningState.OK
    null -> HardeningState.UNKNOWN
}

const val PATCH_RECOMMENDED_DAYS = 90L
const val PATCH_IMPORTANT_DAYS = 365L

/** Pure rating of the phone's settings. The order is fixed so that rows never jump. */
fun evaluateHardening(input: HardeningInput, today: LocalDate): List<HardeningItem> {
    val items = mutableListOf<HardeningItem>()
    items += HardeningItem(
        HardeningId.SCREEN_LOCK,
        if (input.screenLockSet) HardeningState.OK else HardeningState.IMPORTANT,
        HardeningAction.SECURITY_SETTINGS,
    )
    items += HardeningItem(
        HardeningId.USB_DEBUGGING,
        switchState(input.usbDebuggingOn),
        HardeningAction.DEVELOPER_SETTINGS,
    )
    // Wireless debugging is how the deep check works, so it is a reminder, not a fault.
    items += HardeningItem(
        HardeningId.WIFI_DEBUGGING,
        switchState(input.wifiDebuggingOn),
        HardeningAction.DEVELOPER_SETTINGS,
        note = input.wifiDebuggingOn == true,
    )
    items += HardeningItem(
        HardeningId.DEVELOPER_OPTIONS,
        switchState(input.developerOptionsOn),
        HardeningAction.DEVELOPER_SETTINGS,
    )
    input.securityPatch?.let { patch ->
        val age = ChronoUnit.DAYS.between(patch, today).coerceAtLeast(0)
        val state = when {
            age > PATCH_IMPORTANT_DAYS -> HardeningState.IMPORTANT
            age > PATCH_RECOMMENDED_DAYS -> HardeningState.RECOMMENDED
            else -> HardeningState.OK
        }
        items += HardeningItem(HardeningId.SECURITY_PATCH, state, HardeningAction.SYSTEM_UPDATE, detail = age.toInt())
    }
    items += HardeningItem(
        HardeningId.UNKNOWN_APP_INSTALLERS,
        if (input.unknownInstallerApps.isEmpty()) HardeningState.OK else HardeningState.RECOMMENDED,
        HardeningAction.UNKNOWN_APPS,
        detail = input.unknownInstallerApps.size,
    )
    return items
}
