package de.belkis.spyhound.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.belkis.spyhound.R
import de.belkis.spyhound.background.BackgroundReportStore
import de.belkis.spyhound.background.ScanHistoryEntry
import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.scan.GapFix
import de.belkis.spyhound.scan.Report
import de.belkis.spyhound.scan.TextsDe
import de.belkis.spyhound.scan.TextsEn
import de.belkis.spyhound.scan.localized
import de.belkis.spyhound.ui.SpyColors
import de.belkis.spyhound.ui.SpyTypographyExtras
import de.belkis.spyhound.ui.components.CardRowIconBadge
import de.belkis.spyhound.ui.components.ScrollEdge
import de.belkis.spyhound.ui.components.SpyButton
import de.belkis.spyhound.ui.components.SpyCard
import de.belkis.spyhound.ui.components.SpyNavigateBack
import de.belkis.spyhound.ui.components.SpySurface
import de.belkis.spyhound.ui.components.TextActionLink
import de.belkis.spyhound.ui.design.SpyRadii
import de.belkis.spyhound.ui.design.SpySpacing
import de.belkis.spyhound.ui.formatCount
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatHistoryTimestamp(
    timestampMs: Long,
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    val pattern = if (locale.language == Locale.GERMAN.language) {
        "dd.MM.yyyy, HH:mm"
    } else {
        "dd MMM yyyy, HH:mm"
    }
    return DateTimeFormatter.ofPattern(pattern, locale)
        .withZone(zoneId)
        .format(Instant.ofEpochMilli(timestampMs))
}

fun historyItemContentDescription(
    context: Context,
    entry: ScanHistoryEntry,
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    val dateText = formatHistoryTimestamp(entry.timestamp, locale, zoneId)
    val depthLabel = context.getString(entry.access.shortLabelRes())
    val scopeLabel = if (entry.background) {
        "$depthLabel, ${context.getString(R.string.history_background_badge)}"
    } else {
        depthLabel
    }
    val findings = "${formatCount(entry.findingCount, locale)} ${context.getString(R.string.result_findings)}"
    val leads = "${formatCount(entry.leadCount, locale)} ${context.getString(R.string.result_leads)}"
    val gaps = "${formatCount(entry.gapCount, locale)} ${context.getString(R.string.result_gaps)}"
    return "$dateText, $scopeLabel: $findings, $leads, $gaps"
}

private fun AccessLevel.shortLabelRes(): Int = when (this) {
    AccessLevel.APP -> R.string.depth_basic
    AccessLevel.SHELL -> R.string.depth_deep
    AccessLevel.ROOT -> R.string.depth_root
}

@Composable
fun HistoryRoute(
    onBack: () -> Unit,
    onShareReport: (Report) -> Unit,
    onRescan: () -> Unit,
    onGapFix: (GapFix) -> Unit,
    onBugreport: () -> Unit,
    onHelp: () -> Unit,
) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(BackgroundReportStore.history(context)) }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var lastDeleted by remember { mutableStateOf<ScanHistoryEntry?>(null) }

    val selectedEntry = entries.firstOrNull { it.id == selectedId }
    if (selectedEntry != null) {
        BackHandler { selectedId = null }
        HistoryResultScreen(
            entry = selectedEntry,
            onBack = { selectedId = null },
            onShare = { onShareReport(selectedEntry.report) },
            onRescan = onRescan,
            onGapFix = onGapFix,
            onBugreport = onBugreport,
            onHelp = onHelp,
        )
    } else {
        BackHandler(onBack = onBack)
        HistoryScreen(
            entries = entries,
            lastDeleted = lastDeleted,
            onBack = onBack,
            onOpenEntry = { entry -> selectedId = entry.id },
            onDeleteEntry = { entry ->
                val removed = BackgroundReportStore.delete(context, entry.id)
                if (removed != null) {
                    lastDeleted = removed
                    entries = BackgroundReportStore.history(context)
                }
            },
            onUndoDelete = {
                val restore = lastDeleted ?: return@HistoryScreen
                BackgroundReportStore.restore(context, restore)
                lastDeleted = null
                entries = BackgroundReportStore.history(context)
            },
            onClearAll = {
                BackgroundReportStore.clearHistory(context)
                lastDeleted = null
                entries = emptyList()
            },
        )
    }
}

