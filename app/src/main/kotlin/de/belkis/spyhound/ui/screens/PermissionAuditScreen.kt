package de.belkis.spyhound.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.belkis.spyhound.R
import de.belkis.spyhound.collect.AppCollector
import de.belkis.spyhound.collect.PermissionAuditStore
import de.belkis.spyhound.scan.AuditApp
import de.belkis.spyhound.scan.AuditGroup
import de.belkis.spyhound.scan.LookCloserApp
import de.belkis.spyhound.scan.LookRule
import de.belkis.spyhound.scan.PermissionAudit
import de.belkis.spyhound.scan.RightGroup
import de.belkis.spyhound.ui.components.CardRowIconBadge
import de.belkis.spyhound.ui.components.ScrollEdge
import de.belkis.spyhound.ui.components.SpyCard
import de.belkis.spyhound.ui.components.SpyNavigateBack
import de.belkis.spyhound.ui.components.TextActionLink
import de.belkis.spyhound.ui.design.SpyColors
import de.belkis.spyhound.ui.design.SpySpacing
import de.belkis.spyhound.ui.design.staggeredAppear
import de.belkis.spyhound.ui.SpyTypographyExtras
import de.belkis.spyhound.ui.formatCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

/** Result of one audit run: the groups plus when the previous check was taken (null on the first run). */
data class AuditResult(val groups: List<AuditGroup>, val previousAt: Long?)

/** Reads the apps, compares with the last stored check, stores this check, and shows the screen. */
@Composable
fun PermissionAuditRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var progress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val result by produceState<AuditResult?>(null, context) {
        value = withContext(Dispatchers.IO) { runAudit(context) { done, total -> progress = done to total } }
    }
    PermissionAuditScreen(
        result = result,
        progress = progress,
        onBack = onBack,
        onAppInfo = { openAppInfo(context, it) },
        modifier = modifier,
    )
}

private fun runAudit(context: Context, onProgress: (done: Int, total: Int) -> Unit): AuditResult {
    val store = PermissionAuditStore(context)
    val history = store.load()
    val previous = history.lastOrNull()
    // Rights and status only: no APK fingerprints, which the scan needs but this overview does not.
    val apps = AppCollector(context).collect(hashApks = false) { done, total, _ -> onProgress(done + 1, total) }
    val groups = PermissionAudit.evaluate(apps, context.packageName, previous)
    store.save(PermissionAudit.appendSnapshot(history, PermissionAudit.snapshot(groups, System.currentTimeMillis())))
    return AuditResult(groups, previous?.takenAt)
}

private fun openAppInfo(context: Context, packageName: String) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

@Composable
fun PermissionAuditScreen(
    result: AuditResult?,
    onBack: () -> Unit,
    onAppInfo: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialShowSystem: Boolean = false,
    progress: Pair<Int, Int>? = null,
) {
    val locale = LocalConfiguration.current.locales[0]
    val scroll = rememberScrollState()
    val scrolled by remember { derivedStateOf { scroll.value > 0 } }
    var showSystem by remember { mutableStateOf(initialShowSystem) }
    Column(modifier.fillMaxSize()) {
        SpyNavigateBack(onClick = onBack)
        Text(
            stringResource(R.string.audit_title),
            style = MaterialTheme.typography.headlineLarge,
            color = SpyColors.Ivory,
            modifier = Modifier.padding(bottom = SpySpacing.Unit * 2),
        )
        ScrollEdge(scrolled = scrolled)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(SpySpacing.Unit * 4),
        ) {
            if (result == null) {
                val loading = progress?.let { (done, total) ->
                    stringResource(R.string.audit_loading_progress, formatCount(done, locale), formatCount(total, locale))
                } ?: stringResource(R.string.audit_loading)
                Text(loading, style = MaterialTheme.typography.bodyMedium, color = SpyColors.IvoryMuted)
                return@Column
            }
            val newCount = PermissionAudit.totalNew(result.groups, showSystem)
            Column(Modifier.fillMaxWidth().then(staggeredAppear(0))) {
                Text(
                    stringResource(R.string.audit_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpyColors.IvoryMuted,
                    modifier = Modifier.padding(bottom = SpySpacing.Unit * 2),
                )
                val summary = when {
                    result.previousAt == null -> stringResource(R.string.audit_first)
                    newCount > 0 -> stringResource(R.string.audit_new_total, formatCount(newCount, locale))
                    else -> stringResource(R.string.audit_none_new)
                }
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (newCount > 0 && result.previousAt != null) SpyColors.Amber else SpyColors.IvoryMuted,
                )
                result.previousAt?.let {
                    val stamp = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(it))
                    Text(
                        stringResource(R.string.audit_since, stamp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SpyColors.IvoryMuted,
                    )
                }
                TextActionLink(
                    text = stringResource(if (showSystem) R.string.audit_system_hide else R.string.audit_system_show),
                    onClick = { showSystem = !showSystem },
                )
            }
            LookCloserSection(
                PermissionAudit.lookCloser(result.groups),
                onAppInfo,
                Modifier.then(staggeredAppear(1)),
            )
            Text(
                stringResource(R.string.audit_common_note),
                style = MaterialTheme.typography.bodyMedium,
                color = SpyColors.IvoryMuted,
            )
            result.groups.forEachIndexed { i, group ->
                GroupCard(group, showSystem, onAppInfo, Modifier.then(staggeredAppear(i + 2)))
            }
            Spacer(Modifier.height(SpySpacing.Unit * 4))
        }
    }
}

