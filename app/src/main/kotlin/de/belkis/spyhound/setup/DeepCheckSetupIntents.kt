package de.belkis.spyhound.setup

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings

fun isShizukuInstalled(pm: PackageManager): Boolean =
    runCatching { pm.getPackageInfo(SHIZUKU_PACKAGE, 0); true }.getOrDefault(false)

fun isDeveloperOptionsEnabled(context: Context): Boolean =
    Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1

fun openShizukuDownload(context: Context) {
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$SHIZUKU_PACKAGE"))
    val launch = market.resolveActivity(context.packageManager)
    context.startActivity(
        if (launch != null) market
        else Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")),
    )
}

fun openShizukuApp(context: Context) {
    context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)?.let { context.startActivity(it) }
}

fun openDeviceInfoSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))
}

fun openDeveloperSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
}
