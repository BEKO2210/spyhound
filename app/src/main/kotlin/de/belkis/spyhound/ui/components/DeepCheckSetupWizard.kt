package de.belkis.spyhound.ui.components

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
            "Tiefere Prüfung einrichten",
            style = MaterialTheme.typography.headlineMedium,
            color = SpyColors.Ivory,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Damit Spyhound mehr finden kann — ohne Root, nur auf dem Telefon. " +
                "WLAN muss an sein. Nach einem Neustart musst du Shizuku erneut starten.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(20.dp))
        DeepCheckStepBlock(
            step = DeepCheckSetupStep.INSTALL_SHIZUKU,
            snapshot = snapshot,
            active = active,
            body = "Installiere die App Shizuku. Sie hilft Spyhound, tiefer zu schauen — wie ein Werkzeug von Android selbst.",
            help = "Die Installation klappt nicht: Lade Shizuku von shizuku.rikka.app. Bei Samsung kann „Auto Blocker“ " +
                "Installationen blockieren — unter Sicherheit und Datenschutz kurz ausschalten.",
        ) {
            SpyButton(text = "Shizuku installieren", onClick = { openShizukuDownload(context) }, accent = true)
        }
        DeepCheckStepBlock(
            step = DeepCheckSetupStep.DEV_OPTIONS,
            snapshot = snapshot,
            active = active,
            body = "Öffne Einstellungen → Über das Telefon und tippe siebenmal auf „Build-Nummer“. " +
                "Samsung: Über das Telefon → Softwareinformationen → Buildnummer.",
            help = "Es passiert nichts beim Tippen: Tippe schneller hintereinander. Manchmal fragt Android nach deiner PIN.",
        ) {
            SpyButton(text = "Telefoninfo öffnen", onClick = { openDeviceInfoSettings(context) })
        }
        DeepCheckStepBlock(
            step = DeepCheckSetupStep.START_SHIZUKU,
            snapshot = snapshot,
            active = active,
            body = "In Shizuku auf „Kopplung“ tippen. Erlaube die Benachrichtigung von Shizuku. " +
                "In den Entwickleroptionen: „Debugging über WLAN“ einschalten, dann „Gerät über Kopplungscode koppeln“. " +
                "Den Code in die Shizuku-Benachrichtigung eintippen. Danach in Shizuku auf „Starten“.",
            help = "Kopplungscode wird nicht angenommen: Benachrichtigung von Shizuku muss erlaubt sein. " +
                "Nutze den geteilten Bildschirm — Einstellungen und Benachrichtigung gleichzeitig sehen. " +
                "Samsung: Auto Blocker aus. Bleibt „Suche nach Kopplungsdienst“ stehen: Shizuku darf im Hintergrund laufen " +
                "(Akku: uneingeschränkt).",
        ) {
            SpyButton(text = "Shizuku öffnen", onClick = { openShizukuApp(context) }, accent = true)
            Spacer(Modifier.height(8.dp))
            SpyButton(text = "Entwickleroptionen öffnen", onClick = { openDeveloperSettings(context) })
        }
        DeepCheckStepBlock(
            step = DeepCheckSetupStep.ALLOW_SPYHOUND,
            snapshot = snapshot,
            active = active,
            body = "Spyhound braucht deine Erlaubnis, Shizuku zu nutzen. Tippe auf Erlauben, wenn Android fragt.",
            help = "Kein Dialog: Shizuku muss laufen (Schritt 3). App neu öffnen und nochmal tippen.",
        ) {
            SpyButton(
                text = "Spyhound erlauben",
                onClick = onAllowSpyhound,
                accent = true,
                enabled = snapshot.step3Done,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Root? Dann einfach ‚Root anfragen‘ auf dem Startbildschirm tippen.",
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
            "Tiefere Prüfung ist bereit",
            style = MaterialTheme.typography.headlineMedium,
            color = SpyColors.Ivory,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "WLAN-Debugging kannst du danach wieder ausschalten; Shizuku läuft weiter bis zum nächsten Neustart.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        SpyButton(text = "Jetzt prüfen", onClick = onScanNow, accent = true)
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
            text = if (helpOpen) "Hilfe ▲" else "Hilfe",
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
