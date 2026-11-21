package de.belkis.spyhound

import de.belkis.spyhound.ioc.AppRelease
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppReleaseTest {
    private fun release(tag: String, extra: String = "") =
        """{"url":"https://api.github.com/repos/BEKO2210/spyhound/releases/1","tag_name":"$tag","name":"Spyhound",
           "draft":false,"prerelease":false,"body":"notes"$extra}"""

    @Test fun readsTheVersionFromTheTag() {
        assertEquals("0.3.18", AppRelease.latestVersion(release("v0.3.18")))
        assertEquals("1.0", AppRelease.latestVersion(release("1.0")))
    }

    @Test fun anythingButAPlainFinalReleaseIsUnknown() {
        assertNull(AppRelease.latestVersion(release("v1.0.0-rc1")))
        assertNull(AppRelease.latestVersion(release("latest")))
        assertNull(AppRelease.latestVersion(release("v0.3.18").replace(""""prerelease":false""", """"prerelease": true""")))
        assertNull(AppRelease.latestVersion(release("v0.3.18").replace(""""draft":false""", """"draft":true""")))
        assertNull(AppRelease.latestVersion("""{"message":"API rate limit exceeded"}"""))
        assertNull(AppRelease.latestVersion("<html>captive portal</html>"))
        // A list of releases, or a body that smuggles a second tag, is not the answer to "latest".
        assertNull(AppRelease.latestVersion("[" + release("v0.3.18") + "," + release("v9.9.9") + "]"))
    }

    @Test fun comparesNumbersNotText() {
        assertTrue(AppRelease.isNewer("0.3.18", "0.3.17"))
        assertTrue(AppRelease.isNewer("0.3.100", "0.3.99"))
        assertTrue(AppRelease.isNewer("0.4", "0.3.17"))
        assertTrue(AppRelease.isNewer("1.0.0", "0.9.9"))
        assertTrue(AppRelease.isNewer("0.3.18", "0.3.17-debug"))
        assertFalse(AppRelease.isNewer("0.3.17", "0.3.17"))
        assertFalse(AppRelease.isNewer("0.3.17", "0.3.17.0"))
        assertFalse(AppRelease.isNewer("0.3.9", "0.3.17"))
        assertFalse(AppRelease.isNewer("0.3.16", "0.3.17"))
    }

    @Test fun unreadableVersionsNeverAnnounceAnUpdate() {
        assertFalse(AppRelease.isNewer("", "0.3.17"))
        assertFalse(AppRelease.isNewer("newest", "0.3.17"))
        assertFalse(AppRelease.isNewer("0.3.18", ""))
        assertFalse(AppRelease.isNewer("99999.0", "0.3.17"))
    }
}
