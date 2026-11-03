package de.belkis.spyhound.scan

/** The rights a person can reason about, in the order the screen shows them. */
enum class RightGroup(val key: String) {
    CAMERA("camera"),
    MICROPHONE("microphone"),
    FINE_LOCATION("fine_location"),
    BACKGROUND_LOCATION("background_location"),
    SMS("sms"),
    CALL_LOG("call_log"),
    CONTACTS("contacts"),
    ACCESSIBILITY("accessibility"),
    NOTIFICATION_ACCESS("notification_access"),
    DEVICE_ADMIN("device_admin"),
    OVERLAY("overlay");

    fun grantedTo(app: AppRecord): Boolean = when (this) {
        CAMERA -> "android.permission.CAMERA" in app.grantedPermissions
        MICROPHONE -> "android.permission.RECORD_AUDIO" in app.grantedPermissions
        FINE_LOCATION -> "android.permission.ACCESS_FINE_LOCATION" in app.grantedPermissions
        BACKGROUND_LOCATION -> "android.permission.ACCESS_BACKGROUND_LOCATION" in app.grantedPermissions
        SMS -> "android.permission.READ_SMS" in app.grantedPermissions ||
            "android.permission.RECEIVE_SMS" in app.grantedPermissions
        CALL_LOG -> "android.permission.READ_CALL_LOG" in app.grantedPermissions
        CONTACTS -> "android.permission.READ_CONTACTS" in app.grantedPermissions
        ACCESSIBILITY -> app.accessibility
        NOTIFICATION_ACCESS -> app.notificationAccess
        DEVICE_ADMIN -> app.deviceAdmin
        OVERLAY -> "android.permission.SYSTEM_ALERT_WINDOW" in app.grantedPermissions
    }

    companion object {
        fun fromKey(key: String): RightGroup? = entries.firstOrNull { it.key == key }
    }
}

data class AuditApp(val packageName: String, val label: String, val isSystem: Boolean, val isNew: Boolean = false)

data class AuditGroup(val group: RightGroup, val apps: List<AuditApp>) {
    val userApps: List<AuditApp> get() = apps.filter { !it.isSystem }
    val systemApps: List<AuditApp> get() = apps.filter { it.isSystem }
    val newCount: Int get() = apps.count { it.isNew }
}

/** What each right group was held by at one check: group -> package names. */
data class AuditSnapshot(val takenAt: Long, val holders: Map<RightGroup, Set<String>>)

/**
 * One reviewable rule for "worth a look": an app is hinted when it holds at least one right from [first] AND at least
 * one from [second]. A hint is never a verdict: many legitimate apps (a password manager, a launcher) do the same.
 */
enum class LookRule(val first: Set<RightGroup>, val second: Set<RightGroup>) {
    ACCESSIBILITY_PLUS_DATA(setOf(RightGroup.ACCESSIBILITY), PRIVATE_DATA),
    NOTIFICATION_ACCESS_PLUS_DATA(setOf(RightGroup.NOTIFICATION_ACCESS), PRIVATE_DATA),
    DEVICE_ADMIN_PLUS_DATA(setOf(RightGroup.DEVICE_ADMIN), PRIVATE_DATA),
    BACKGROUND_LOCATION_PLUS_MICROPHONE(setOf(RightGroup.BACKGROUND_LOCATION), setOf(RightGroup.MICROPHONE)),
    OVERLAY_PLUS_ACCESSIBILITY(setOf(RightGroup.OVERLAY), setOf(RightGroup.ACCESSIBILITY));

    fun matches(held: Set<RightGroup>) = held.any { it in first } && held.any { it in second }
}

// Rights that expose messages, calls, whereabouts or sound: the data stalkerware is after.
private val PRIVATE_DATA = setOf(
    RightGroup.SMS, RightGroup.CALL_LOG, RightGroup.FINE_LOCATION, RightGroup.BACKGROUND_LOCATION, RightGroup.MICROPHONE,
)

/** A user-installed app whose rights match at least one [LookRule]. */
data class LookCloserApp(val app: AuditApp, val rules: List<LookRule>)

