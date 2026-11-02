package de.belkis.spyhound

import de.belkis.spyhound.scan.AppRecord
import de.belkis.spyhound.scan.PermissionAudit
import de.belkis.spyhound.scan.RightGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionAuditTest {
    private fun app(pkg: String, label: String = pkg, system: Boolean = false, vararg perms: String, acc: Boolean = false, admin: Boolean = false) =
        AppRecord(pkg, label, isSystem = system, grantedPermissions = perms.toSet(), accessibility = acc, deviceAdmin = admin)

    private val cam = "android.permission.CAMERA"
    private val mic = "android.permission.RECORD_AUDIO"

    private val apps = listOf(
        app("b.app", "Beta", false, cam, mic),
        app("a.app", "alpha", false, cam),
        app("sys.cam", "Camera", true, cam),
        app("me", "Spyhound", false, cam),
        app("acc.app", "Helper", false, acc = true, admin = true),
        app("sms.app", "Msg", false, "android.permission.RECEIVE_SMS"),
    )

    private fun group(g: List<de.belkis.spyhound.scan.AuditGroup>, r: RightGroup) = g.first { it.group == r }

    @Test fun groupsHoldTheRightApps() {
        val g = PermissionAudit.evaluate(apps, "me")
        assertEquals(listOf("a.app", "b.app"), group(g, RightGroup.CAMERA).userApps.map { it.packageName })
        assertEquals(listOf("sys.cam"), group(g, RightGroup.CAMERA).systemApps.map { it.packageName })
        assertEquals(listOf("b.app"), group(g, RightGroup.MICROPHONE).apps.map { it.packageName })
        assertEquals(listOf("acc.app"), group(g, RightGroup.ACCESSIBILITY).apps.map { it.packageName })
        assertEquals(listOf("acc.app"), group(g, RightGroup.DEVICE_ADMIN).apps.map { it.packageName })
        assertEquals(listOf("sms.app"), group(g, RightGroup.SMS).apps.map { it.packageName })
        assertTrue(group(g, RightGroup.OVERLAY).apps.isEmpty())
        assertEquals(RightGroup.entries, g.map { it.group })
    }

    @Test fun firstCheckMarksNothingNew() {
        val g = PermissionAudit.evaluate(apps, "me", null)
        assertEquals(0, PermissionAudit.totalNew(g, true))
    }

    @Test fun marksOnlyRightsGrantedSinceLastCheck() {
        val before = PermissionAudit.snapshot(PermissionAudit.evaluate(apps.take(1), "me"), 1L)
        val g = PermissionAudit.evaluate(apps, "me", before)
        assertEquals(listOf("a.app", "sys.cam"), group(g, RightGroup.CAMERA).apps.filter { it.isNew }.map { it.packageName })
        assertFalse(group(g, RightGroup.MICROPHONE).apps.single().isNew)
        // camera: a.app + sys.cam, accessibility, admin, sms
        assertEquals(5, PermissionAudit.totalNew(g, true))
        assertEquals(4, PermissionAudit.totalNew(g, false))
    }

    @Test fun diffListsNewPairs() {
        val old = PermissionAudit.snapshot(PermissionAudit.evaluate(apps.take(2), "me"), 1L)
        val now = PermissionAudit.snapshot(PermissionAudit.evaluate(apps, "me"), 2L)
        val d = PermissionAudit.diff(old, now)
        assertEquals(setOf("sys.cam"), d[RightGroup.CAMERA])
        assertEquals(setOf("acc.app"), d[RightGroup.ACCESSIBILITY])
        assertFalse(d.containsKey(RightGroup.MICROPHONE))
    }

    @Test fun snapshotsSurviveEncoding() {
        val a = PermissionAudit.snapshot(PermissionAudit.evaluate(apps, "me"), 86_400_000L * 3)
        val b = PermissionAudit.snapshot(PermissionAudit.evaluate(apps.take(1), "me"), 5L)
        val back = PermissionAudit.decode(PermissionAudit.encode(listOf(a, b)))
        assertEquals(2, back.size)
        assertEquals(a.takenAt, back[0].takenAt)
        assertEquals(a.holders.filterValues { it.isNotEmpty() }, back[0].holders)
        assertEquals(b.holders.filterValues { it.isNotEmpty() }, back[1].holders)
        assertTrue(PermissionAudit.decode("garbage\nG\tcamera\tx").isEmpty())
    }

    @Test fun unchangedCheckReplacesLastAndHistoryIsCapped() {
        val s = PermissionAudit.snapshot(PermissionAudit.evaluate(apps, "me"), 1L)
        val same = s.copy(takenAt = 2L)
        assertEquals(listOf(2L), PermissionAudit.appendSnapshot(listOf(s), same).map { it.takenAt })
        val other = PermissionAudit.snapshot(PermissionAudit.evaluate(apps.take(1), "me"), 3L)
        assertEquals(2, PermissionAudit.appendSnapshot(listOf(s), other).size)
        var h = emptyList<de.belkis.spyhound.scan.AuditSnapshot>()
        repeat(40) { i -> h = PermissionAudit.appendSnapshot(h, other.copy(takenAt = i.toLong(), holders = mapOf(RightGroup.CAMERA to setOf("p$i")))) }
        assertEquals(PermissionAudit.MAX_SNAPSHOTS, h.size)
        assertEquals(39L, h.last().takenAt)
    }
}
