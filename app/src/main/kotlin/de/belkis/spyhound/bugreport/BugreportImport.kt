package de.belkis.spyhound.bugreport

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns

object BugreportImport {
    fun acceptsIntent(context: Context, intent: Intent): Boolean {
        val action = intent.action ?: return false
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_VIEW) return false
        val uri = streamUri(intent) ?: intent.data ?: return false
        return acceptsUri(context, uri, intent.type)
    }

    fun streamUri(intent: Intent): Uri? {
        if (intent.action != Intent.ACTION_SEND) return null
        return if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }

    fun acceptsUri(context: Context, uri: Uri, hintType: String? = null): Boolean {
        val type = hintType?.substringBefore(';')?.trim()?.lowercase()
            ?: context.contentResolver.getType(uri)?.substringBefore(';')?.trim()?.lowercase()
        if (type == "application/zip") return true
        if (type == "application/octet-stream" || type.isNullOrBlank()) {
            return looksLikeBugreportZip(displayName(context, uri))
        }
        return false
    }

    private fun looksLikeBugreportZip(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val base = name.substringAfterLast('/').lowercase()
        return base.startsWith("bugreport") && base.endsWith(".zip")
    }

    fun displayName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) return c.getString(0)
            }
        }
        return uri.lastPathSegment
    }
}
