package de.belkis.spyhound.collect

import android.app.AppOpsManager
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import de.belkis.spyhound.scan.HardeningInput
import java.time.LocalDate

/** Reads phone settings that any app may read. No extra permissions. */
class HardeningCollector(private val context: Context) {
    fun collect(): HardeningInput {
        val resolver = context.contentResolver
        fun flag(name: String) = runCatching { Settings.Global.getInt(resolver, name, 0) == 1 }.getOrDefault(false)
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return HardeningInput(
            screenLockSet = keyguard?.isDeviceSecure ?: false,
            usbDebuggingOn = flag(Settings.Global.ADB_ENABLED),
            wifiDebuggingOn = flag("adb_wifi_enabled"),
            developerOptionsOn = flag(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED),
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
