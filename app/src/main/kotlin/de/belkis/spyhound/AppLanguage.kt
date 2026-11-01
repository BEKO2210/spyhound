package de.belkis.spyhound

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** Empty language tag follows the device language. */
object AppLanguage {
    fun selected(context: Context): String = if (Build.VERSION.SDK_INT >= 33) {
        context.getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags()
    } else {
        AppCompatDelegate.getApplicationLocales().toLanguageTags()
    }

    fun localizedContext(context: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return context
        val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (tag.isEmpty()) return context
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocales(LocaleList.forLanguageTags(tag))
        return context.createConfigurationContext(configuration)
    }

    fun select(context: Context, languageTag: String) {
        if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(languageTag)
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
        }
    }
}
