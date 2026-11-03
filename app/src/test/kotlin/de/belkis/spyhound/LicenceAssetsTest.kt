package de.belkis.spyhound

import de.belkis.spyhound.ui.info.licenceTexts
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LicenceAssetsTest {
    private val assets = File("src/main/assets")

    @Test
    fun licenceAndNoticeMatchTheRepositoryRoot() {
        // The app shows these word for word; a NOTICE edit without copying it here would show an outdated text.
        assertEquals(File("../LICENSE").readText(), File(assets, "licenses/LICENSE.txt").readText())
        assertEquals(File("../NOTICE").readText(), File(assets, "licenses/NOTICE.txt").readText())
    }

    @Test
    fun everyShownLicenceExistsAndEveryLicenceFileIsShown() {
        val shown = licenceTexts.map { it.asset }.toSet()
        shown.forEach { assertTrue(File(assets, it).length() > 0, it) }
        val shipped = File(assets, "licenses").list()!!.map { "licenses/$it" }.toSet()
        assertEquals(shipped, shown)
    }
}
