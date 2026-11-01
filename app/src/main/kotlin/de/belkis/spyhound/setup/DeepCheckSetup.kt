package de.belkis.spyhound.setup

import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.shell.ShizukuShell

data class DeepCheckSetupSnapshot(
    val shizukuInstalled: Boolean,
    val devOptionsEnabled: Boolean,
    val shizukuStatus: ShizukuShell.Status,
    val access: AccessLevel,
) {
    val step1Done: Boolean get() = shizukuInstalled
    val step2Done: Boolean get() = devOptionsEnabled
    val step3Done: Boolean get() = shizukuStatus != ShizukuShell.Status.NOT_RUNNING
    val step4Done: Boolean get() = access >= AccessLevel.SHELL
    val allDone: Boolean get() = step1Done && step2Done && step3Done && step4Done
}

enum class DeepCheckSetupStep(val title: String) {
    INSTALL_SHIZUKU("Shizuku installieren"),
    DEV_OPTIONS("Entwickleroptionen einschalten"),
    START_SHIZUKU("Shizuku starten"),
    ALLOW_SPYHOUND("Spyhound erlauben"),
}

fun activeDeepCheckStep(snapshot: DeepCheckSetupSnapshot): DeepCheckSetupStep? = when {
    snapshot.allDone -> null
    !snapshot.step1Done -> DeepCheckSetupStep.INSTALL_SHIZUKU
    !snapshot.step2Done -> DeepCheckSetupStep.DEV_OPTIONS
    !snapshot.step3Done -> DeepCheckSetupStep.START_SHIZUKU
    !snapshot.step4Done -> DeepCheckSetupStep.ALLOW_SPYHOUND
    else -> null
}

fun stepDone(step: DeepCheckSetupStep, snapshot: DeepCheckSetupSnapshot): Boolean = when (step) {
    DeepCheckSetupStep.INSTALL_SHIZUKU -> snapshot.step1Done
    DeepCheckSetupStep.DEV_OPTIONS -> snapshot.step2Done
    DeepCheckSetupStep.START_SHIZUKU -> snapshot.step3Done
    DeepCheckSetupStep.ALLOW_SPYHOUND -> snapshot.step4Done
}

/** Debug-only snapshots for emulator screenshots (`adb … --es deep_check_screenshot step2`). */
fun debugDeepCheckSnapshot(stepKey: String): DeepCheckSetupSnapshot? = when (stepKey) {
    "step1" -> DeepCheckSetupSnapshot(false, false, ShizukuShell.Status.NOT_RUNNING, AccessLevel.APP)
    "step2" -> DeepCheckSetupSnapshot(true, false, ShizukuShell.Status.NOT_RUNNING, AccessLevel.APP)
    "step3" -> DeepCheckSetupSnapshot(true, true, ShizukuShell.Status.NOT_RUNNING, AccessLevel.APP)
    "step4" -> DeepCheckSetupSnapshot(true, true, ShizukuShell.Status.NEEDS_PERMISSION, AccessLevel.APP)
    "done" -> DeepCheckSetupSnapshot(true, true, ShizukuShell.Status.READY, AccessLevel.SHELL)
    else -> null
}

const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
