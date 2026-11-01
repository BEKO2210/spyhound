package de.belkis.spyhound.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.scan.Progress
import de.belkis.spyhound.ui.SpyColors
import de.belkis.spyhound.ui.SpyTypographyExtras
import de.belkis.spyhound.ui.components.RadarCanvas
import de.belkis.spyhound.ui.components.ScanStepRow
import de.belkis.spyhound.ui.components.SpyButton
import de.belkis.spyhound.ui.components.ThinProgressLine

private val SCAN_STEPS = listOf("Apps", "Gerät", "SMS", "Dateien", "Tiefenprüfung")

private fun stepIndex(step: String): Int = when (step) {
    "Start" -> -1
    "Apps" -> 0
    "Gerät" -> 1
    "SMS" -> 2
    "Dateien", "Prozesse", "Pakete", "Systemprotokolle", "Fingerabdrücke" -> 3
    "Fertig" -> SCAN_STEPS.size
    else -> if (step.contains("Tiefen") || step == "Tiefenprüfung") 4 else 3
}

@Composable
fun ScanScreen(
    progress: Progress,
    onCancel: () -> Unit,
) {
    val current = stepIndex(progress.step)
    val blipSeed = remember { progress.detail.hashCode() }
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Witterung", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Der Spürhund folgt Spuren — offline, ohne Datenabfluss.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        RadarCanvas(
            fraction = progress.fraction,
            modifier = Modifier.size(280.dp),
            blipCount = (progress.fraction * 32).toInt().coerceIn(0, 32),
            blipSeed = blipSeed,
        )
        Spacer(Modifier.height(16.dp))
        val pct = (progress.fraction.coerceIn(0f, 1f) * 100).toInt()
        Text(
            "$pct %",
            style = SpyTypographyExtras.tabularHeadline.copy(fontSize = MaterialTheme.typography.headlineLarge.fontSize),
            color = SpyColors.Amber,
        )
        Spacer(Modifier.height(8.dp))
        ThinProgressLine(progress.fraction, Modifier.padding(horizontal = 4.dp))
        Spacer(Modifier.height(16.dp))
        Text(progress.step, style = MaterialTheme.typography.titleMedium, color = SpyColors.Amber)
        Text(progress.detail, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth()) {
            SCAN_STEPS.forEachIndexed { index, label ->
                val done = progress.fraction >= 1f || current > index
                val active = when (index) {
                    4 -> progress.fraction in 0.72f..0.99f
                    else -> current == index && progress.fraction < 1f
                }
                ScanStepRow(label = label, done = done, active = active)
            }
        }
        Spacer(Modifier.weight(1f))
        SpyButton(text = "Abbrechen", onClick = onCancel, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
    }
}
