package de.belkis.spyhound

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import de.belkis.spyhound.ioc.FeedCatalog
import de.belkis.spyhound.ui.info.infoLicenceEntries
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class InfoLicenceEntriesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun bundledLicenceNoticeCount() {
        val json = context.assets.open("iocs/sources.json").bufferedReader().use { it.readText() }
        val feeds = FeedCatalog.parse(json)
        val entries = infoLicenceEntries(context, feeds)
        assertEquals(12, entries.size)
    }
}
