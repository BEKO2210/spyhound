package de.belkis.spyhound.collect

import android.content.Context
import de.belkis.spyhound.scan.AuditSnapshot
import de.belkis.spyhound.scan.PermissionAudit
import java.io.File

/** Check history of the rights audit, kept in the app's private storage. Nothing is sent anywhere. */
class PermissionAuditStore(context: Context) {
    private val file = File(context.filesDir, "permission_audit.txt")

    fun load(): List<AuditSnapshot> =
        runCatching { if (file.exists()) PermissionAudit.decode(file.readText()) else emptyList() }.getOrDefault(emptyList())

    fun save(history: List<AuditSnapshot>) {
        runCatching {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(PermissionAudit.encode(history))
            if (!tmp.renameTo(file)) { file.writeText(tmp.readText()); tmp.delete() }
        }
    }
}
