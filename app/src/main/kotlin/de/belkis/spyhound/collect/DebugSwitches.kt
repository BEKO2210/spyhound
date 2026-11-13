package de.belkis.spyhound.collect

import android.content.Context
import android.os.Build
import android.provider.Settings
import de.belkis.spyhound.shell.Shell

/**
 * USB debugging, wireless debugging and developer options as far as they can be known. Android 17 reports all three
 * as 0 to apps even while they are on (Pixel 11 Pro XL, CP4A.261105), so there a 0 means "unknown", never "off".
 * Null is unknown; the UI must then not show a check mark.
 */
object DebugSwitches {
    const val UNRELIABLE_FROM_SDK = 37

    fun read(context: Context, key: String, sdk: Int = Build.VERSION.SDK_INT): Boolean? =
        interpret(runCatching { Settings.Global.getInt(context.contentResolver, key) }.getOrNull(), sdk)

    fun interpret(value: Int?, sdk: Int): Boolean? = when {
        value == null -> null
        value == 1 -> true
        sdk >= UNRELIABLE_FROM_SDK -> null
        else -> false
    }

    /** The real value through the deep-check shell, which Android does not mislead. "null" means never set, so off. */
    fun fromShell(shell: Shell?, key: String): Boolean? =
        when (shell?.read("settings get global $key", 4096, 10)?.trim()) {
            "1" -> true
            "0", "null" -> false
            else -> null
        }

    /** A proven value wins over what the setting claims; any debugging switch that is on implies developer options. */
    fun resolve(read: Boolean?, proven: Boolean?): Boolean? = proven ?: read
}
