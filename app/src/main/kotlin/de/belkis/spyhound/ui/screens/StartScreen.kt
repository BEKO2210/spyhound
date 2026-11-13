package de.belkis.spyhound.ui.screens

import kotlin.math.roundToInt
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import de.belkis.spyhound.ui.components.emphasizeLabels
import de.belkis.spyhound.ui.design.SpyMotion
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.res.stringArrayResource
import de.belkis.spyhound.ui.buildStartStatements
import de.belkis.spyhound.ui.activeSourceNames
import de.belkis.spyhound.ui.components.StatementBanner
import de.belkis.spyhound.ui.design.rememberReducedMotion
import kotlinx.coroutines.delay
import androidx.compose.animation.Crossfade
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.res.pluralStringResource
import de.belkis.spyhound.scan.HardeningAction
import de.belkis.spyhound.scan.HardeningId
import de.belkis.spyhound.scan.HardeningItem
import de.belkis.spyhound.scan.HardeningState
import de.belkis.spyhound.R
import de.belkis.spyhound.dns.DnsFilterSettings
import de.belkis.spyhound.dns.DnsHit
import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.ui.SpyColors
import de.belkis.spyhound.ui.SpyTypographyExtras
import de.belkis.spyhound.ui.UiState
import de.belkis.spyhound.ui.components.CardFeedbackLine
import de.belkis.spyhound.ui.components.CardRowIconBadge
import de.belkis.spyhound.ui.components.DepthProgressBar
import de.belkis.spyhound.ui.components.InfoCircleButton
import de.belkis.spyhound.ui.components.LanguageToggle
import de.belkis.spyhound.ui.components.RadarScanButton
import de.belkis.spyhound.ui.components.ScrollEdge
import de.belkis.spyhound.ui.components.SectionHeader
import de.belkis.spyhound.ui.components.SpyButton
import de.belkis.spyhound.ui.components.SpyCard
import de.belkis.spyhound.ui.components.StatusItem
import de.belkis.spyhound.ui.components.StatusKind
import de.belkis.spyhound.ui.components.StatusState
import de.belkis.spyhound.ui.components.StatusStrip
import de.belkis.spyhound.ui.components.TextActionLink
import de.belkis.spyhound.ui.design.SpySpacing
import de.belkis.spyhound.ui.design.countUp
import de.belkis.spyhound.ui.design.staggeredAppear
import java.text.DateFormat
import java.util.Date
import de.belkis.spyhound.ui.formatCount

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StartScreen(
    state: UiState,
    onScan: () -> Unit,
    onUpdateIndicators: () -> Unit,
    onRequestRoot: () -> Unit,
    onRequestSms: () -> Unit,
    onRequestFiles: () -> Unit,
    onDnsToggle: (Boolean) -> Unit,
    onWeeklyBackgroundToggle: (Boolean) -> Unit,
    onPickBugreport: () -> Unit,
    onDismissBanner: () -> Unit,
    onDemoReport: () -> Unit = {},
    onDnsFilterChange: (DnsFilterSettings) -> Unit = {},
    onDeepCheckSetup: () -> Unit,
    onOpenInfo: () -> Unit,
    onOpenPermissionAudit: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    radarModifier: Modifier = Modifier,
) {
    var showBugreportSteps by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val context = LocalContext.current
    var notificationsGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val notificationsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsGranted = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    }
    val tablet = LocalConfiguration.current.screenWidthDp >= 600
    val headerContent: @Composable ColumnScope.(radarDiameter: Dp, stripCollapse: () -> Float) -> Unit = { radarDiameter, stripCollapse ->
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val brand: @Composable (Modifier) -> Unit = { brandModifier ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = brandModifier
                        .combinedClickable(
                            onClick = {},
                            onLongClick = onDemoReport,
                        )
                        .padding(vertical = 4.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.hound_mark),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.labelLarge,
                        color = SpyColors.IvoryMuted,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                // Toggle, logo and info button share one row whenever the column is as wide as a phone; only a
                // narrower column puts the logo on its own line above them.
                val oneRow = maxWidth >= 320.dp
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!oneRow) brand(Modifier)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                    ) {
                        if (oneRow) brand(Modifier.align(Alignment.Center))
                        LanguageToggle(
                            modifier = Modifier.align(Alignment.CenterStart),
                        )
                        InfoCircleButton(
                            onClick = onOpenInfo,
                            modifier = Modifier.align(Alignment.CenterEnd),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            // While the cards scroll, the radar shrinks with the finger to about half its size and grows back on
            // the way up. Scale and height follow the scroll position directly, so nothing jumps.
            Box(
                Modifier
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val shrink = RadarScrollShrink * stripCollapse().coerceIn(0f, 1f)
                        val height = (placeable.height * (1f - shrink)).roundToInt()
                        layout(placeable.width, height) { placeable.place(0, (height - placeable.height) / 2) }
                    }
                    .graphicsLayer {
                        val scale = 1f - RadarScrollShrink * stripCollapse().coerceIn(0f, 1f)
                        scaleX = scale
                        scaleY = scale
                    },
            ) {
                RadarScanButton(
                    onClick = onScan,
                    enabled = state.iocs != null && state.progress == null,
                    modifier = radarModifier,
                    lastRadar = state.lastRadar,
                    diameter = radarDiameter,
                )
            }
            Spacer(Modifier.height(12.dp))
            val depth = when (state.access) {
                AccessLevel.APP -> stringResource(R.string.depth_basic)
                AccessLevel.SHELL -> stringResource(R.string.depth_deep)
                AccessLevel.ROOT -> stringResource(R.string.depth_root)
            }
            val sources = state.iocs?.status?.available ?: 0
            // The catalogue grows (29 spyware feeds, then the malware lists); count it instead of hard-coding.
            val sourcesTotal = state.iocs?.status?.let { it.available + it.missing.size + it.corrupt.size } ?: 0
            val depthLevel = when (state.access) {
                AccessLevel.APP -> 1
                AccessLevel.SHELL -> 2
                AccessLevel.ROOT -> 3
            }
            val filesViaShell = state.access != AccessLevel.APP
            StatusStrip(
                listOf(
                StatusItem(
                    kind = StatusKind.DEPTH,
                    label = depth,
                    explanation = stringResource(R.string.status_depth_explain, depth),
                    state = if (state.access == AccessLevel.APP) StatusState.PARTIAL else StatusState.ACTIVE,
                    depthLevel = depthLevel,
                    actionLabel = stringResource(R.string.status_action_assistant),
                    action = onDeepCheckSetup,
                ),
                StatusItem(
                    kind = StatusKind.SMS,
                    label = stringResource(R.string.status_sms),
                    explanation = stringResource(if (state.smsGranted) R.string.status_sms_on else R.string.status_sms_off),
                    state = if (state.smsGranted) StatusState.ACTIVE else StatusState.OFF,
                    actionLabel = if (state.smsGranted) null else stringResource(R.string.start_allow),
                    action = if (state.smsGranted) null else onRequestSms,
                ),
                StatusItem(
                    kind = StatusKind.FILES,
                    label = stringResource(R.string.status_files),
                    // With shell rights the scan reads shared storage itself, so files count as checked
                    // even without the app permission - the result says the same.
                    explanation = stringResource(
                        when {
                            state.filesGranted -> R.string.status_files_on
                            filesViaShell -> R.string.status_files_shell
                            else -> R.string.status_files_off
                        },
                    ),
                    state = if (state.filesGranted || filesViaShell) StatusState.ACTIVE else StatusState.OFF,
                    actionLabel = if (state.filesGranted || filesViaShell) null else stringResource(R.string.start_allow),
                    action = if (state.filesGranted || filesViaShell) null else onRequestFiles,
                ),
                StatusItem(
                    kind = StatusKind.SOURCES,
                    label = stringResource(R.string.status_sources_ratio, sources, sourcesTotal),
                    explanation = stringResource(R.string.status_sources_explain, sources, sourcesTotal),
                    state = if (sourcesTotal > 0 && sources >= sourcesTotal) StatusState.ACTIVE else StatusState.PARTIAL,
                    actionLabel = stringResource(R.string.t_063),
                    action = onUpdateIndicators,
                ),
                StatusItem(
                    kind = StatusKind.DNS,
                    label = stringResource(R.string.status_dns),
                    explanation = stringResource(if (state.dnsRunning) R.string.status_dns_on else R.string.status_dns_off),
                    state = if (state.dnsRunning) StatusState.ACTIVE else StatusState.OFF,
                    actionLabel = stringResource(if (state.dnsRunning) R.string.status_action_stop else R.string.status_action_start),
                    action = { onDnsToggle(!state.dnsRunning) },
                ),
                ),
                modifier = Modifier.fillMaxWidth(),
                collapse = stripCollapse,
            )
        }
    }
    val defaultRadarDiameter = 176.dp
    val cardsBody: @Composable ColumnScope.() -> Unit = {
        var staggerIndex = 0
            val statementLocale = LocalConfiguration.current.locales[0]
            val statementTexts = stringArrayResource(R.array.start_statements).toList()
            val statements = remember(statementTexts, state.iocs, statementLocale) {
                buildStartStatements(
                    statementTexts,
                    indicatorCount = state.iocs?.set?.size ?: 0,
                    activeSources = state.iocs?.status?.available ?: 0,
                    sourceNames = activeSourceNames(state.iocs?.status?.active.orEmpty()),
                    locale = statementLocale,
                )
            }
            StatementBanner(
                statements,
                modifier = Modifier.then(staggeredAppear(staggerIndex++)),
                paused = scroll.isScrollInProgress,
            )
            if (state.access != AccessLevel.ROOT) {
                SpyCard(Modifier.then(staggeredAppear(staggerIndex++))) {
                    SectionHeader(
                        label = stringResource(R.string.start_card_depth),
                        hint = stringResource(R.string.start_help_access),
                    )
                    DepthProgressBar(current = state.access)
                    when (state.access) {
                        AccessLevel.APP -> {
                            Text(
                                stringResource(R.string.start_depth_explain),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SpyColors.IvoryMuted,
                            )
                            SpyButton(
                                text = stringResource(R.string.t_004),
                                onClick = onDeepCheckSetup,
                                accent = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            RootRequestSlot(asking = state.rootAsking, denied = state.rootDenied, onRequest = onRequestRoot)
                        }
                        AccessLevel.SHELL -> {
                            Text(
                                stringResource(R.string.t_007),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SpyColors.Clear,
                            )
                            RootRequestSlot(asking = state.rootAsking, denied = state.rootDenied, onRequest = onRequestRoot)
                        }
                        AccessLevel.ROOT -> Unit
                    }
                }
            }
            state.iocs?.let { loaded ->
                val updateFeedback = rememberUpdateFeedback(state.updateMessage)
                SpyCard(Modifier.then(staggeredAppear(staggerIndex++))) {
                    SectionHeader(
                        label = stringResource(R.string.t_062),
                        hint = stringResource(R.string.start_help_indicators),
                    )
                    val animatedCount = countUp(loaded.set.size)
                    val locale = LocalConfiguration.current.locales[0]
                    val formattedCount = remember(animatedCount, locale) { formatCount(animatedCount, locale) }
                    Text(
                        formattedCount,
                        style = SpyTypographyExtras.tabularHeadline,
                    )
                    Text(
                        stringResource(
                            R.string.start_indicators_from_sources,
                            loaded.status.available,
                            loaded.status.sources,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SpyColors.IvoryMuted,
                    )
                    if (loaded.status.missing.isNotEmpty()) {
                        Text(
                            pluralStringResource(R.plurals.sources_missing, loaded.status.missing.size, loaded.status.missing.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SpyColors.IvoryMuted,
                        )
                    }
                    val allSourcesReady = loaded.status.missing.isEmpty() &&
                        loaded.status.corrupt.isEmpty() &&
                        loaded.status.available >= loaded.status.sources
                    SpyButton(
                        text = when {
                            state.updating != null -> state.updating
                            allSourcesReady -> stringResource(R.string.indicators_update_sources)
                            else -> stringResource(R.string.t_063)
                        },
                        onClick = onUpdateIndicators,
                        enabled = state.updating == null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CardFeedbackLine(
                        message = updateFeedback?.display,
                        isError = updateFeedback?.isError == true,
                        contentDescription = state.updateMessage,
                    )
                }
            }
            SpyCard(Modifier.then(staggeredAppear(staggerIndex++))) {
                SectionHeader(
                    label = stringResource(R.string.t_064),
                    hint = stringResource(R.string.start_help_dns),
                )
                DnsWatchCardBody(state = state, onDnsToggle = onDnsToggle, onFilterChange = onDnsFilterChange)
            }
            SpyCard(Modifier.then(staggeredAppear(staggerIndex++))) {
                SectionHeader(
                    label = stringResource(R.string.t_067),
                    hint = stringResource(R.string.start_help_bugreport),
                )
                Text(
                    stringResource(R.string.t_068),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpyColors.IvoryMuted,
                )
                SpyButton(
                    text = stringResource(R.string.t_069),
                    onClick = onPickBugreport,
                    enabled = state.iocs != null && state.busy == null,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextActionLink(
                    text = if (showBugreportSteps) stringResource(R.string.t_070) else stringResource(R.string.t_071),
                    onClick = { showBugreportSteps = !showBugreportSteps },
                )
                AnimatedVisibility(showBugreportSteps) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(emphasizeLabels(stringResource(R.string.t_072)), style = MaterialTheme.typography.bodyLarge)
                        Text(emphasizeLabels(stringResource(R.string.t_073)), style = MaterialTheme.typography.bodyLarge)
                        Text(emphasizeLabels(stringResource(R.string.t_074)), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            emphasizeLabels(stringResource(R.string.t_075) + stringResource(R.string.t_076)),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            SpyCard(Modifier.then(staggeredAppear(staggerIndex++))) {
                SectionHeader(
                    label = stringResource(R.string.t_078),
                    hint = stringResource(R.string.start_help_permissions),
                )
                PermissionCardRow(
                    iconRes = R.drawable.ic_status_sms,
                    title = stringResource(R.string.t_079),
                    subtitle = stringResource(R.string.t_080),
                    granted = state.smsGranted,
                    onAllow = onRequestSms,
                )
                HorizontalDivider(color = SpyColors.Border, thickness = 1.dp)
                PermissionCardRow(
                    iconRes = R.drawable.ic_status_files,
                    title = stringResource(R.string.t_081),
                    subtitle = stringResource(R.string.t_082),
                    granted = state.filesGranted,
                    onAllow = onRequestFiles,
                )
                HorizontalDivider(color = SpyColors.Border, thickness = 1.dp)
                PermissionCardRow(
                    iconRes = R.drawable.ic_status_notifications,
                    title = stringResource(R.string.start_permission_notifications),
                    subtitle = stringResource(R.string.start_permission_notifications_hint),
                    granted = notificationsGranted,
                    onAllow = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }
            PermissionAuditEntryCard(onOpenPermissionAudit, Modifier.then(staggeredAppear(staggerIndex++)))
            HistoryEntryCard(onOpenHistory, Modifier.then(staggeredAppear(staggerIndex++)))
            BackgroundWeeklyCard(
                enabled = state.weeklyBackgroundEnabled,
                onToggle = onWeeklyBackgroundToggle,
                modifier = Modifier.then(staggeredAppear(staggerIndex++)),
            )
            if (state.hardening.isNotEmpty()) HardeningCard(state.hardening, Modifier.then(staggeredAppear(staggerIndex++)))
            Spacer(Modifier.height(4.dp))
    }
    val cardsContent: @Composable ColumnScope.() -> Unit = {
        val scrolled by remember { derivedStateOf { scroll.value > 0 } }
        Spacer(Modifier.height(10.dp))
        ScrollEdge(scrolled)
        Column(
            Modifier
                .weight(1f)
                .drawWithContent {
                    drawContent()
                    if (scrolled) {
                        drawRect(
                            Brush.verticalGradient(
                                listOf(SpyColors.Ink, SpyColors.Ink.copy(alpha = 0f)),
                                endY = 28.dp.toPx(),
                            ),
                            size = androidx.compose.ui.geometry.Size(size.width, 28.dp.toPx()),
                        )
                    }
                }
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(SpySpacing.Section),
        ) {
            cardsBody()
        }
    }
    Box(Modifier.fillMaxSize()) {
        if (tablet) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(SpySpacing.Section)) {
                BoxWithConstraints(
                    Modifier.weight(1f).fillMaxHeight(),
                ) {
                    val radarDiameter = (maxWidth * 0.8f).coerceAtMost(maxHeight * 0.5f)
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        headerContent(radarDiameter) { 0f }
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight(), content = cardsContent)
            }
        } else {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                // The radar takes about a quarter of the height, so on small screens, with large text or display
                // zoom it shrinks instead of pushing the cards into a sliver at the bottom.
                val radarDiameter = (maxHeight * 0.24f).coerceIn(120.dp, defaultRadarDiameter)
                if (maxHeight < CompactHeight) {
                    // Too short for a fixed header: everything scrolls as one page.
                    Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                        headerContent(radarDiameter) { 0f }
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(SpySpacing.Section), content = cardsBody)
                    }
                } else {
                    // Scrolling the cards folds the status strip down to its icons; scrolling back up unfolds it.
                    val collapseDistance = with(LocalDensity.current) { StripCollapseDistance.toPx() }
                    val reducedMotion = rememberReducedMotion()
                    // When the finger lets go between folded and unfolded, settle softly to the nearer state. A new
                    // touch interrupts the animation, so it never fights the finger.
                    LaunchedEffect(collapseDistance, reducedMotion) {
                        if (reducedMotion) return@LaunchedEffect
                        snapshotFlow { scroll.isScrollInProgress }.collect { moving ->
                            if (moving) return@collect
                            delay(SettleDelayMs)
                            val folded = minOf(collapseDistance, scroll.maxValue.toFloat())
                            val at = scroll.value.toFloat()
                            if (scroll.isScrollInProgress || folded <= 0f || at <= 0f || at >= folded) return@collect
                            val target = if (at < folded / 2f) 0 else folded.roundToInt()
                            scroll.animateScrollTo(target, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessLow))
                        }
                    }
                    Column(Modifier.fillMaxSize()) {
                        headerContent(radarDiameter) { FastOutSlowInEasing.transform((scroll.value / collapseDistance).coerceIn(0f, 1f)) }
                        cardsContent()
                    }
                }
            }
        }
        // Status messages float above the list instead of being inserted into the header, which used to
        // push the whole screen down and back up again.
        StatusToast(
            message = state.busy ?: state.bannerMessage,
            busy = state.busy != null,
            onDismiss = if (state.busy == null && state.bannerMessage != null) onDismissBanner else null,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
        )
    }
}

@Composable
fun BackgroundWeeklyCard(enabled: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    SpyCard(modifier) {
        SectionHeader(
            label = stringResource(R.string.start_card_background),
            hint = stringResource(R.string.start_help_background),
        )
        val enabledLabel = stringResource(if (enabled) R.string.start_dns_state_on else R.string.start_dns_state_off)
        val title = stringResource(R.string.start_background_weekly)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = SpyColors.Ivory,
                modifier = Modifier.weight(1f),
            )
            Text(
                enabledLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) SpyColors.Clear else SpyColors.IvoryMuted,
                modifier = Modifier.padding(end = 8.dp),
            )
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics { contentDescription = "$title: $enabledLabel" },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SpyColors.Ivory,
                    checkedTrackColor = SpyColors.Clear,
                    uncheckedThumbColor = SpyColors.IvoryMuted,
                    uncheckedTrackColor = SpyColors.Border,
                    checkedBorderColor = SpyColors.Border,
                    uncheckedBorderColor = SpyColors.Border,
                ),
            )
        }
        Text(
            stringResource(R.string.start_background_weekly_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = SpyColors.IvoryMuted,
        )
    }
}

