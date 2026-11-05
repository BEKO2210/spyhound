package de.belkis.spyhound.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import de.belkis.spyhound.AppLanguage
import de.belkis.spyhound.MainActivity
import de.belkis.spyhound.R

object BackgroundScanNotifier {
    const val CHANNEL_ID = "spyhound_background_scan"
    private const val NOTIFICATION_ID = 1201

    fun notifyNewFindings(context: Context, count: Int) {
        val localized = AppLanguage.localizedContext(context)
        ensureChannel(localized)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_OPEN_BACKGROUND_RESULT
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_status_notifications)
            .setContentTitle(localized.getString(R.string.bg_scan_notification_title))
            .setContentText(localized.resources.getQuantityString(R.plurals.bg_scan_notification_text, count, count))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    localized.resources.getQuantityString(R.plurals.bg_scan_notification_text, count, count),
                ),
            )
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.bg_scan_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.bg_scan_channel_description)
        }
        manager.createNotificationChannel(channel)
    }
}
