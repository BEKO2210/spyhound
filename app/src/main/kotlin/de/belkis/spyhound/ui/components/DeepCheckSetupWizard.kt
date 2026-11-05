package de.belkis.spyhound.ui.components

import de.belkis.spyhound.R
import androidx.compose.ui.res.stringResource

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.setup.BuiltinAdbWizardStep
import de.belkis.spyhound.setup.DeepCheckSetupSnapshot
import de.belkis.spyhound.setup.DeepCheckSetupStep
import de.belkis.spyhound.setup.activeDeepCheckStep
import de.belkis.spyhound.setup.deriveBuiltinAdbWizardSteps
import de.belkis.spyhound.setup.openDeveloperSettings
import de.belkis.spyhound.setup.openDeviceInfoSettings
import de.belkis.spyhound.setup.openShizukuApp
import de.belkis.spyhound.setup.openShizukuDownload
import de.belkis.spyhound.setup.stepDone
import de.belkis.spyhound.shell.AdbFailure
import de.belkis.spyhound.shell.AdbSetupState
import de.belkis.spyhound.ui.SpyColors
import de.belkis.spyhound.ui.design.SpyMotion
import de.belkis.spyhound.ui.design.rememberReducedMotion

private fun adbFailureMessage(reason: AdbFailure): Int = when (reason) {
    AdbFailure.UNSUPPORTED -> R.string.adb_failed_unsupported
    AdbFailure.TOO_MANY_INVALID_CODES -> R.string.adb_failed_codes
    AdbFailure.PAIRING_REJECTED -> R.string.adb_failed_pairing
    AdbFailure.CONNECT_FAILED -> R.string.adb_failed_connect
    AdbFailure.TIMEOUT -> R.string.adb_failed_timeout
    AdbFailure.INTERNAL -> R.string.adb_failed_internal
    AdbFailure.NO_KEYSTORE -> R.string.adb_failed_no_keystore
}