@Composable
fun HistoryResultScreen(
    entry: ScanHistoryEntry,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onRescan: () -> Unit,
    onGapFix: (GapFix) -> Unit,
    onBugreport: () -> Unit,
    onHelp: () -> Unit,
    zoneId: ZoneId = ZoneId.systemDefault(),
) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val texts = if (locale.language == Locale.GERMAN.language) TextsDe else TextsEn
    val omitted = stringResource(R.string.history_details_omitted)
    val localizedReport = remember(entry.report, texts, omitted) {
        entry.report.localized(texts).also { report ->
            report.checks.forEach { check ->
                check.findings.replaceAll { it.copy(detail = it.detail.ifBlank { omitted }) }
                check.leads.replaceAll { it.copy(where = it.where.ifBlank { omitted }) }
                check.gaps.replaceAll { it.copy(detail = it.detail.ifBlank { omitted }) }
            }
        }
    }
    val formattedDate = remember(entry.timestamp, locale, zoneId) {
        formatHistoryTimestamp(entry.timestamp, locale, zoneId)
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalHistoricalResult provides true) {
        ResultScreen(
            report = localizedReport,
            onShare = onShare,
            onRescan = onRescan,
            onGapFix = onGapFix,
            onBugreport = onBugreport,
            onHelp = onHelp,
            header = {
                HistoryResultHeader(
                    formattedDate = formattedDate,
                    background = entry.background,
                    onBack = onBack,
                )
            },
        )
    }
}

