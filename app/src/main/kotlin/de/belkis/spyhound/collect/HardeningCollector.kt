package de.belkis.spyhound.collect

import android.app.AppOpsManager
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import de.belkis.spyhound.scan.HardeningInput
import de.belkis.spyhound.shell.Shell
import java.time.LocalDate

/**
 * Reads phone settings that any app may read. No extra permissions. [shell] (deep check) sees the real debug switches;
 * [wifiDebuggingSeen] is true when this phone's own wireless-debugging service showed up through mDNS.
 */
class HardeningCollector(private val context: Context) {
    fun collect(shell: Shell? = null, wifiDebuggingSeen: Boolean = false): HardeningInput {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val usb = DebugSwitches.resolve(DebugSwitches.read(context, Settings.Global.ADB_ENABLED),
            DebugSwitches.fromShell(shell, Settings.Global.ADB_ENABLED))
        val wifi = DebugSwitches.resolve(DebugSwitches.read(context, "adb_wifi_enabled"),
            DebugSwitches.fromShell(shell, "adb_wifi_enabled") ?: true.takeIf { wifiDebuggingSeen })
        val dev = DebugSwitches.resolve(DebugSwitches.read(context, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED),
            DebugSwitches.fromShell(shell, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED) ?: true.takeIf { usb == true || wifi == true })
        return HardeningInput(
            screenLockSet = keyguard?.isDeviceSecure ?: false,
            usbDebuggingOn = usb,
            wifiDebuggingOn = wifi,
            developerOptionsOn = dev,
            securityPatch = runCatching { LocalDate.parse(Build.VERSION.SECURITY_PATCH) }.getOrNull(),
            unknownInstallerApps = installerApps(),
        )
    }

    @Suppress("DEPRECATION")
    private fun installerApps(): List<String> {
        val pm = context.packageManager
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return emptyList()
        return runCatching {
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS).filter { p ->
                val ai = p.applicationInfo ?: return@filter false
                val system = (ai.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
                !system && p.requestedPermissions?.contains("android.permission.REQUEST_INSTALL_PACKAGES") == true &&
                    runCatching {
                        ops.unsafeCheckOpNoThrow("android:request_install_packages", ai.uid, p.packageName) ==
                            AppOpsManager.MODE_ALLOWED
                    }.getOrDefault(false)
            }.map { p -> p.applicationInfo?.let { pm.getApplicationLabel(it).toString() } ?: p.packageName }
        }.getOrDefault(emptyList())
    }
}