@Composable
fun DeepCheckSetupWizard(
    snapshot: DeepCheckSetupSnapshot,
    onDismiss: () -> Unit,
    onAllowSpyhound: () -> Unit,
    onScanNow: () -> Unit,
    modifier: Modifier = Modifier,
    builtinAdb: AdbSetupState? = null,
    adbWirelessOn: Boolean = false,
    adbPairingPort: Int? = null,
    onSetupWithoutPc: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val active = activeDeepCheckStep(snapshot)
    BackHandler(onBack = onDismiss)
    if (onSetupWithoutPc != null) {
        LaunchedEffect(Unit) { onSetupWithoutPc() }
    }
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SpyNavigateBack(onClick = onDismiss)
        if (snapshot.allDone) {
            DonePanel(builtinAdb = snapshot.builtinAdbActive, onScanNow = onScanNow)
            return@Column
        }
        Text(
            stringResource(R.string.t_011),
            style = MaterialTheme.typography.headlineLarge,
            color = SpyColors.Ivory,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            emphasizeLabels(stringResource(R.string.deep_check_intro)),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(20.dp))

        if (onSetupWithoutPc != null) {
            BuiltinAdbWizardSection(
                wirelessOn = adbWirelessOn,
                adbState = builtinAdb,
                pairingPort = adbPairingPort,
                devOptionsEnabled = snapshot.devOptionsEnabled,
                onOpenDeveloperSettings = { openDeveloperSettings(context) },
                onOpenAboutPhone = { openDeviceInfoSettings(context) },
                onRetry = onSetupWithoutPc,
                onScanNow = onScanNow,
            )
            Spacer(Modifier.height(12.dp))
        }

        CollapsibleSection(
            title = stringResource(R.string.adb_wizard_shizuku_section),
            initiallyExpanded = false,
        ) {
            ShizukuSetupSteps(
                snapshot = snapshot,
                active = active,
                onAllowSpyhound = onAllowSpyhound,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            emphasizeLabels(stringResource(R.string.t_032)),
            style = MaterialTheme.typography.bodyMedium,
            color = SpyColors.IvoryMuted,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BuiltinAdbWizardSection(
    wirelessOn: Boolean,
    adbState: AdbSetupState?,
    pairingPort: Int?,
    devOptionsEnabled: Boolean,
    onOpenDeveloperSettings: () -> Unit,
    onOpenAboutPhone: () -> Unit,
    onRetry: () -> Unit,
    onScanNow: () -> Unit,
) {
    val steps = deriveBuiltinAdbWizardSteps(wirelessOn, adbState, pairingPort)
    var splitHintOpen by remember { mutableStateOf(false) }

    Text(
        stringResource(R.string.adb_wizard_intro),
        style = MaterialTheme.typography.bodyMedium,
        color = SpyColors.IvoryMuted,
    )
    Spacer(Modifier.height(16.dp))

    if (steps.setupComplete) {
        SpyCard {
            Text(
                stringResource(R.string.adb_wizard_success_title),
                style = MaterialTheme.typography.titleMedium,
                color = SpyColors.Clear,
            )
            Text(
                stringResource(R.string.adb_wizard_success_body),
                style = MaterialTheme.typography.bodyMedium,
                color = SpyColors.IvoryMuted,
            )
            SpyButton(
                text = stringResource(R.string.adb_wizard_scan_now),
                onClick = onScanNow,
                accent = true,
            )
        }
        Spacer(Modifier.height(12.dp))
    } else {
        AdbWizardStepCard(
            number = 1,
            title = stringResource(R.string.adb_wizard_step1_title),
            body = stringResource(R.string.adb_wizard_step1_body),
            done = steps.step1Done,
            active = steps.activeStep == BuiltinAdbWizardStep.WLAN_DEBUG,
            failed = steps.failedStep == BuiltinAdbWizardStep.WLAN_DEBUG,
            failureMessage = steps.failure?.let { stringResource(adbFailureMessage(it)) },
            onRetry = onRetry,
            actionLabel = stringResource(R.string.adb_wizard_step1_action),
            onAction = onOpenDeveloperSettings,
            extraContent = {
                if (!devOptionsEnabled && !steps.step1Done) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        emphasizeLabels(stringResource(R.string.adb_wizard_dev_hint)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SpyColors.IvoryMuted,
                    )
                    Spacer(Modifier.height(8.dp))
                    SpyButton(
                        text = stringResource(R.string.adb_wizard_dev_action),
                        onClick = onOpenAboutPhone,
                        fillWidth = false,
                    )
                }
            },
        )
        Spacer(Modifier.height(12.dp))
        AdbWizardStepCard(
            number = 2,
            title = stringResource(R.string.adb_wizard_step2_title),
            body = stringResource(R.string.adb_wizard_step2_body),
            done = steps.step2Done,
            active = steps.activeStep == BuiltinAdbWizardStep.PAIRING_DIALOG,
            failed = steps.failedStep == BuiltinAdbWizardStep.PAIRING_DIALOG,
            failureMessage = steps.failure?.let { stringResource(adbFailureMessage(it)) },
            onRetry = onRetry,
            actionLabel = stringResource(R.string.adb_wizard_step2_action),
            onAction = onOpenDeveloperSettings,
            enabled = steps.step1Done,
        )
        Spacer(Modifier.height(12.dp))
        AdbWizardStepCard(
            number = 3,
            title = stringResource(R.string.adb_wizard_step3_title),
            body = stringResource(R.string.adb_wizard_step3_body),
            done = steps.step3Done,
            active = steps.activeStep == BuiltinAdbWizardStep.ENTER_CODE,
            failed = steps.failedStep == BuiltinAdbWizardStep.ENTER_CODE,
            failureMessage = steps.failure?.let { stringResource(adbFailureMessage(it)) },
            onRetry = onRetry,
            actionLabel = null,
            onAction = null,
            enabled = steps.step2Done,
            extraContent = {
                Spacer(Modifier.height(12.dp))
                Icon(
                    painter = painterResource(R.drawable.illu_adb_notif_code),
                    contentDescription = null,
                    tint = SpyColors.Ivory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                )
            },
        )
    }

    TextActionLink(
        text = stringResource(R.string.adb_wizard_splitscreen),
        onClick = { splitHintOpen = !splitHintOpen },
        modifier = Modifier.padding(top = 12.dp),
    )
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(top = 4.dp),
    ) {
        if (splitHintOpen) {
            Text(
                emphasizeLabels(stringResource(R.string.adb_wizard_splitscreen_hint)),
                style = MaterialTheme.typography.bodyMedium,
                color = SpyColors.IvoryMuted,
            )
        }
    }
}

@Composable
private fun AdbWizardStepCard(
    number: Int,
    title: String,
    body: String,
    done: Boolean,
    active: Boolean,
    failed: Boolean,
    failureMessage: String?,
    onRetry: () -> Unit,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    enabled: Boolean = true,
    extraContent: @Composable (() -> Unit)? = null,
) {
    val reduced = rememberReducedMotion()
    val doneAlpha by animateFloatAsState(
        targetValue = if (done) 1f else 0f,
        animationSpec = if (reduced) tween(0) else tween(SpyMotion.Element, easing = SpyMotion.Enter),
        label = "step done",
    )
    val titleColor by animateColorAsState(
        targetValue = when {
            done -> SpyColors.IvoryMuted
            active -> SpyColors.Ivory
            else -> SpyColors.IvoryMuted.copy(alpha = 0.85f)
        },
        animationSpec = if (reduced) tween(0) else tween(SpyMotion.Element, easing = SpyMotion.Enter),
        label = "title color",
    )
    val doneCd = stringResource(R.string.adb_wizard_step_done_cd)

    SpyCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 168.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .graphicsLayer { alpha = if (done) 0.35f else 1f },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    number.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (done) SpyColors.Clear else SpyColors.Ivory,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor)
                Spacer(Modifier.height(4.dp))
                Text(emphasizeLabels(body), style = MaterialTheme.typography.bodyMedium, color = SpyColors.IvoryMuted)
                extraContent?.invoke()
                if (failed && failureMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    CardFeedbackLine(
                        message = failureMessage,
                        isError = true,
                        contentDescription = failureMessage,
                    )
                    TextActionLink(
                        text = stringResource(R.string.adb_wizard_retry),
                        onClick = onRetry,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            if (done) {
                Icon(
                    painter = painterResource(R.drawable.ic_wizard_step_done),
                    contentDescription = doneCd,
                    tint = SpyColors.Clear,
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer { alpha = doneAlpha },
                )
            }
        }
        if (!done && active && actionLabel != null && onAction != null) {
            SpyButton(
                text = actionLabel,
                onClick = onAction,
                enabled = enabled,
                fillWidth = false,
                singleLine = true,
                accent = true,
                modifier = Modifier.padding(start = 44.dp, top = 12.dp),
            )
        }
    }
}