@Composable
private fun DnsWatchCardBody(state: UiState, onDnsToggle: (Boolean) -> Unit, onFilterChange: (DnsFilterSettings) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val stateLabel = stringResource(
        if (state.dnsRunning) R.string.start_dns_state_on else R.string.start_dns_state_off,
    )
    val dnsTitle = stringResource(R.string.t_064)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.t_064),
            style = MaterialTheme.typography.labelLarge,
            color = SpyColors.Ivory,
            modifier = Modifier.weight(1f),
        )
        Text(
            stateLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = if (state.dnsRunning) SpyColors.Clear else SpyColors.IvoryMuted,
            modifier = Modifier.padding(end = 8.dp),
        )
        Switch(
            checked = state.dnsRunning,
            onCheckedChange = onDnsToggle,
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .semantics { contentDescription = "$dnsTitle: $stateLabel" },
            colors = SwitchDefaults.colors(
                checkedThumbColor = SpyColors.Ivory,
                checkedTrackColor = SpyColors.Clear,
                uncheckedThumbColor = SpyColors.IvoryMuted,
                uncheckedTrackColor = SpyColors.Border,
                checkedBorderColor = SpyColors.Border,
                uncheckedBorderColor = SpyColors.Border,
            ),
        )
    }
    Text(
        stringResource(R.string.t_065),
        style = MaterialTheme.typography.bodyMedium,
        color = SpyColors.IvoryMuted,
    )
    val f = state.dnsFilter
    Spacer(Modifier.height(8.dp))
    DnsFilterRow(
        title = stringResource(R.string.dns_filter_phishing),
        warn = f.warnPhishing, block = f.blockPhishing,
        onWarn = { onFilterChange(f.copy(warnPhishing = it)) },
        onBlock = { onFilterChange(f.copy(blockPhishing = it)) },
    )
    DnsFilterRow(
        title = stringResource(R.string.dns_filter_tracker),
        warn = f.warnTracker, block = f.blockTracker,
        onWarn = { onFilterChange(f.copy(warnTracker = it)) },
        onBlock = { onFilterChange(f.copy(blockTracker = it)) },
    )
    Text(
        stringResource(R.string.dns_filter_note),
        style = MaterialTheme.typography.bodyMedium,
        color = SpyColors.IvoryMuted,
    )
    if (state.dnsHits.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.t_066), style = MaterialTheme.typography.labelLarge)
        val fmt = remember(locale) { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale) }
        state.dnsHits.take(5).forEach { hit -> DnsHitLine(hit, fmt) }
    }
}