@Composable
fun HistoryResultHeader(
    formattedDate: String,
    background: Boolean,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SpyNavigateBack(onClick = onBack)
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.history_result_header, formattedDate),
                style = MaterialTheme.typography.titleMedium,
                color = SpyColors.Ivory,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (background) {
                HistoryBadge(
                    text = stringResource(R.string.history_background_badge),
                    color = SpyColors.Amber,
                    borderColor = SpyColors.Amber,
                )
            }
        }
        HorizontalDivider(color = SpyColors.Border, thickness = 1.dp)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun HistoryScreen(
    entries: List<ScanHistoryEntry>,
    onBack: () -> Unit,
    onOpenEntry: (ScanHistoryEntry) -> Unit,
    onDeleteEntry: (ScanHistoryEntry) -> Unit,
    onClearAll: () -> Unit,
    lastDeleted: ScanHistoryEntry? = null,
    onUndoDelete: () -> Unit = {},
    zoneId: ZoneId = ZoneId.systemDefault(),
) {
    var showClearConfirm by rememberSaveable { mutableStateOf(false) }
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()
    val scrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    entries.firstOrNull { it.id == deleteId }?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            containerColor = SpyColors.SurfaceRaised,
            title = { Text(stringResource(R.string.history_delete_confirm_title)) },
            text = { Text(stringResource(R.string.history_delete_entry_cd,
                formatHistoryTimestamp(entry.timestamp, LocalConfiguration.current.locales[0], zoneId))) },
            dismissButton = { TextActionLink(stringResource(R.string.scan_cancel), { deleteId = null }) },
            confirmButton = {
                TextActionLink(stringResource(R.string.history_delete_confirm_action), {
                    deleteId = null
                    onDeleteEntry(entry)
                })
            },
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = SpyColors.SurfaceRaised,
            shape = RoundedCornerShape(SpyRadii.Card),
            tonalElevation = 0.dp,
            titleContentColor = SpyColors.Ivory,
            textContentColor = SpyColors.IvoryMuted,
            title = {
                Text(
                    stringResource(R.string.history_clear_confirm_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Text(
                    stringResource(R.string.history_clear_confirm_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            dismissButton = {
                TextActionLink(
                    text = stringResource(R.string.scan_cancel),
                    onClick = { showClearConfirm = false },
                )
            },
            confirmButton = {
                SpyButton(
                    text = stringResource(R.string.history_clear_confirm_action),
                    onClick = {
                        showClearConfirm = false
                        onClearAll()
                    },
                    accent = true,
                    fillWidth = false,
                )
            },
        )
    }

    Column(Modifier.fillMaxSize()) {
        SpyNavigateBack(onClick = onBack)
        Row(
            Modifier.fillMaxWidth().padding(bottom = SpySpacing.Unit * 2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.history_title),
                style = MaterialTheme.typography.headlineLarge,
                color = SpyColors.Ivory,
                modifier = Modifier.weight(1f),
            )
            if (entries.isNotEmpty()) {
                TextActionLink(
                    text = stringResource(R.string.history_clear_all),
                    onClick = { showClearConfirm = true },
                )
            }
        }
        ScrollEdge(scrolled)
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.history_privacy_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpyColors.IvoryMuted,
                )
            }
            if (entries.isEmpty()) {
                item {
                    HistoryEmptyCard()
                }
            } else {
                items(entries, key = { it.id }) { entry ->
                    HistoryEntryItem(
                        entry = entry,
                        zoneId = zoneId,
                        onClick = { onOpenEntry(entry) },
                        onDelete = { deleteId = entry.id },
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
        if (lastDeleted != null) {
            Spacer(Modifier.height(8.dp))
            SpySurface(raised = true, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.history_entry_deleted),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SpyColors.Ivory,
                        modifier = Modifier.weight(1f),
                    )
                    TextActionLink(
                        text = stringResource(R.string.history_undo),
                        onClick = onUndoDelete,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryEmptyCard() {
    SpyCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CardRowIconBadge(iconRes = R.drawable.ic_history, active = false)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.history_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = SpyColors.Ivory,
                )
                Spacer(Modifier.height(SpySpacing.Unit))
                Text(
                    text = stringResource(R.string.history_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpyColors.IvoryMuted,
                )
            }
        }
    }
}

@Composable
private fun HistoryEntryItem(
    entry: ScanHistoryEntry,
    zoneId: ZoneId,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val dateText = remember(entry.timestamp, locale, zoneId) {
        formatHistoryTimestamp(entry.timestamp, locale, zoneId)
    }
    val itemA11yDescription = remember(entry, locale, zoneId) {
        historyItemContentDescription(context, entry, locale, zoneId)
    }
    val deleteCd = stringResource(R.string.history_delete_entry_cd, dateText)

    SpyCard(
        modifier = Modifier
            .semantics { contentDescription = itemA11yDescription }
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.titleMedium,
                    color = SpyColors.Ivory,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HistoryBadge(
                        text = stringResource(entry.access.shortLabelRes()),
                        color = SpyColors.IvoryMuted,
                        borderColor = SpyColors.Border,
                    )
                    if (entry.background) {
                        HistoryBadge(
                            text = stringResource(R.string.history_background_badge),
                            color = SpyColors.Amber,
                            borderColor = SpyColors.Amber,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SpyColors.SurfaceRaised)
                    .border(1.dp, SpyColors.Border, CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = deleteCd,
                        onClick = onDelete,
                    )
                    .semantics {
                        contentDescription = deleteCd
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                    tint = SpyColors.IvoryMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HistoryCountStat(
                count = entry.findingCount,
                label = stringResource(R.string.result_findings),
                color = SpyColors.Danger,
                modifier = Modifier.weight(1f),
            )
            HistoryCountStat(
                count = entry.leadCount,
                label = stringResource(R.string.result_leads),
                color = SpyColors.Amber,
                modifier = Modifier.weight(1f),
            )
            HistoryCountStat(
                count = entry.gapCount,
                label = stringResource(R.string.result_gaps),
                color = SpyColors.IvoryMuted,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HistoryCountStat(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    SpySurface(modifier = modifier, raised = true) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = formatCount(count, LocalConfiguration.current.locales[0]),
                style = SpyTypographyExtras.statusValue,
                color = color,
            )
            Text(
                text = label,
                style = SpyTypographyExtras.statusName,
                color = SpyColors.IvoryMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HistoryBadge(
    text: String,
    color: Color,
    borderColor: Color,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
fun HistoryEntryCard(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val title = stringResource(R.string.history_entry_title)
    SpyCard(
        modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpen,
            )
            .semantics { contentDescription = title },
    ) {
        Row(verticalAlignment = Alignment.Top) {
            CardRowIconBadge(iconRes = R.drawable.ic_history, active = false)
            Column(Modifier.weight(1f).padding(start = 14.dp, end = 8.dp, top = 8.dp)) {
                Text(title.uppercase(), style = SpyTypographyExtras.sectionLabel)
                Spacer(Modifier.height(SpySpacing.Unit))
                Text(
                    stringResource(R.string.history_entry_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpyColors.IvoryMuted,
                )
            }
            Text(
                stringResource(R.string.history_entry_open),
                style = MaterialTheme.typography.labelLarge,
                color = SpyColors.Amber,
                maxLines = 1,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
