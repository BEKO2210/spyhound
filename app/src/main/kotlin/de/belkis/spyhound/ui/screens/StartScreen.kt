package de.belkis.spyhound.ui.screens

import de.belkis.spyhound.ui.design.SpyMotion
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.belkis.spyhound.R
import de.belkis.spyhound.dns.DnsHit
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
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

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
    onPickBugreport: () -> Unit,
    onDismissBanner: () -> Unit,
    onDemoReport: () -> Unit = {},
    onDeepCheckSetup: () -> Unit,
    onOpenInfo: () -> Unit,
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
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
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
                LanguageToggle(
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                InfoCircleButton(
                    onClick = onOpenInfo,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
            Spacer(Modifier.height(12.dp))
            RadarScanButton(
                onClick = onScan,
                enabled = state.iocs != null && state.progress == null,
                lastRadar = state.lastRadar,
            )
            Spacer(Modifier.height(12.dp))
            val depth = when (state.access) {
                AccessLevel.APP -> stringResource(R.string.depth_basic)
                AccessLevel.SHELL -> stringResource(R.string.depth_deep)
                AccessLevel.ROOT -> stringResource(R.string.depth_root)
            }
            val sources = state.iocs?.status?.available ?: 0
            val depthLevel = when (state.access) {
                AccessLevel.APP -> 1
                AccessLevel.SHELL -> 2
                AccessLevel.ROOT -> 3
            }
            StatusStrip(listOf(
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
                    explanation = stringResource(if (state.filesGranted) R.string.status_files_on else R.string.status_files_off),
                    state = if (state.filesGranted) StatusState.ACTIVE else StatusState.OFF,
                    actionLabel = if (state.filesGranted) null else stringResource(R.string.start_allow),
                    action = if (state.filesGranted) null else onRequestFiles,
                ),
                StatusItem(
                    kind = StatusKind.SOURCES,
                    label = stringResource(R.string.status_sources_ratio, sources, 29),
                    explanation = stringResource(R.string.status_sources_explain, sources, 29),
                    state = if (sources == 29) StatusState.ACTIVE else StatusState.PARTIAL,
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
            ))
        }
        val scrolled by remember { derivedStateOf { scroll.value > 0 } }
        Spacer(Modifier.height(10.dp))
        ScrollEdge(scrolled)
        var staggerIndex = 0
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
            Text(
                stringResource(R.string.t_061),
                style = MaterialTheme.typography.bodyMedium,
                color = SpyColors.IvoryMuted,
                modifier = Modifier.then(staggeredAppear(staggerIndex++)),
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
                                maxLines = 2,
                                overflow = TextOverflow.Clip,
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
                                maxLines = 2,
                                overflow = TextOverflow.Clip,
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
                    val formattedCount = remember(animatedCount, Locale.getDefault()) {
                        NumberFormat.getNumberInstance(Locale.getDefault()).format(animatedCount)
                    }
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
                        maxLines = 2,
                        overflow = TextOverflow.Clip,
                    )
                    if (loaded.status.missing.isNotEmpty()) {
                        Text(
                            stringResource(R.string.sources_missing, loaded.status.missing.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SpyColors.IvoryMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
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
                DnsWatchCardBody(state = state, onDnsToggle = onDnsToggle)
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
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
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
                        Text(stringResource(R.string.t_072), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.t_073), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.t_074), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.t_075) + stringResource(R.string.t_076),
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
            Spacer(Modifier.height(4.dp))
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
private fun DnsWatchCardBody(state: UiState, onDnsToggle: (Boolean) -> Unit) {
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
            maxLines = 1,
            overflow = TextOverflow.Clip,
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
        maxLines = 2,
        overflow = TextOverflow.Clip,
    )
    if (state.dnsHits.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.t_066), style = MaterialTheme.typography.labelLarge)
        val fmt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        state.dnsHits.take(5).forEach { hit -> DnsHitLine(hit, fmt) }
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
            Text(title, style = MaterialTheme.typography.labelLarge, color = SpyColors.Ivory, maxLines = 1, overflow = TextOverflow.Clip)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = SpyColors.IvoryMuted,
                maxLines = 2,
                overflow = TextOverflow.Clip,
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
private fun DnsHitLine(hit: DnsHit, fmt: DateFormat) {
    Text(
        "${fmt.format(Date(hit.atMillis))} · ${hit.domain}",
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = SpyColors.Danger,
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
