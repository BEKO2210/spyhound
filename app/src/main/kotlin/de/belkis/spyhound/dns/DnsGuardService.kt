package de.belkis.spyhound.dns

import de.belkis.spyhound.R
import de.belkis.spyhound.AppLanguage
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.Indicator
import de.belkis.spyhound.ioc.IocSet
import de.belkis.spyhound.ioc.IocStore
import kotlinx.coroutines.runBlocking
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Local VPN that only captures DNS to the virtual resolvers and forwards queries to the device’s
 * normal DNS servers via [protect]. No traffic is blocked unless [BLOCK_ON_HIT] is enabled later.
 */
class DnsGuardService : VpnService() {
    private var tun: ParcelFileDescriptor? = null
    private val stopFlag = AtomicBoolean(false)
    private var worker: Thread? = null

    /** One slow resolver must not hold up every other lookup on the phone. */
    private var lookups: ExecutorService? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == DnsGuard.ACTION_STOP) {
            shutdown()
            return START_NOT_STICKY
        }
        if (intent?.action != DnsGuard.ACTION_START && !DnsGuard.isRunning) {
            stopSelf()
            return START_NOT_STICKY
        }
        ensureChannels()
        startForeground(NOTIF_SERVICE, buildServiceNotification())
        if (worker?.isAlive == true) return START_STICKY
        stopFlag.set(false)
        worker = Thread(::runTunnel, "dns-guard-tun").also { it.start() }
        return START_STICKY
    }

    override fun onDestroy() {
        closeTunnel()
        super.onDestroy()
    }

    /** Closing the tunnel ends the VPN for the system and unblocks the worker's read. */
    private fun shutdown() {
        closeTunnel()
        stopSelf()
    }

    private fun closeTunnel() {
        stopFlag.set(true)
        lookups?.shutdownNow()
        lookups = null
        worker?.interrupt()
        worker = null
        runCatching { tun?.close() }
        tun = null
        DnsGuard.isRunning = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun runTunnel() {
        val iocs = runBlocking { IocStore(this@DnsGuardService).load().set }
        val iface = openTunnel() ?: run {
            stopSelf()
            return
        }
        if (stopFlag.get()) {
            // Stopped while the tunnel was being opened.
            runCatching { iface.close() }
            return
        }
        tun = iface
        DnsGuard.isRunning = true
        val pool = Executors.newFixedThreadPool(LOOKUP_THREADS).also { lookups = it }
        val buf = ByteArray(32_767)
        FileInputStream(iface.fileDescriptor).use { input ->
            FileOutputStream(iface.fileDescriptor).use { output ->
                while (!stopFlag.get() && !Thread.interrupted()) {
                    val n = try {
                        input.read(buf)
                    } catch (_: Exception) {
                        break
                    }
                    if (n <= 0) continue
                    val packet = buf.copyOf(n)
                    // A failing lookup must never take the whole watcher (and the phone's DNS) down with it.
                    runCatching { pool.execute { runCatching { handlePacket(packet, n, iocs, output) } } }
                }
            }
        }
        pool.shutdownNow()
        stopSelf()
    }

    private fun handlePacket(
        buf: ByteArray,
        length: Int,
        iocs: IocSet,
        output: FileOutputStream,
    ) {
        val parsed = DnsPacket.parse(buf, length) ?: return
        if (!parsed.isDnsQuery) return
        val names = DnsPacket.questionNames(parsed.payload) ?: return
        for (name in names) {
            iocs.lookupHost(name)?.let { recordHit(it, name) }
        }
        if (BLOCK_ON_HIT && names.any { iocs.lookupHost(it) != null }) return
        val dnsResponse = forwardQuery(parsed) ?: return
        val reply = DnsPacket.buildReply(parsed, dnsResponse) ?: return
        synchronized(output) { runCatching { output.write(reply) } }
    }

    private fun recordHit(indicator: Indicator, queriedName: String) {
        val hit = DnsHit(
            atMillis = System.currentTimeMillis(),
            domain = queriedName,
            family = indicator.family,
            category = indicator.category,
        )
        runBlocking { DnsHitStore.append(this@DnsGuardService, hit) }
        postWarningNotification(hit)
    }

    private fun forwardQuery(parsed: DnsPacket.Parsed): ByteArray? {
        val servers = DnsUpstream.order(upstreamDnsServers(), parsed.isIpv6)
        for (dns in servers) {
            try {
                DatagramSocket().use { socket ->
                    protect(socket)
                    socket.soTimeout = UPSTREAM_TIMEOUT_MS
                    socket.send(DatagramPacket(parsed.payload, parsed.payload.size, dns, 53))
                    val inBuf = ByteArray(4096)
                    val incoming = DatagramPacket(inBuf, inBuf.size)
                    socket.receive(incoming)
                    return inBuf.copyOf(incoming.length)
                }
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }

    /**
     * Resolvers of the real network underneath. While the watcher runs, the active network is our own VPN,
     * whose only resolver is this service - asking it would loop until the query times out.
     * Hard-coded public resolvers only when no network reports any (briefly after connect, captive portals).
     */
    private fun upstreamDnsServers(): List<InetAddress> {
        val cm = getSystemService(ConnectivityManager::class.java)
        @Suppress("DEPRECATION")
        val networks = listOfNotNull(cm.activeNetwork) + cm.allNetworks
        for (network in networks.distinct()) {
            val caps = cm.getNetworkCapabilities(network) ?: continue
            if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)) continue
            if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) continue
            val servers = DnsUpstream.usable(cm.getLinkProperties(network)?.dnsServers.orEmpty(), ownResolvers)
            if (servers.isNotEmpty()) return servers
        }
        return FALLBACK_DNS
    }

    // Numeric literals: getByName parses them without a lookup, and equality ignores v6 notation.
    private val ownResolvers by lazy { setOf(InetAddress.getByName(VIRTUAL_DNS_V4), InetAddress.getByName(VIRTUAL_DNS_V6)) }

    private fun openTunnel(): ParcelFileDescriptor? {
        return Builder()
            .setSession(localizedString(R.string.dns_session))
            .setMtu(1500)
            .addAddress(TUN_ADDRESS_V4, 32)
            .addAddress(TUN_ADDRESS_V6, 128)
            .addDnsServer(VIRTUAL_DNS_V4)
            .addDnsServer(VIRTUAL_DNS_V6)
            .addRoute(VIRTUAL_DNS_V4, 32)
            .addRoute(VIRTUAL_DNS_V6, 128)
            .allowFamily(OsConstants.AF_INET)
            .allowFamily(OsConstants.AF_INET6)
            .establish()
    }

    private fun localizedString(id: Int) = AppLanguage.localizedContext(this).getString(id)

    private fun ensureChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ALERTS,
                    localizedString(R.string.dns_alert_channel),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = localizedString(R.string.dns_alert_description) },
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SERVICE,
                    localizedString(R.string.dns_watch),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { description = localizedString(R.string.dns_service_description) },
            )
        }
    }

    private fun buildServiceNotification(): Notification {
        val pending = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Builder(this, CHANNEL_SERVICE)
            .setContentTitle(localizedString(R.string.dns_active))
            .setContentText(localizedString(R.string.dns_active_description))
            .setSmallIcon(de.belkis.spyhound.R.drawable.ic_launcher_monochrome)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    private fun postWarningNotification(hit: DnsHit) {
        val nm = getSystemService(NotificationManager::class.java)
        val title = when (hit.category) {
            Category.MERCENARY -> localizedString(R.string.dns_spyware_match)
            Category.STALKERWARE -> localizedString(R.string.dns_stalkerware_match)
            Category.MALWARE -> localizedString(R.string.dns_malware_match)
        }
        val text = "${hit.domain} · ${hit.family}"
        val id = (hit.domain.hashCode() xor hit.family.hashCode()) and 0x7fffffff
        val notification = Notification.Builder(this, CHANNEL_ALERTS)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setSmallIcon(de.belkis.spyhound.R.drawable.ic_launcher_monochrome)
            .setPriority(Notification.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(id, notification)
    }

    companion object {
        /** Prepared for a future block mode; default is warn-only per spec. */
        const val BLOCK_ON_HIT = false
        private const val LOOKUP_THREADS = 4
        private const val UPSTREAM_TIMEOUT_MS = 4_000

        private const val VIRTUAL_DNS_V4 = "10.111.222.1"
        private const val VIRTUAL_DNS_V6 = "fd00:5ec::1"

        // The interface needs its own address: packets to the interface's own address are delivered locally
        // and never reach the tunnel, so a resolver on that address would swallow every query.
        private const val TUN_ADDRESS_V4 = "10.111.222.2"
        private const val TUN_ADDRESS_V6 = "fd00:5ec::2"
        private const val CHANNEL_ALERTS = "Warnungen"
        private const val CHANNEL_SERVICE = "dns_guard_service"
        private const val NOTIF_SERVICE = 4101

        private val FALLBACK_DNS = listOf(
            InetAddress.getByName("9.9.9.9"),
            InetAddress.getByName("2606:4700:4700::1111"),
        )
    }
}
