package de.belkis.spyhound.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.belkis.spyhound.R
import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.scan.Finding
import de.belkis.spyhound.scan.Gap
import de.belkis.spyhound.scan.MessageAnalyzer
import de.belkis.spyhound.scan.ShellAnalyzer
import de.belkis.spyhound.scan.GapFix
import de.belkis.spyhound.scan.Guidance
import de.belkis.spyhound.scan.GuidanceAction
import de.belkis.spyhound.scan.Lead
import de.belkis.spyhound.scan.Report
import de.belkis.spyhound.scan.TimeRange
import de.belkis.spyhound.scan.Verdict
import de.belkis.spyhound.scan.guidanceFor
import de.belkis.spyhound.ui.SpyColors
import de.belkis.spyhound.ui.SpyTypographyExtras
import de.belkis.spyhound.ui.components.emphasizeLabels
import de.belkis.spyhound.ui.components.ScrollEdge
import de.belkis.spyhound.ui.components.SectionHeader
import de.belkis.spyhound.ui.components.SpyButton
import de.belkis.spyhound.ui.components.SpySurface
import de.belkis.spyhound.ui.components.StatusItem
import de.belkis.spyhound.ui.components.StatusStrip
import de.belkis.spyhound.ui.components.StatusKind
import de.belkis.spyhound.ui.components.StatusState
import de.belkis.spyhound.ui.design.SpyHaptics
import de.belkis.spyhound.ui.design.SpyMotion
import de.belkis.spyhound.ui.design.SpySpacing
import de.belkis.spyhound.ui.design.SpyTypeScale
import de.belkis.spyhound.ui.design.countUp
import de.belkis.spyhound.ui.formatCount
import de.belkis.spyhound.ui.design.rememberReducedMotion
import de.belkis.spyhound.ui.design.staggeredAppear

@Composable
fun ResultScreen(
    report: Report,
    onShare: () -> Unit,
    onRescan: () -> Unit,
    onGapFix: (GapFix) -> Unit,
    onBugreport: () -> Unit,
    onHelp: () -> Unit,
    heroSymbolModifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {
    val readOnly = LocalHistoricalResult.current
    val tablet = LocalConfiguration.current.screenWidthDp >= 600
    val listState = rememberLazyListState()
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 } }
    val examined = report.checks.sumOf { it.examined }
    val context = LocalContext.current
    val view = LocalView.current
    val haptics = remember(context) { SpyHaptics(context) }
    LaunchedEffect(report.startedAt, report.finishedAt) {
        if (!readOnly && report.findings.isNotEmpty()) haptics.alert(view)
    }
    Column(Modifier.fillMaxSize()) {
        header?.invoke()
        if (tablet) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(SpySpacing.Section)) {
                ResultSummary(report, examined, Modifier.weight(1f), onGapFix, heroSymbolModifier)
                Column(Modifier.weight(1f)) {
                    ScrollEdge(scrolled)
                    ResultList(report, listState, onGapFix, onHelp, Modifier.weight(1f))
                }
            }
        } else {
            val reduced = rememberReducedMotion()
            ResultList(report, listState, onGapFix, onHelp, Modifier.weight(1f)) {
                ResultSummary(
                    report = report,
                    examined = examined,
                    modifier = Modifier.fillMaxWidth().padding(bottom = SpySpacing.Section - 8.dp),
                    onGapFix = onGapFix,
                    heroSymbolModifier = heroSymbolModifier,
                    heroModifier = Modifier.graphicsLayer {
                        val progress = if (reduced) 0f else if (listState.firstVisibleItemIndex > 0) 1f
                        else (listState.firstVisibleItemScrollOffset / 148.dp.toPx()).coerceIn(0f, 1f)
                        alpha = 1f - 0.35f * progress
                        scaleX = 1f - 0.06f * progress
                        scaleY = scaleX
                    },
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SpyButton(stringResource(R.string.t_052), onShare, Modifier.weight(1f))
            SpyButton(stringResource(R.string.t_051), onRescan, Modifier.weight(1f), accent = true)
        }
    }
}

internal val LocalHistoricalResult = androidx.compose.runtime.staticCompositionLocalOf { false }

