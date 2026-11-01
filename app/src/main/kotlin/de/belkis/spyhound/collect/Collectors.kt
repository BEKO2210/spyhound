package de.belkis.spyhound.collect

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.provider.Telephony
import de.belkis.spyhound.scan.ApkFile
import de.belkis.spyhound.scan.AppRecord
import de.belkis.spyhound.scan.DeviceState
import de.belkis.spyhound.scan.Message
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/** Reads what any app may read about the other apps: no special rights beyond QUERY_ALL_PACKAGES. */
class AppCollector(private val context: Context) {
    private val pm = context.packageManager

    @Suppress("DEPRECATION")
    fun collect(onProgress: (done: Int, total: Int, label: String) -> Unit): List<AppRecord> {
        val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNING_CERTIFICATES or
            PackageManager.MATCH_DISABLED_COMPONENTS or PackageManager.MATCH_UNINSTALLED_PACKAGES
        val packages = pm.getInstalledPackages(flags)
        val accessibility = enabledComponents(Settings.Secure.getString(context.contentResolver, "enabled_accessibility_services"))
        val listeners = enabledComponents(Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners"))
        val admins = runCatching {
            (context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager).activeAdmins
                ?.map { it.packageName }?.toSet()
        }.getOrNull().orEmpty()
        val launchable = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), PackageManager.MATCH_ALL,
        ).map { it.activityInfo.packageName }.toSet()

        return packages.mapIndexed { i, p ->
            val ai = p.applicationInfo
            val label = ai?.let { runCatching { pm.getApplicationLabel(it).toString() }.getOrNull() } ?: p.packageName
            onProgress(i, packages.size, label)
            val system = ai != null && (ai.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
            val apkPaths = listOfNotNull(ai?.sourceDir) + (ai?.splitSourceDirs?.toList() ?: emptyList())
            val (sha1, sha256) = certs(p)
            AppRecord(
                packageName = p.packageName,
                label = label,
                apks = apkPaths.map { ApkFile(it, sha256Of(File(it))) },
                certSha1 = sha1,
                certSha256 = sha256,
                installer = installer(p.packageName),
                isSystem = system,
                firstInstall = p.firstInstallTime,
                grantedPermissions = granted(p),
                accessibility = p.packageName in accessibility,
                notificationAccess = p.packageName in listeners,
                deviceAdmin = p.packageName in admins,
                hiddenFromLauncher = !system && p.packageName !in launchable,
            )
        }
    }

    private fun enabledComponents(raw: String?): Set<String> =
        raw.orEmpty().split(':').mapNotNull { it.substringBefore('/').takeIf(String::isNotBlank) }.toSet()

    private fun granted(p: PackageInfo): Set<String> {
        val perms = p.requestedPermissions ?: return emptySet()
        val flags = p.requestedPermissionsFlags ?: return emptySet()
        return perms.indices.filter { flags[it] and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0 }.map { perms[it] }.toSet()
    }

    private fun certs(p: PackageInfo): Pair<List<String>, List<String>> {
        val info = p.signingInfo ?: return emptyList<String>() to emptyList()
        val sigs = if (info.hasMultipleSigners()) info.apkContentsSigners else info.signingCertificateHistory
        val list = sigs?.map { it.toByteArray() }.orEmpty()
        return list.map { digest("SHA-1", it) } to list.map { digest("SHA-256", it) }
    }

    @Suppress("DEPRECATION")
    private fun installer(pkg: String): String? = runCatching {
        if (Build.VERSION.SDK_INT >= 30) pm.getInstallSourceInfo(pkg).installingPackageName else pm.getInstallerPackageName(pkg)
    }.getOrNull()

    companion object {
        fun digest(alg: String, bytes: ByteArray): String =
            MessageDigest.getInstance(alg).digest(bytes).joinToString("") { "%02x".format(it) }

        fun sha256Of(f: File): String? = runCatching {
            val md = MessageDigest.getInstance("SHA-256")
            FileInputStream(f).use { input ->
                val buf = ByteArray(1 shl 20)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    md.update(buf, 0, n)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        }.getOrNull()
    }
}

class DeviceCollector(private val context: Context) {
    fun collect(apps: List<AppRecord>): DeviceState {
        return DeviceState(
            props = getprop(),
            securityPatch = Build.VERSION.SECURITY_PATCH,
            sdk = Build.VERSION.SDK_INT,
            adbEnabled = global(Settings.Global.ADB_ENABLED),
            adbWifiEnabled = global("adb_wifi_enabled"),
            developerOptions = global(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED),
            installSources = installSources(apps),
            rootTraces = rootTraces(apps),
            selinux = runCatching { if (File("/sys/fs/selinux/enforce").readText().trim() == "0") "Permissive" else "Enforcing" }.getOrNull(),
        )
    }