@Composable
private fun ShizukuSetupSteps(
    snapshot: DeepCheckSetupSnapshot,
    active: DeepCheckSetupStep?,
    onAllowSpyhound: () -> Unit,
) {
    val context = LocalContext.current
    DeepCheckStepBlock(
        step = DeepCheckSetupStep.INSTALL_SHIZUKU,
        snapshot = snapshot,
        active = active,
        body = stringResource(R.string.t_012),
        help = stringResource(R.string.t_013) + stringResource(R.string.t_014),
    ) {
        SpyButton(text = stringResource(R.string.t_015), onClick = { openShizukuDownload(context) }, accent = true)
    }
    DeepCheckStepBlock(
        step = DeepCheckSetupStep.DEV_OPTIONS,
        snapshot = snapshot,
        active = active,
        body = stringResource(R.string.t_016) + stringResource(R.string.t_017),
        help = stringResource(R.string.t_018),
    ) {
        SpyButton(text = stringResource(R.string.t_019), onClick = { openDeviceInfoSettings(context) })
    }
    DeepCheckStepBlock(
        step = DeepCheckSetupStep.START_SHIZUKU,
        snapshot = snapshot,
        active = active,
        body = stringResource(R.string.t_020) +
            stringResource(R.string.t_021) +
            stringResource(R.string.t_022),
        help = stringResource(R.string.t_023) +
            stringResource(R.string.t_024) +
            stringResource(R.string.t_025) +
            stringResource(R.string.t_026),
    ) {
        SpyButton(text = stringResource(R.string.t_027), onClick = { openShizukuApp(context) }, accent = true)
        Spacer(Modifier.height(8.dp))
        SpyButton(text = stringResource(R.string.t_028), onClick = { openDeveloperSettings(context) })
    }
    DeepCheckStepBlock(
        step = DeepCheckSetupStep.ALLOW_SPYHOUND,
        snapshot = snapshot,
        active = active,
        body = stringResource(R.string.t_029),
        help = stringResource(R.string.t_030),
    ) {
        SpyButton(
            text = stringResource(R.string.t_031),
            onClick = onAllowSpyhound,
            accent = true,
            enabled = snapshot.step3Done,
        )
    }
}

@Composable
private fun DonePanel(builtinAdb: Boolean, onScanNow: () -> Unit) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = SpyColors.Clear,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.t_033),
            style = MaterialTheme.typography.headlineMedium,
            color = SpyColors.Ivory,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            // Spyhound's own connection needs wireless debugging on; only Shizuku keeps running without it.
            stringResource(if (builtinAdb) R.string.adb_wizard_success_body else R.string.t_034),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        SpyButton(text = stringResource(R.string.t_035), onClick = onScanNow, accent = true)
    }
}

@Composable
private fun DeepCheckStepBlock(
    step: DeepCheckSetupStep,
    snapshot: DeepCheckSetupSnapshot,
    active: DeepCheckSetupStep?,
    body: String,
    help: String,
    actions: @Composable () -> Unit,
) {
    val done = stepDone(step, snapshot)
    val isActive = active == step
    var helpOpen by remember(step) { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        ScanStepRow(label = stringResource(step.title), done = done, active = isActive)
        if (isActive || done) {
            Text(
                emphasizeLabels(body),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActive) SpyColors.Ivory else SpyColors.IvoryMuted,
                modifier = Modifier.padding(start = 34.dp, top = 4.dp),
            )
            if (isActive) {
                Column(Modifier.padding(start = 34.dp, top = 12.dp, end = 0.dp)) {
                    actions()
                }
            }
        }
        TextActionLink(
            text = if (helpOpen) stringResource(R.string.t_036) else stringResource(R.string.t_037),
            onClick = { helpOpen = !helpOpen },
            modifier = Modifier.padding(start = 34.dp, top = 6.dp),
        )
        if (helpOpen) {
            Text(
                emphasizeLabels(help),
                style = MaterialTheme.typography.bodyMedium,
                color = SpyColors.IvoryMuted,
                modifier = Modifier.padding(start = 34.dp, top = 4.dp, bottom = 4.dp),
            )
        }
    }
}