@Composable
private fun ResultSummary(
    report: Report,
    examined: Int,
    modifier: Modifier,
    onGapFix: (GapFix) -> Unit,
    heroSymbolModifier: Modifier = Modifier,
    heroModifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SpySpacing.Section)) {
        ResultHero(report = report, symbolModifier = heroSymbolModifier, modifier = heroModifier)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            CountCell(report.findings.size, pluralStringResource(R.plurals.count_findings, report.findings.size), SpyColors.Danger)
            CountCell(report.leads.size, pluralStringResource(R.plurals.count_leads, report.leads.size), SpyColors.Amber)
            CountCell(report.gaps.size, pluralStringResource(R.plurals.count_gaps, report.gaps.size), SpyColors.IvoryMuted)
            CountCell(examined, stringResource(R.string.result_checked), SpyColors.Ivory)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.result_checked_with).uppercase(), style = SpyTypographyExtras.sectionLabel)
        val depth = when (report.access) {
            AccessLevel.APP -> stringResource(R.string.depth_basic)
            AccessLevel.SHELL -> stringResource(R.string.depth_deep)
            AccessLevel.ROOT -> stringResource(R.string.depth_root)
        }
        val sms = report.checks.any { it.id == MessageAnalyzer.NAME && it.ran }
        val files = report.checks.any { it.id == ShellAnalyzer.FILES && it.ran }
        val sources = report.indicatorSources
        val total = sources + report.indicatorSourcesMissing
        // The counters above already show how many checks ran; the strip says what they could use.
        StatusStrip(listOf(
            StatusItem(
                kind = StatusKind.DEPTH,
                label = depth,
                explanation = stringResource(R.string.status_depth_explain, depth),
                state = if (report.access == AccessLevel.APP) StatusState.PARTIAL else StatusState.ACTIVE,
                depthLevel = when (report.access) { AccessLevel.APP -> 1; AccessLevel.SHELL -> 2; AccessLevel.ROOT -> 3 },
            ),
            StatusItem(
                kind = StatusKind.SMS,
                label = stringResource(R.string.status_sms),
                explanation = stringResource(if (sms) R.string.status_sms_on else R.string.status_sms_off),
                state = if (sms) StatusState.ACTIVE else StatusState.OFF,
                actionLabel = if (sms || LocalHistoricalResult.current) null else stringResource(R.string.t_055),
                action = if (sms || LocalHistoricalResult.current) null else ({ onGapFix(GapFix.PERMISSION_SMS) }),
            ),
            StatusItem(
                kind = StatusKind.FILES,
                label = stringResource(R.string.status_files),
                explanation = stringResource(if (files) R.string.status_files_on else R.string.status_files_off),
                state = if (files) StatusState.ACTIVE else StatusState.OFF,
                actionLabel = if (files || LocalHistoricalResult.current) null else stringResource(R.string.t_056),
                action = if (files || LocalHistoricalResult.current) null else ({ onGapFix(GapFix.PERMISSION_FILES) }),
            ),
            StatusItem(
                kind = StatusKind.SOURCES,
                label = stringResource(R.string.status_sources_ratio, sources, total),
                explanation = stringResource(R.string.status_sources_explain, sources, total),
                state = if (report.indicatorSourcesMissing == 0) StatusState.ACTIVE else StatusState.PARTIAL,
                actionLabel = if (report.indicatorSourcesMissing == 0 || LocalHistoricalResult.current) null else stringResource(R.string.t_057),
                action = if (report.indicatorSourcesMissing == 0 || LocalHistoricalResult.current) null else ({ onGapFix(GapFix.UPDATE_INDICATORS) }),
            ),
        ))
        // The app language, not the system default: German UI must get German lines.
        val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
        val range = remember(report.oldestEvidence, report.finishedAt, locale) {
            TimeRange.evaluate(report.oldestEvidence, report.finishedAt, locale)
        }
        if (range.hasEvidence) {
            SpySurface(Modifier.fillMaxWidth().padding(top = 8.dp), raised = true) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.result_time_range).uppercase(), style = SpyTypographyExtras.sectionLabel)
                    range.sources.forEach { Text(it.text, style = MaterialTheme.typography.bodySmall, color = SpyColors.IvoryMuted) }
                    if (range.warning) Text(stringResource(R.string.result_time_range_warning), style = MaterialTheme.typography.bodySmall, color = SpyColors.Amber)
                }
            }
        }
        }
    }
}