    private fun global(key: String): Boolean? =
        runCatching { Settings.Global.getInt(context.contentResolver, key) == 1 }.getOrNull()

    /** `getprop` works for ordinary apps; SELinux hides some properties, Shizuku sees more. */
    fun getprop(): Map<String, String> = parseGetprop(runCatching {
        val p = ProcessBuilder("getprop").start()
        p.inputStream.bufferedReader().use { it.readText() }.also { p.waitFor() }
    }.getOrDefault(""))

    private fun installSources(apps: List<AppRecord>): List<String> {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return apps.filter { !it.isSystem && "android.permission.REQUEST_INSTALL_PACKAGES" in requested(it.packageName) }
            .filter { app ->
                val uid = runCatching { context.packageManager.getApplicationInfo(app.packageName, 0).uid }.getOrNull() ?: return@filter false
                runCatching {
                    ops.unsafeCheckOpNoThrow("android:request_install_packages", uid, app.packageName) == AppOpsManager.MODE_ALLOWED
                }.getOrDefault(false)
            }.map { it.label }
    }

    private fun requested(pkg: String): Set<String> = runCatching {
        context.packageManager.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS).requestedPermissions?.toSet()
    }.getOrNull().orEmpty()

    private fun rootTraces(apps: List<AppRecord>): List<String> {
        val files = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/app/Superuser.apk", "/data/local/xbin/su", "/data/local/bin/su")
            .filter { File(it).exists() }
        val rootApps = setOf("com.topjohnwu.magisk", "io.github.huskydg.magisk", "me.weishu.kernelsu", "me.bmax.apatch", "eu.chainfire.supersu", "com.noshufou.android.su")
        return files + apps.filter { it.packageName in rootApps }.map { it.label }
    }

    companion object {
        private val PROP = Regex("""^\[([^\]]+)]: \[(.*)]$""")
        fun parseGetprop(text: String): Map<String, String> =
            text.lineSequence().mapNotNull { PROP.find(it.trim())?.destructured?.let { (k, v) -> k to v } }.toMap()
    }
}

class SmsCollector(private val context: Context) {
    /** Null when the permission is missing, so the report shows a gap instead of "clean". */
    @SuppressLint("Recycle")
    fun collect(): List<Message>? {
        if (context.checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) return null
        val out = ArrayList<Message>()
        runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI, arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.DATE, Telephony.Sms.BODY),
                null, null, null,
            )?.use { c ->
                while (c.moveToNext()) out += Message(c.getString(0).orEmpty(), c.getLong(1), c.getString(2).orEmpty())
            }
        }.onFailure { return null }
        return out
    }
}

/** Shared storage (Downloads, Messenger media ...). Needs "All files access" to see other apps' files. */
class StorageCollector {
    data class Result(val listing: String, val hashes: String, val skippedLarge: Int, val unreadable: Int)

    fun canRead(): Boolean = Environment.isExternalStorageManager()

    fun collect(onProgress: (String) -> Unit): Result {
        val root = Environment.getExternalStorageDirectory()
        val listing = StringBuilder()
        val hashes = StringBuilder()
        var large = 0
        var unreadable = 0
        root.walkTopDown().onFail { _, _ -> unreadable++ }.forEach { f ->
            if (!f.isFile) return@forEach
            listing.append(f.absolutePath).append('\n')
            if (f.extension.lowercase() !in CANDIDATES && f.extension.isNotEmpty()) return@forEach
            if (f.length() > MAX_HASH_BYTES) { large++; return@forEach }
            onProgress(f.name)
            val sha = AppCollector.sha256Of(f)
            if (sha == null) unreadable++ else hashes.append(sha).append("  ").append(f.absolutePath).append('\n')
        }
        return Result(listing.toString(), hashes.toString(), large, unreadable)
    }

    companion object {
        /** Installers, executables and archives: what a spyware sample is shipped as. */
        val CANDIDATES = setOf("apk", "apks", "xapk", "apkm", "dex", "odex", "vdex", "jar", "so", "zip", "bin", "elf", "sh", "exe", "dll", "ipa")
        const val MAX_HASH_BYTES = 120L shl 20
    }
}