@Composable
private fun DnsFilterRow(
    title: String,
    warn: Boolean,
    block: Boolean,
    onWarn: (Boolean) -> Unit,
    onBlock: (Boolean) -> Unit,
) {
    val warnLabel = stringResource(R.string.dns_filter_warn)
    val blockLabel = stringResource(R.string.dns_filter_block)
    val colors = SwitchDefaults.colors(
        checkedThumbColor = SpyColors.Ivory,
        checkedTrackColor = SpyColors.Clear,
        uncheckedThumbColor = SpyColors.IvoryMuted,
        uncheckedTrackColor = SpyColors.Border,
        checkedBorderColor = SpyColors.Border,
        uncheckedBorderColor = SpyColors.Border,
    )
    Text(title, style = MaterialTheme.typography.labelLarge, color = SpyColors.Ivory)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(warnLabel, style = MaterialTheme.typography.bodyMedium, color = SpyColors.IvoryMuted, modifier = Modifier.weight(1f))
        Switch(
            checked = warn,
            onCheckedChange = onWarn,
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .semantics { contentDescription = "$title: $warnLabel" },
            colors = colors,
        )
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(blockLabel, style = MaterialTheme.typography.bodyMedium, color = SpyColors.IvoryMuted, modifier = Modifier.weight(1f))
        Switch(
            checked = block,
            onCheckedChange = onBlock,
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .semantics { contentDescription = "$title: $blockLabel" },
            colors = colors,
        )
    }
}

