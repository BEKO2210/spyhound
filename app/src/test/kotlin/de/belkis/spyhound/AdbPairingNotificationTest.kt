package de.belkis.spyhound

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import de.belkis.spyhound.shell.AdbPairingNotification
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Robolectric's ShadowNotificationManager keeps only the latest notification per id and has no post counter.
 * A repost builds a new [Notification] object, so we count posts by object identity: the same instance still
 * in the manager means "not reposted", a different instance means "posted again".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AdbPairingNotificationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val nm = context.getSystemService(NotificationManager::class.java)
    private val shadow = shadowOf(nm)
    private val id = 4712 // AdbPairingNotification.ID is private

    @Before
    fun setUp() {
        shadowOf(context as android.app.Application).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        nm.cancelAll()
        AdbPairingNotification.cancel(context) // the object keeps state between tests
        assertNull(shadow.getNotification(id))
    }

    private fun posted(): Notification = assertNotNull(shadow.getNotification(id), "notification should be posted")

    private fun text(n: Notification) = n.extras.getCharSequence(Notification.EXTRA_TEXT).toString()

    @Test
    fun sameArgsWithOnlyIfChangedPostsOnce() {
        AdbPairingNotification.show(context, onlyIfChanged = true)
        val first = posted()
        AdbPairingNotification.show(context, onlyIfChanged = true)
        assertSame(first, posted())
    }

    @Test
    fun changedTextPostsAgain() {
        AdbPairingNotification.show(context, onlyIfChanged = true)
        val first = posted()
        AdbPairingNotification.show(context, wrongCode = true, onlyIfChanged = true)
        val second = posted()
        assertNotSame(first, second)
        assertEquals(false, text(first) == text(second))
    }

    @Test
    fun swipedAwayPostsAgain() {
        AdbPairingNotification.show(context, onlyIfChanged = true)
        val first = posted()
        nm.cancel(id) // the user swipes it away; AdbPairingNotification.cancel is not involved
        assertNull(shadow.getNotification(id))
        AdbPairingNotification.show(context, onlyIfChanged = true)
        assertNotSame(first, posted())
    }

    @Test
    fun cancelThenShowPostsAgain() {
        AdbPairingNotification.show(context, onlyIfChanged = true)
        val first = posted()
        AdbPairingNotification.cancel(context)
        assertNull(shadow.getNotification(id))
        AdbPairingNotification.show(context, onlyIfChanged = true)
        assertNotSame(first, posted())
    }

    @Test
    fun withoutOnlyIfChangedAlwaysPosts() {
        AdbPairingNotification.show(context)
        val first = posted()
        AdbPairingNotification.show(context)
        assertNotSame(first, posted())
    }
}