object PermissionAudit {
    /**
     * User-installed apps whose combination of rights matches a [LookRule], sorted by name. System apps are never
     * hinted. Camera+microphone+contacts alone (a messenger) matches nothing.
     */
    fun lookCloser(groups: List<AuditGroup>): List<LookCloserApp> {
        val held = mutableMapOf<String, MutableSet<RightGroup>>()
        val apps = mutableMapOf<String, AuditApp>()
        groups.forEach { g ->
            g.userApps.forEach { a -> apps[a.packageName] = a; held.getOrPut(a.packageName) { mutableSetOf() } += g.group }
        }
        return apps.values
            .mapNotNull { a ->
                val rules = LookRule.entries.filter { it.matches(held.getValue(a.packageName)) }
                if (rules.isEmpty()) null else LookCloserApp(a, rules)
            }
            .sortedWith(compareBy({ it.app.label.lowercase() }, { it.app.packageName }))
    }

    /** Which apps hold which right. Own app is skipped; apps are sorted by name, groups keep their fixed order. */
    fun evaluate(apps: List<AppRecord>, ownPackage: String, previous: AuditSnapshot? = null): List<AuditGroup> {
        val candidates = apps.filter { it.packageName != ownPackage }
        return RightGroup.entries.map { group ->
            val before = previous?.holders?.get(group)
            val holders = candidates.filter { group.grantedTo(it) }
                .sortedWith(compareBy({ it.label.lowercase() }, { it.packageName }))
                .map { app ->
                    AuditApp(
                        app.packageName, app.label, app.isSystem,
                        // Without an earlier check nothing can be "new".
                        isNew = previous != null && app.packageName !in before.orEmpty(),
                    )
                }
            AuditGroup(group, holders)
        }
    }

    fun snapshot(groups: List<AuditGroup>, takenAt: Long) =
        AuditSnapshot(takenAt, groups.associate { g -> g.group to g.apps.map { it.packageName }.toSet() })

    fun totalNew(groups: List<AuditGroup>, includeSystem: Boolean): Int =
        groups.sumOf { g -> g.apps.count { it.isNew && (includeSystem || !it.isSystem) } }

    /** Newly granted (group, package) pairs between two snapshots. */
    fun diff(previous: AuditSnapshot, current: AuditSnapshot): Map<RightGroup, Set<String>> =
        current.holders.mapValues { (g, now) -> now - previous.holders[g].orEmpty() }.filterValues { it.isNotEmpty() }

    // Plain line format: no JSON library needed, nothing leaves the app.
    fun encode(snapshots: List<AuditSnapshot>): String = buildString {
        snapshots.forEach { s ->
            append("S\t").append(s.takenAt).append('\n')
            s.holders.forEach { (g, pkgs) ->
                if (pkgs.isNotEmpty()) append("G\t").append(g.key).append('\t').append(pkgs.sorted().joinToString(",")).append('\n')
            }
        }
    }

    fun decode(text: String): List<AuditSnapshot> {
        val out = mutableListOf<AuditSnapshot>()
        var time: Long? = null
        var holders = mutableMapOf<RightGroup, Set<String>>()
        fun flush() { time?.let { out += AuditSnapshot(it, holders) }; holders = mutableMapOf() }
        for (line in text.lineSequence()) {
            val p = line.split('\t')
            when (p[0]) {
                "S" -> { flush(); time = p.getOrNull(1)?.toLongOrNull() }
                "G" -> {
                    val g = p.getOrNull(1)?.let(RightGroup::fromKey)
                    if (g != null && time != null) holders[g] = p.getOrNull(2).orEmpty().split(',').filter { it.isNotBlank() }.toSet()
                }
            }
        }
        flush()
        return out
    }

    const val MAX_SNAPSHOTS = 30

    /** Appends a check; an unchanged check only refreshes the time of the last one so history stays short. */
    fun appendSnapshot(history: List<AuditSnapshot>, next: AuditSnapshot): List<AuditSnapshot> {
        val last = history.lastOrNull()
        val merged = if (last != null && last.holders.filterValues { it.isNotEmpty() } == next.holders.filterValues { it.isNotEmpty() }) {
            history.dropLast(1) + next
        } else {
            history + next
        }
        return merged.takeLast(MAX_SNAPSHOTS)
    }
}