private data class UpdateFeedback(val display: String, val isError: Boolean)

@Composable
private fun rememberUpdateFeedback(raw: String?): UpdateFeedback? {
    if (raw == null) return null
    val errorShort = stringResource(R.string.start_update_error)
    val isError = raw.contains("failed", ignoreCase = true) || raw.contains("fehlgeschlagen", ignoreCase = true)
    return UpdateFeedback(if (isError) errorShort else raw, isError)
}

@Composable
private fun PermissionCardRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    granted: Boolean,
    onAllow: () -> Unit,
) {
    val allowLabel = stringResource(R.string.start_allow)
    val okLabel = stringResource(R.string.status_ok)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(
                enabled = !granted,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onAllow,
            )
            .semantics {
                contentDescription = if (granted) "$title: $okLabel" else "$title: $allowLabel"
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CardRowIconBadge(iconRes = iconRes, active = granted)
        Column(
            Modifier
                .weight(1f)
                .widthIn(min = 0.dp)
                .padding(start = 14.dp, end = 8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = SpyColors.Ivory)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = SpyColors.IvoryMuted,
            )
        }
        if (granted) {
            Icon(
                painter = painterResource(R.drawable.ic_permission_granted),
                contentDescription = okLabel,
                tint = SpyColors.Clear,
                modifier = Modifier.size(22.dp),
            )
        } else {
            TextActionLink(text = allowLabel, onClick = onAllow, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
fun HardeningCard(items: List<HardeningItem>, modifier: Modifier = Modifier) {
    SpyCard(modifier) {
        SectionHeader(
            label = stringResource(R.string.harden_title),
            hint = stringResource(R.string.harden_hint),
        )
        items.forEachIndexed { i, item ->
            if (i > 0) HorizontalDivider(color = SpyColors.Border, thickness = 1.dp)
            HardeningCardRow(item)
        }
    }
}

@Composable
private fun HardeningCardRow(item: HardeningItem) {
    val context = LocalContext.current
    val ok = item.state == HardeningState.OK
    val (iconRes, titleRes) = when (item.id) {
        HardeningId.SCREEN_LOCK -> R.drawable.ic_hardening_lock to R.string.harden_lock_title
        HardeningId.USB_DEBUGGING -> R.drawable.ic_hardening_usb to R.string.harden_usb_title
        HardeningId.WIFI_DEBUGGING -> R.drawable.ic_hardening_wifi to R.string.harden_wifi_title
        HardeningId.DEVELOPER_OPTIONS -> R.drawable.ic_hardening_developer to R.string.harden_dev_title
        HardeningId.SECURITY_PATCH -> R.drawable.ic_hardening_patch to R.string.harden_patch_title
        HardeningId.UNKNOWN_APP_INSTALLERS -> R.drawable.ic_hardening_install to R.string.harden_install_title
    }
    val subtitle = if (item.state == HardeningState.UNKNOWN) stringResource(R.string.harden_switch_unknown) else when (item.id) {
        HardeningId.SCREEN_LOCK -> stringResource(if (ok) R.string.harden_lock_ok else R.string.harden_lock_bad)
        HardeningId.USB_DEBUGGING -> stringResource(if (ok) R.string.harden_usb_ok else R.string.harden_usb_bad)
        HardeningId.WIFI_DEBUGGING -> stringResource(if (ok) R.string.harden_wifi_ok else R.string.harden_wifi_bad)
        HardeningId.DEVELOPER_OPTIONS -> stringResource(if (ok) R.string.harden_dev_ok else R.string.harden_dev_bad)
        HardeningId.SECURITY_PATCH ->
            stringResource(if (ok) R.string.harden_patch_ok else R.string.harden_patch_bad, item.detail)
        HardeningId.UNKNOWN_APP_INSTALLERS ->
            if (ok) stringResource(R.string.harden_install_ok)
            else pluralStringResource(R.plurals.harden_install_bad, item.detail, item.detail)
    }
    val title = stringResource(titleRes)
    val openLabel = stringResource(R.string.harden_open)
    val okLabel = stringResource(R.string.status_ok)
    val stateLabel = when (item.state) {
        HardeningState.OK -> okLabel
        HardeningState.RECOMMENDED -> stringResource(R.string.harden_recommended)
        HardeningState.IMPORTANT -> stringResource(R.string.harden_important)
        HardeningState.UNKNOWN -> stringResource(R.string.harden_unknown)
    }
    val open = { openHardeningAction(context, item.action) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(
                enabled = !ok,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = open,
            )
            .semantics { contentDescription = if (ok) "$title: $okLabel. $subtitle" else "$title: $stateLabel. $subtitle. $openLabel" }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CardRowIconBadge(iconRes = iconRes, active = ok)
        Column(
            Modifier
                .weight(1f)
                .widthIn(min = 0.dp)
                .padding(start = 14.dp, end = 8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = SpyColors.Ivory)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = SpyColors.IvoryMuted)
        }
        if (ok) {
            Icon(
                painter = painterResource(R.drawable.ic_permission_granted),
                contentDescription = null,
                tint = SpyColors.Clear,
                modifier = Modifier.size(22.dp),
            )
        } else {
            TextActionLink(text = openLabel, onClick = open, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

private fun openHardeningAction(context: Context, action: HardeningAction) {
    val primary = when (action) {
        HardeningAction.SECURITY_SETTINGS -> Settings.ACTION_SECURITY_SETTINGS
        HardeningAction.DEVELOPER_SETTINGS -> Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
        HardeningAction.SYSTEM_UPDATE -> Settings.ACTION_DEVICE_INFO_SETTINGS
        HardeningAction.UNKNOWN_APPS -> Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
    }
    // Some phones lack a screen for the exact target; the general settings always open.
    runCatching { context.startActivity(Intent(primary).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .recoverCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

@Composable
private fun DnsHitLine(hit: DnsHit, fmt: DateFormat) {
    Text(
        buildString {
            append("${fmt.format(Date(hit.atMillis))} · ${hit.domain}")
            if (hit.blocked) append(" · ").append(stringResource(R.string.dns_hit_blocked))
        },
        style = MaterialTheme.typography.bodyMedium,
        color = if (hit.category.isEvidence || hit.category == Category.PHISHING) SpyColors.Danger else SpyColors.IvoryMuted,
    )
}

/**
 * The root request answers in place: link, then "asking", then a calm note. One fixed-height slot with a
 * crossfade, so the card never changes size. No root is the normal case on most phones, so it is not an error.
 */
@Composable
private fun RootRequestSlot(asking: Boolean, denied: Boolean, onRequest: () -> Unit) {
    val phase = when {
        asking -> 1
        denied -> 2
        else -> 0
    }
    Crossfade(
        targetState = phase,
        animationSpec = tween(SpyMotion.Element, easing = SpyMotion.Enter),
        label = "root request",
        modifier = Modifier.fillMaxWidth().height(48.dp),
    ) { p ->
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (p) {
                1 -> {
                    CircularProgressIndicator(Modifier.size(16.dp), color = SpyColors.Amber, strokeWidth = 2.dp)
                    Text(
                        stringResource(R.string.root_asking),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SpyColors.IvoryMuted,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
                2 -> {
                    // One baseline for note and link, so the line reads as one sentence.
                    Text(stringResource(R.string.root_unavailable), style = MaterialTheme.typography.bodyMedium, color = SpyColors.IvoryMuted, modifier = Modifier.alignByBaseline())
                    Text("  ·  ", style = MaterialTheme.typography.bodyMedium, color = SpyColors.IvoryMuted, modifier = Modifier.alignByBaseline())
                    TextActionLink(text = stringResource(R.string.root_retry), onClick = onRequest, modifier = Modifier.alignByBaseline())
                }
                else -> TextActionLink(text = stringResource(R.string.t_006), onClick = onRequest)
            }
        }
    }
}

/** A floating status pill; it fades and lifts in, so nothing underneath moves. */
@Composable
private fun StatusToast(message: String?, busy: Boolean, onDismiss: (() -> Unit)?, modifier: Modifier = Modifier) {
    var last by remember { mutableStateOf(message) }
    if (message != null) last = message
    val shown = message != null
    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(SpyMotion.Element, easing = SpyMotion.Enter), label = "toast alpha")
    val lift by animateFloatAsState(if (shown) 0f else 1f, tween(SpyMotion.Element, easing = SpyMotion.Enter), label = "toast lift")
    val text = last ?: return
    if (alpha == 0f && !shown) return
    Row(
        modifier
            .padding(horizontal = 20.dp)
            .graphicsLayer { this.alpha = alpha; translationY = lift * 16.dp.toPx() }
            .clip(RoundedCornerShape(24.dp))
            .background(SpyColors.SurfaceRaised)
            .border(1.dp, SpyColors.Border, RoundedCornerShape(24.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) CircularProgressIndicator(Modifier.size(16.dp), color = SpyColors.Amber, strokeWidth = 2.dp)
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = SpyColors.Ivory,
            modifier = Modifier.padding(start = if (busy) 10.dp else 0.dp).weight(1f, fill = false),
        )
        if (onDismiss != null) TextActionLink(text = stringResource(R.string.t_060), onClick = onDismiss, modifier = Modifier.padding(start = 12.dp))
    }
}

/** Below this height the start screen header scrolls with the cards instead of staying fixed. */
private val CompactHeight = 700.dp

/** How much of its size the radar gives up when the cards are scrolled (0.45 = shrinks to 55 %). */
private const val RadarScrollShrink = 0.45f

/** Scroll distance over which the radar shrinks and the status strip folds; long enough to follow the finger calmly. */
private val StripCollapseDistance = 220.dp

/** Pause after the scroll stops before the fold settles, so a finger that only rests does not trigger it. */
private const val SettleDelayMs = 80L