@Composable
private fun ResultHero(report: Report, symbolModifier: Modifier = Modifier, modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    val danger = report.findings.isNotEmpty()
    val tint = if (danger) SpyColors.Danger else if (report.leads.isNotEmpty()) SpyColors.Amber else SpyColors.Clear
    val findingCount = report.findings.size
    val leadCount = report.leads.size
    val verdict = when {
        findingCount > 0 -> pluralStringResource(R.plurals.result_verdict_findings, findingCount, findingCount)
        leadCount > 0 -> pluralStringResource(R.plurals.result_verdict_leads, leadCount, leadCount)
        else -> stringResource(R.string.result_verdict_clean)
    }
    val depth = when (report.access) {
        AccessLevel.APP -> stringResource(R.string.depth_basic)
        AccessLevel.SHELL -> stringResource(R.string.depth_deep)
        AccessLevel.ROOT -> stringResource(R.string.depth_root)
    }
    val contextLine = stringResource(
        R.string.result_context_line,
        stringResource(R.string.result_context_areas),
        depth,
    )
    var entered by remember(report.startedAt, report.finishedAt) { mutableStateOf(false) }
    LaunchedEffect(report.startedAt, report.finishedAt) { entered = true }
    val wave by animateFloatAsState(if (entered || reduced) 1f else 0f, tween(if (reduced) 0 else SpyMotion.Hero, easing = SpyMotion.Enter), label = "result wave")
    val glow = if (danger && !reduced) {
        val pulse = rememberInfiniteTransition(label = "finding glow")
        val alpha by pulse.animateFloat(
            initialValue = 0.12f, targetValue = 0.3f,
            animationSpec = infiniteRepeatable(tween(SpyMotion.Hero * 4, easing = SpyMotion.Enter), RepeatMode.Reverse),
            label = "finding glow alpha",
        )
        alpha
    } else 0.18f
    Box(
        modifier.fillMaxWidth().height(148.dp).drawBehind {
            val center = Offset(size.width / 2f, 52.dp.toPx())
            if (danger) {
                drawCircle(SpyColors.Danger.copy(alpha = if (reduced) 0.18f else glow), radius = 24.dp.toPx(), center = center)
            } else if (report.verdict == Verdict.NOTHING_FOUND && !reduced && wave < 1f) {
                drawCircle(SpyColors.Clear.copy(alpha = (1f - wave) * 0.55f), radius = (28.dp + 90.dp * wave).toPx(), center = center, style = Stroke(1.dp.toPx()))
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painterResource(if (danger) R.drawable.ic_result_finding else R.drawable.ic_result_clear),
                contentDescription = null,
                tint = tint,
                modifier = symbolModifier.size(48.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                verdict,
                style = MaterialTheme.typography.headlineMedium,
                color = tint,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { contentDescription = verdict },
            )
            Text(
                contextLine,
                style = MaterialTheme.typography.bodyMedium,
                color = SpyColors.IvoryMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CountCell(value: Int, label: String, color: Color) {
    var target by remember(value) { mutableIntStateOf(0) }
    LaunchedEffect(value) { target = value }
    val number = countUp(target)
    val locale = LocalConfiguration.current.locales[0]
    // Pad only to the final value's length, with figure spaces (as wide as a digit): the number counts up in place
    // and still sits centred over its label at the end.
    val width = formatCount(value, locale).length
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = 56.dp)) {
        Text(
            formatCount(number, locale).padStart(width, '\u2007'),
            style = SpyTypographyExtras.tabularHeadline.copy(color = color, fontSize = SpyTypeScale.Heading),
            maxLines = 1,
            modifier = Modifier.semantics { contentDescription = "$value $label" },
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = SpyColors.IvoryMuted, maxLines = 1)
    }
}

private sealed interface ResultEntry {
    data class Header(val label: Int, val hint: Int) : ResultEntry
    data class Card(
        val area: String,
        val headline: String,
        val packageName: String?,
        val detail: String?,
        val guidance: Guidance,
        val source: String? = null,
        val actionTarget: String? = null,
    ) : ResultEntry
}

@Composable
private fun ResultList(
    report: Report,
    state: androidx.compose.foundation.lazy.LazyListState,
    onGapFix: (GapFix) -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier,
    summary: (@Composable () -> Unit)? = null,
) {
    val entries = buildList<ResultEntry> {
        if (report.findings.isNotEmpty()) {
            add(ResultEntry.Header(R.string.result_findings, R.string.result_findings_hint))
            report.findings.forEach { add(it.entry()) }
        }
        if (report.leads.isNotEmpty()) {
            add(ResultEntry.Header(R.string.result_leads, R.string.result_leads_hint))
            report.leads.forEach { add(it.entry()) }
        }
        if (report.gaps.isNotEmpty()) {
            add(ResultEntry.Header(R.string.result_gaps, R.string.result_gaps_hint))
            report.gaps.forEach { add(it.entry()) }
        }
    }
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                val fade = 24.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(SpyColors.Ink.copy(alpha = 0f), SpyColors.Ink),
                        startY = size.height - fade,
                        endY = size.height,
                    ),
                    topLeft = Offset(0f, size.height - fade),
                    size = Size(size.width, fade),
                )
            },
        state = state,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (summary != null) item(key = "summary") { summary() }
        itemsIndexed(entries, key = { index, _ -> index }) { index, entry ->
            when (entry) {
                is ResultEntry.Header -> SectionHeader(stringResource(entry.label), stringResource(entry.hint))
                is ResultEntry.Card -> GuidanceCard(
                    entry,
                    onGapFix,
                    onHelp,
                    Modifier.animateItem().then(staggeredAppear(index)),
                )
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

private fun splitResultHeadline(where: String): Pair<String, String?> {
    val appLine = Regex("^App (.+) \\(([a-zA-Z][a-zA-Z0-9_]*(?:\\.[a-zA-Z][a-zA-Z0-9_]*)+)\\)$").find(where)
    if (appLine != null) {
        return appLine.groupValues[1] to appLine.groupValues[2]
    }
    val trailingPkg = Regex("^(.+) \\(([a-zA-Z][a-zA-Z0-9_]*(?:\\.[a-zA-Z][a-zA-Z0-9_]*)+)\\)$").find(where)
    if (trailingPkg != null) {
        return trailingPkg.groupValues[1] to trailingPkg.groupValues[2]
    }
    val pkg = Regex("[a-zA-Z][a-zA-Z0-9_]*(?:\\.[a-zA-Z][a-zA-Z0-9_]*)+").find(where)?.value
    return where to pkg?.takeIf { it != where }
}

private fun Finding.entry(): ResultEntry.Card {
    val (headline, pkg) = splitResultHeadline(where)
    return ResultEntry.Card(check, headline, pkg, detail, guidanceFor(this), indicator.sourceId, where)
}

private fun Lead.entry(): ResultEntry.Card {
    val (headline, pkg) = splitResultHeadline(where)
    return ResultEntry.Card(check, headline, pkg, detail, guidanceFor(this), actionTarget = where)
}

private fun Gap.entry() = ResultEntry.Card(check, detail, null, null, guidanceFor(this))

@Composable
private fun GuidanceCard(entry: ResultEntry.Card, onGapFix: (GapFix) -> Unit, onHelp: () -> Unit, modifier: Modifier) {
    var open by remember(entry) { mutableStateOf(false) }
    val reduced = rememberReducedMotion()
    val context = LocalContext.current
    val meaning = stringResource(guidanceStringId(entry.guidance.meaningKey))
    val steps = stringResource(guidanceStringId(entry.guidance.stepsKey))
    val stateLabel = stringResource(if (open) R.string.result_collapse else R.string.result_expand)
    val chevronRotation by animateFloatAsState(
        if (open) 180f else 0f,
        tween(if (reduced) 0 else SpyMotion.Element, easing = SpyMotion.Enter),
        label = "card chevron",
    )
    SpySurface(modifier.fillMaxWidth(), raised = open) {
        Column {
            Row(
                Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp)
                    .clickable(role = Role.Button) { open = !open }
                    .semantics {
                        contentDescription = "${entry.area}. ${entry.headline}. $stateLabel"
                        stateDescription = stateLabel
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(entry.area.uppercase(), style = SpyTypographyExtras.sectionLabel)
                    Text(
                        entry.headline,
                        style = MaterialTheme.typography.titleMedium,
                        color = SpyColors.Ivory,
                    )
                    entry.packageName?.let { pkg ->
                        Text(
                            pkg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SpyColors.IvoryMuted,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    painterResource(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    tint = SpyColors.Amber,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = chevronRotation },
                )
            }
            AnimatedVisibility(visible = open) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    entry.detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = SpyColors.IvoryMuted) }
                    entry.source?.takeIf { it.isNotBlank() }?.let {
                        Text(stringResource(R.string.source_named, it), style = MaterialTheme.typography.labelMedium, color = SpyColors.IvoryMuted)
                    }
                    Text(stringResource(R.string.result_meaning), style = SpyTypographyExtras.sectionLabel)
                    Text(meaning, style = MaterialTheme.typography.bodyMedium, color = SpyColors.Ivory)
                    Text(stringResource(R.string.result_steps), style = SpyTypographyExtras.sectionLabel)
                    Text(emphasizeLabels(steps), style = MaterialTheme.typography.bodyMedium, color = SpyColors.Ivory)
                    (if (LocalHistoricalResult.current) null else entry.guidance.action)?.let { action ->
                        SpyButton(
                            stringResource(
                                if (action == GuidanceAction.GET_HELP && entry.guidance.meaningKey.contains("stalkerware")) R.string.result_call_help
                                else if (action == GuidanceAction.GET_HELP) R.string.result_email_help
                                else action.label()
                            ),
                            onClick = { performGuidanceAction(action, entry, context, onGapFix, onHelp) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

private fun GuidanceAction.label(): Int = when (this) {
    GuidanceAction.APP_INFO -> R.string.result_app_info
    GuidanceAction.SETTINGS_SECURITY -> R.string.result_security
    GuidanceAction.SETTINGS_DEVELOPER -> R.string.result_developer
    GuidanceAction.SETTINGS_ACCESSIBILITY -> R.string.result_accessibility
    GuidanceAction.SETTINGS_UNKNOWN_SOURCES -> R.string.result_unknown_sources
    GuidanceAction.PERMISSION_SMS -> R.string.t_055
    GuidanceAction.PERMISSION_FILES -> R.string.t_056
    GuidanceAction.SETUP_DEEP_CHECK -> R.string.t_042
    GuidanceAction.UPDATE_INDICATORS -> R.string.t_057
    GuidanceAction.GET_HELP -> R.string.t_050
}

private fun performGuidanceAction(
    action: GuidanceAction,
    entry: ResultEntry.Card,
    context: android.content.Context,
    onGapFix: (GapFix) -> Unit,
    onHelp: () -> Unit,
) {
    when (action) {
        GuidanceAction.APP_INFO -> {
            val packageName = Regex("[a-zA-Z][a-zA-Z0-9_]*(?:\\.[a-zA-Z][a-zA-Z0-9_]*)+").find(entry.actionTarget.orEmpty())?.value
            if (packageName != null) context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
        GuidanceAction.SETTINGS_SECURITY -> context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
        GuidanceAction.SETTINGS_DEVELOPER -> context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        GuidanceAction.SETTINGS_ACCESSIBILITY -> context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        GuidanceAction.SETTINGS_UNKNOWN_SOURCES -> context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES))
        GuidanceAction.PERMISSION_SMS -> onGapFix(GapFix.PERMISSION_SMS)
        GuidanceAction.PERMISSION_FILES -> onGapFix(GapFix.PERMISSION_FILES)
        GuidanceAction.SETUP_DEEP_CHECK -> onGapFix(GapFix.SHIZUKU)
        GuidanceAction.UPDATE_INDICATORS -> onGapFix(GapFix.UPDATE_INDICATORS)
        GuidanceAction.GET_HELP -> {
            val target = if (entry.guidance.meaningKey.contains("stalkerware")) {
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:116016"))
            } else {
                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:help@accessnow.org"))
            }
            if (target.resolveActivity(context.packageManager) != null) context.startActivity(target) else onHelp()
        }
    }
}
