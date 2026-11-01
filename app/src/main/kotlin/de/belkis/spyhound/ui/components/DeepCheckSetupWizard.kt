package de.belkis.spyhound.ui.components

import de.belkis.spyhound.R
import androidx.compose.ui.res.stringResource

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.setup.DeepCheckSetupSnapshot
import de.belkis.spyhound.setup.DeepCheckSetupStep
import de.belkis.spyhound.setup.activeDeepCheckStep
import de.belkis.spyhound.setup.openDeveloperSettings
import de.belkis.spyhound.setup.openDeviceInfoSettings
import de.belkis.spyhound.setup.openShizukuApp
import de.belkis.spyhound.setup.openShizukuDownload
import de.belkis.spyhound.setup.stepDone
import de.belkis.spyhound.ui.SpyColors

@Composable
fun DeepCheckSetupWizard(
    snapshot: DeepCheckSetupSnapshot,
    onDismiss: () -> Unit,
    onAllowSpyhound: () -> Unit,
    onScanNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val active = activeDeepCheckStep(snapshot)
    BackHandler(onBack = onDismiss)
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SpyNavigateBack(onClick = onDismiss)
        if (snapshot.allDone) {
            DonePanel(onScanNow = onScanNow)
            return@Column
        }
        Text(
            stringResource(R.string.t_011),
            style = MaterialTheme.typography.headlineMedium,
            color = SpyColors.Ivory,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.deep_check_intro),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(20.dp))
        DeepCheckStepBlock(
            step = DeepCheckSetupStep.INSTALL_SHIZUKU,
            snapshot = snapshot,
            active = active,
            body = stringResource(R.string.t_012),
            help = stringResource(R.string.t_013) +
                stringResource(R.string.t_014),
        ) {
            SpyButton(text = stringResource(R.string.t_015), onClick = { openShizukuDownload(context) }, accent = true)
        }
        DeepCheckStepBlock(
            step = DeepCheckSetupStep.DEV_OPTIONS,
            snapshot = snapshot,
            active = active,
            body = stringResource(R.string.t_016) +
                stringResource(R.string.t_017),
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
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.t_032),
            style = MaterialTheme.typography.bodyMedium,
            color = SpyColors.IvoryMuted,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DonePanel(onScanNow: () -> Unit) {
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
            stringResource(R.string.t_034),
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
        ScanStepRow(label = step.title, done = done, active = isActive)
        if (isActive || done) {
            Text(
                body,
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
                help,
                style = MaterialTheme.typography.bodyMedium,
                color = SpyColors.IvoryMuted,
                modifier = Modifier.padding(start = 34.dp, top = 4.dp, bottom = 4.dp),
            )
        }
    }
}
