package de.belkis.spyhound.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.ui.SpyColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShizukuGuideSheet(visible: Boolean, onDismiss: () -> Unit) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SpyColors.Surface,
        contentColor = SpyColors.Ivory,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("Shizuku einrichten", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Android 11+, WLAN, ca. 10 Minuten. Nach jedem Neustart Shizuku erneut starten (Schritt 4).",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            guideStep(
                "1",
                "Entwickleroptionen: Siebenmal auf die Build-Nummer tippen, USB-Debugging einschalten.",
            )
            guideStep(
                "2",
                "Shizuku installieren (shizuku.rikka.app), öffnen, Hintergrundbetrieb uneingeschränkt erlauben.",
            )
            guideStep(
                "3",
                "WLAN-Debugging in den Entwickleroptionen einschalten. In Shizuku: Über WLAN-Debugging starten → Koppeln. " +
                    "Kopplungscode in den Entwickleroptionen anzeigen und in der Shizuku-Benachrichtigung eingeben.",
            )
            guideStep("4", "In Shizuku auf Starten tippen — „Shizuku läuft“ muss erscheinen.")
            guideStep("5", "In Spyhound Shizuku verbinden und die Freigabe bestätigen.")
            guideStep(
                "6",
                "WLAN-Debugging danach wieder ausschalten (Shizuku läuft weiter). Nach Neustart Schritt 4 wiederholen.",
            )
            Spacer(Modifier.height(16.dp))
            Text("Samsung (One UI)", style = MaterialTheme.typography.titleMedium, color = SpyColors.Amber)
            Spacer(Modifier.height(6.dp))
            Text(
                "Build-Nummer unter Telefoninfo → Softwareinformationen. Auto Blocker kann Debugging blockieren — " +
                    "vorübergehend unter Sicherheit und Datenschutz ausschalten. Shizuku: Akku uneingeschränkt, " +
                    "nicht in Schlafende Apps.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            Text("Pixel", style = MaterialTheme.typography.titleMedium, color = SpyColors.Amber)
            Spacer(Modifier.height(6.dp))
            Text(
                "Entwickleroptionen unter System. Kopplungsdialog offen lassen, bis der Code in Shizuku eingegeben ist. " +
                    "Shizuku: Akkunutzung uneingeschränkt.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun guideStep(number: String, body: String) {
    Spacer(Modifier.height(10.dp))
    Text("$number. $body", style = MaterialTheme.typography.bodyLarge)
}