private fun LookRule.reasonRes() = when (this) {
    LookRule.ACCESSIBILITY_PLUS_DATA -> R.string.audit_look_accessibility_data
    LookRule.NOTIFICATION_ACCESS_PLUS_DATA -> R.string.audit_look_notification_data
    LookRule.DEVICE_ADMIN_PLUS_DATA -> R.string.audit_look_admin_data
    LookRule.BACKGROUND_LOCATION_PLUS_MICROPHONE -> R.string.audit_look_bg_mic
    LookRule.OVERLAY_PLUS_ACCESSIBILITY -> R.string.audit_look_overlay_accessibility
}

/** Left edge of the header text next to the 40 dp icon; rows, dividers and notes below line up with it. */
private val TextRail = 40.dp + 14.dp

/** Moves the title line down so its middle meets the middle of the 40 dp icon. */
private val TitleInset = 10.dp

/** Calm top section: apps whose mix of rights deserves a closer look (a hint, amber, never an alarm). */
@Composable
private fun LookCloserSection(flagged: List<LookCloserApp>, onAppInfo: (String) -> Unit, modifier: Modifier = Modifier) {
    SpyCard(modifier) {
        Row(verticalAlignment = Alignment.Top) {
            CardRowIconBadge(iconRes = R.drawable.ic_right_audit, active = false)
            Column(Modifier.weight(1f).padding(start = 14.dp, end = 8.dp, top = TitleInset)) {
                Text(stringResource(R.string.audit_look_title), style = MaterialTheme.typography.labelLarge, color = SpyColors.Ivory)
                Text(
                    stringResource(if (flagged.isEmpty()) R.string.audit_look_none else R.string.audit_look_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (flagged.isEmpty()) SpyColors.Clear else SpyColors.IvoryMuted,
                )
            }
        }
        flagged.forEach { f ->
            HorizontalDivider(Modifier.padding(start = TextRail), color = SpyColors.Border, thickness = 1.dp)
            AppRow(f.app, onAppInfo, reasons = f.rules.map { stringResource(it.reasonRes()) })
        }
    }
}

private fun RightGroup.iconRes() = when (this) {
    RightGroup.CAMERA -> R.drawable.ic_right_camera
    RightGroup.MICROPHONE -> R.drawable.ic_right_microphone
    RightGroup.FINE_LOCATION -> R.drawable.ic_right_fine_location
    RightGroup.BACKGROUND_LOCATION -> R.drawable.ic_right_background_location
    RightGroup.SMS -> R.drawable.ic_right_sms
    RightGroup.CALL_LOG -> R.drawable.ic_right_call_log
    RightGroup.CONTACTS -> R.drawable.ic_right_contacts
    RightGroup.ACCESSIBILITY -> R.drawable.ic_right_accessibility
    RightGroup.NOTIFICATION_ACCESS -> R.drawable.ic_right_notification_access
    RightGroup.DEVICE_ADMIN -> R.drawable.ic_right_device_admin
    RightGroup.OVERLAY -> R.drawable.ic_right_overlay
}

private fun RightGroup.titleRes() = when (this) {
    RightGroup.CAMERA -> R.string.audit_g_camera
    RightGroup.MICROPHONE -> R.string.audit_g_microphone
    RightGroup.FINE_LOCATION -> R.string.audit_g_fine_location
    RightGroup.BACKGROUND_LOCATION -> R.string.audit_g_background_location
    RightGroup.SMS -> R.string.audit_g_sms
    RightGroup.CALL_LOG -> R.string.audit_g_call_log
    RightGroup.CONTACTS -> R.string.audit_g_contacts
    RightGroup.ACCESSIBILITY -> R.string.audit_g_accessibility
    RightGroup.NOTIFICATION_ACCESS -> R.string.audit_g_notification_access
    RightGroup.DEVICE_ADMIN -> R.string.audit_g_device_admin
    RightGroup.OVERLAY -> R.string.audit_g_overlay
}

private fun RightGroup.bodyRes() = when (this) {
    RightGroup.CAMERA -> R.string.audit_g_camera_d
    RightGroup.MICROPHONE -> R.string.audit_g_microphone_d
    RightGroup.FINE_LOCATION -> R.string.audit_g_fine_location_d
    RightGroup.BACKGROUND_LOCATION -> R.string.audit_g_background_location_d
    RightGroup.SMS -> R.string.audit_g_sms_d
    RightGroup.CALL_LOG -> R.string.audit_g_call_log_d
    RightGroup.CONTACTS -> R.string.audit_g_contacts_d
    RightGroup.ACCESSIBILITY -> R.string.audit_g_accessibility_d
    RightGroup.NOTIFICATION_ACCESS -> R.string.audit_g_notification_access_d
    RightGroup.DEVICE_ADMIN -> R.string.audit_g_device_admin_d
    RightGroup.OVERLAY -> R.string.audit_g_overlay_d
}

@Composable
private fun GroupCard(group: AuditGroup, showSystem: Boolean, onAppInfo: (String) -> Unit, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val visible = if (showSystem) group.apps else group.userApps
    val users = group.userApps
    val system = if (showSystem) group.systemApps else emptyList()
    val newCount = visible.count { it.isNew }
    SpyCard(modifier) {
        Row(verticalAlignment = Alignment.Top) {
            CardRowIconBadge(iconRes = group.group.iconRes(), active = false)
            Column(Modifier.weight(1f).padding(start = 14.dp, end = 8.dp, top = TitleInset)) {
                Text(stringResource(group.group.titleRes()), style = MaterialTheme.typography.labelLarge, color = SpyColors.Ivory)
                Text(stringResource(group.group.bodyRes()), style = MaterialTheme.typography.bodyMedium, color = SpyColors.IvoryMuted)
            }
            Column(Modifier.padding(top = TitleInset), horizontalAlignment = Alignment.End) {
                Text(
                    if (visible.size == 1) stringResource(R.string.audit_count_one)
                    else stringResource(R.string.audit_count, formatCount(visible.size, locale)),
                    style = MaterialTheme.typography.labelLarge,
                    color = SpyColors.Ivory,
                )
                if (newCount > 0) {
                    Text(
                        stringResource(R.string.audit_new_count, formatCount(newCount, locale)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SpyColors.Amber,
                    )
                }
            }
        }
        if (visible.isEmpty()) {
            Text(
                stringResource(R.string.audit_none),
                style = MaterialTheme.typography.bodyMedium,
                color = SpyColors.Clear,
                modifier = Modifier.padding(start = TextRail),
            )
        }
        users.forEach { app ->
            HorizontalDivider(Modifier.padding(start = TextRail), color = SpyColors.Border, thickness = 1.dp)
            AppRow(app, onAppInfo)
        }
        if (system.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(start = TextRail), color = SpyColors.Border, thickness = 1.dp)
            Text(
                stringResource(R.string.audit_system_label).uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                color = SpyColors.IvoryMuted,
                modifier = Modifier.padding(start = TextRail),
            )
            system.forEach { app ->
                HorizontalDivider(Modifier.padding(start = TextRail), color = SpyColors.Border, thickness = 1.dp)
                AppRow(app, onAppInfo)
            }
        }
    }
}

@Composable
private fun AppRow(app: AuditApp, onAppInfo: (String) -> Unit, reasons: List<String> = emptyList()) {
    val infoLabel = stringResource(R.string.audit_app_info)
    val infoDesc = stringResource(R.string.audit_app_info_for, app.label)
    val newDesc = stringResource(R.string.audit_new_desc, app.label)
    // Name, badge and "App info" share the first line; everything below hangs from it.
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = TextRail)
            .defaultMinSize(minHeight = 48.dp)
            .semantics { if (app.isNew) contentDescription = newDesc },
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f).widthIn(min = 0.dp).padding(end = 8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    app.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = SpyColors.Ivory,
                    maxLines = 2,
                    modifier = Modifier.weight(1f, fill = false).alignByBaseline(),
                )
                if (app.isNew) NewBadge(Modifier.alignByBaseline())
            }
            Text(
                app.packageName,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = SpyColors.IvoryMuted,
                maxLines = 2,
            )
            reasons.forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = SpyColors.Amber)
            }
        }
        Box(
            Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onAppInfo(app.packageName) },
                )
                .semantics { contentDescription = infoDesc },
            contentAlignment = Alignment.TopEnd,
        ) {
            Text(infoLabel, style = MaterialTheme.typography.labelLarge, color = SpyColors.Amber, maxLines = 1)
        }
    }
}

@Composable
private fun NewBadge(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Box(modifier.border(1.dp, SpyColors.Amber, shape).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(
            stringResource(R.string.audit_new_badge),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
            color = SpyColors.Amber,
            maxLines = 1,
        )
    }
}

/** Start-screen card that opens the audit. */
@Composable
fun PermissionAuditEntryCard(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val title = stringResource(R.string.audit_entry_title)
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
            CardRowIconBadge(iconRes = R.drawable.ic_right_audit, active = false)
            Column(Modifier.weight(1f).padding(start = 14.dp, end = 8.dp, top = TitleInset)) {
                Text(title.uppercase(), style = SpyTypographyExtras.sectionLabel)
                Spacer(Modifier.height(SpySpacing.Unit))
                Text(stringResource(R.string.audit_entry_body), style = MaterialTheme.typography.bodyMedium, color = SpyColors.IvoryMuted)
            }
            Text(
                stringResource(R.string.audit_entry_open),
                style = MaterialTheme.typography.labelLarge,
                color = SpyColors.Amber,
                maxLines = 1,
                modifier = Modifier.padding(top = TitleInset),
            )
        }
    }
}
