package de.belkis.spyhound.ui

import de.belkis.spyhound.setup.DeepCheckSetupSnapshot

fun UiState.deepCheckSnapshot(): DeepCheckSetupSnapshot =
    DeepCheckSetupSnapshot(
        shizukuInstalled = shizukuInstalled,
        devOptionsEnabled = devOptionsEnabled,
        shizukuStatus = shizuku,
        access = access,
    )
