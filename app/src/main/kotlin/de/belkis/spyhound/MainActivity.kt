package de.belkis.spyhound

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.belkis.spyhound.BuildConfig
import de.belkis.spyhound.dns.DnsGuard
import de.belkis.spyhound.scan.GapFix
import de.belkis.spyhound.scan.ReportText
import de.belkis.spyhound.ui.MainViewModel
import de.belkis.spyhound.ui.SpyhoundScaffold
import de.belkis.spyhound.ui.SpyhoundTheme
import de.belkis.spyhound.ui.UiState
import de.belkis.spyhound.ui.screens.ResultScreen
import de.belkis.spyhound.ui.screens.ScanScreen
import de.belkis.spyhound.ui.screens.StartScreen

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    private val smsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.refreshPermissions()
    }

    private val notificationsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.refreshPermissions()
    }

    private val vpnPrepare = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) DnsGuard.start(this)
        vm.refreshDnsGuard()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            SpyhoundTheme {
                val state by vm.state.collectAsStateWithLifecycle()
                LaunchedEffect(Unit) { vm.refreshDnsGuard() }
                SpyhoundScaffold {
                    Box(Modifier.fillMaxSize()) {
                        SpyhoundNav(state)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshPermissions()
    }

    @Composable
    private fun SpyhoundNav(state: UiState) {
        when {
            state.report != null -> ResultScreen(
                report = state.report,
                onShare = { shareReport(state.report) },
                onRescan = {
                    vm.closeReport()
                    vm.startScan()
                },
                onGapFix = { fix -> handleGapFix(fix) },
                onHelp = {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.accessnow.org/help/")))
                },
            )
            state.progress != null -> ScanScreen(
                progress = state.progress,
                onCancel = { vm.cancelScan() },
            )
            else -> StartScreen(
                state = state,
                onScan = { vm.startScan() },
                onUpdateIndicators = { vm.updateIndicators() },
                onConnectShizuku = { vm.connectShizuku() },
                onRequestRoot = { vm.requestRoot() },
                onRequestSms = {
                    smsPermission.launch(Manifest.permission.READ_SMS)
                },
                onRequestFiles = { openAllFilesAccess() },
                onDnsToggle = { on -> toggleDns(on) },
                onDemoReport = {
                    if (BuildConfig.DEBUG) vm.showDemoReport()
                },
            )
        }
    }

    private fun toggleDns(enable: Boolean) {
        if (enable) {
            val prep = android.net.VpnService.prepare(this)
            if (prep != null) vpnPrepare.launch(prep) else DnsGuard.start(this)
        } else {
            DnsGuard.stop(this)
        }
        vm.refreshDnsGuard()
    }

    private fun openAllFilesAccess() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private fun shareReport(report: de.belkis.spyhound.scan.Report) {
        val text = ReportText.render(report, BuildConfig.VERSION_NAME)
        startActivity(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Spyhound Prüfbericht")
                putExtra(Intent.EXTRA_TEXT, text)
            },
        )
    }

    private fun handleGapFix(fix: GapFix) {
        when (fix) {
            GapFix.PERMISSION_SMS -> smsPermission.launch(Manifest.permission.READ_SMS)
            GapFix.PERMISSION_FILES -> openAllFilesAccess()
            GapFix.SHIZUKU -> vm.connectShizuku()
            GapFix.ROOT -> vm.requestRoot()
            GapFix.UPDATE_INDICATORS -> vm.updateIndicators()
            GapFix.NONE -> Unit
        }
    }
}
