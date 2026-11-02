package de.belkis.spyhound.setup

import de.belkis.spyhound.shell.AdbFailure
import de.belkis.spyhound.shell.AdbSetupState

/** The three guided “set up without a PC” steps shown in the wizard. */
enum class BuiltinAdbWizardStep { WLAN_DEBUG, PAIRING_DIALOG, ENTER_CODE }

/**
 * Pure mapping from live environment + [AdbSetupState] to wizard step completion.
 * Step 1 is done when wireless debugging is on ([wirelessOn]), not from the hidden dev-options flag (Android 17).
 * Step 2 is done when the TLS pairing service was discovered ([pairingPort] while waiting or later).
 * Step 3 is done when pairing succeeded ([Paired] or [Connected]).
 */
data class BuiltinAdbWizardSteps(
    val step1Done: Boolean,
    val step2Done: Boolean,
    val step3Done: Boolean,
    val activeStep: BuiltinAdbWizardStep?,
    val failedStep: BuiltinAdbWizardStep?,
    val failure: AdbFailure?,
) {
    val setupComplete: Boolean get() = step3Done
}

fun deriveBuiltinAdbWizardSteps(
    wirelessOn: Boolean,
    state: AdbSetupState?,
    pairingPort: Int?,
): BuiltinAdbWizardSteps {
    val adb = state ?: AdbSetupState.NeedsDevOptions
    val step1Done = wirelessOn
    val step2Done = when (adb) {
        AdbSetupState.Paired, AdbSetupState.Connected, AdbSetupState.Pairing -> true
        AdbSetupState.WaitingForPairingCode -> pairingPort != null
        is AdbSetupState.Failed -> pairingPort != null
        else -> false
    }
    val step3Done = adb == AdbSetupState.Paired || adb == AdbSetupState.Connected

    val failure = adb as? AdbSetupState.Failed
    val failedStep = when (failure?.reason) {
        null -> null
        AdbFailure.UNSUPPORTED -> BuiltinAdbWizardStep.WLAN_DEBUG
        else -> when {
            !step1Done -> BuiltinAdbWizardStep.WLAN_DEBUG
            !step2Done -> BuiltinAdbWizardStep.PAIRING_DIALOG
            else -> BuiltinAdbWizardStep.ENTER_CODE
        }
    }

    val activeStep = when {
        step3Done -> null
        failure != null -> failedStep
        !step1Done -> BuiltinAdbWizardStep.WLAN_DEBUG
        !step2Done -> BuiltinAdbWizardStep.PAIRING_DIALOG
        !step3Done -> BuiltinAdbWizardStep.ENTER_CODE
        else -> null
    }

    return BuiltinAdbWizardSteps(
        step1Done = step1Done,
        step2Done = step2Done,
        step3Done = step3Done,
        activeStep = activeStep,
        failedStep = failedStep,
        failure = failure?.reason,
    )
}
