package de.belkis.spyhound.dns

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.ConnectivityManager
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
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Local VPN that only captures DNS to the virtual resolvers and forwards queries to the device’s
 * normal DNS servers via [protect]. No traffic is blocked unless [BLOCK_ON_HIT] is enabled later.
 */
class DnsGuardService : VpnService() {
    private var tun: ParcelFileDescriptor? = null
    private val stopFlag = AtomicBoolean(false)
    private var worker: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
        stopFlag.set(true)
        worker?.interrupt()
        worker = null
        tun?.close()
        tun = null
        DnsGuard.isRunning = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }

    private fun runTunnel() {
        val iocs = runBlocking { IocStore(this@DnsGuardService).load().set }
        val iface = openTunnel() ?: run {
            stopSelf()
            return
        }
        tun = iface
        DnsGuard.isRunning = true
        val socket4 = DatagramSocket().apply { protect(this) }
        val socket6 = DatagramSocket().apply { protect(this) }
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
                    handlePacket(buf, n, iocs, socket4, socket6, output)
                }
            }
        }
        socket4.close()
        socket6.close()
        stopSelf()
    }

    private fun handlePacket(
        buf: ByteArray,
        length: Int,
        iocs: IocSet,
        socket4: DatagramSocket,
        socket6: DatagramSocket,
        output: FileOutputStream,
    ) {
        val parsed = DnsPacket.parse(buf, length) ?: return
        if (!parsed.isDnsQuery) return
        val names = DnsPacket.questionNames(parsed.payload) ?: return
        for (name in names) {
            iocs.lookupHost(name)?.let { recordHit(it, name) }
        }
        if (BLOCK_ON_HIT && names.any { iocs.lookupHost(it) != null }) return
        val dnsResponse = forwardQuery(parsed, socket4, socket6) ?: return
        val reply = DnsPacket.buildReply(parsed, dnsResponse) ?: return
        output.write(reply)
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

    private fun forwardQuery(parsed: DnsPacket.Parsed, socket4: DatagramSocket, socket6: DatagramSocket): ByteArray? {
        val servers = upstreamDnsServers()
        val preferV6 = parsed.isIpv6
        val ordered = servers.filter { if (preferV6) it is Inet6Address else it is Inet4Address }
        if (ordered.isEmpty()) return null
        val socket = if (preferV6) socket6 else socket4
        for (dns in ordered) {
            try {
                socket.soTimeout = 8_000
                val out = DatagramPacket(parsed.payload, parsed.payload.size, dns, 53)
                socket.send(out)
                val inBuf = ByteArray(4096)
                val incoming = DatagramPacket(inBuf, inBuf.size)
                socket.receive(incoming)
                return inBuf.copyOf(incoming.length)
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }

    /**
     * DNS from [ConnectivityManager.getLinkProperties] on the active network — what the OS would use without us.
     * Fallback only when there is no active network or the link reports no resolvers (common briefly after
     * connect or on some captive portals); hard-coded public resolvers avoid dropping queries entirely.
     */
    private fun upstreamDnsServers(): List<InetAddress> {
        val cm = getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork
        if (network != null) {
            val props = cm.getLinkProperties(network)
            if (props != null) {
                val fromLink = props.dnsServers.filter { it is Inet4Address || it is Inet6Address }
                if (fromLink.isNotEmpty()) return fromLink
            }
        }
        return FALLBACK_DNS
    }

    private fun openTunnel(): ParcelFileDescriptor? {
        return Builder()
            .setSession("Spyhound DNS-Wächter")
            .setMtu(1500)
            .addAddress(VIRTUAL_DNS_V4, 32)
            .addAddress(VIRTUAL_DNS_V6, 128)
            .addDnsServer(VIRTUAL_DNS_V4)
            .addDnsServer(VIRTUAL_DNS_V6)
            .addRoute(VIRTUAL_DNS_V4, 32)
            .addRoute(VIRTUAL_DNS_V6, 128)
            .allowFamily(OsConstants.AF_INET)
            .allowFamily(OsConstants.AF_INET6)
            .establish()
    }

    private fun ensureChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ALERTS,
                    "Warnungen",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = "DNS-Treffer auf bekannte Spionage-Domains" },
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SERVICE,
                    "DNS-Wächter",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { description = "Läuft im Hintergrund und leitet DNS weiter" },
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
            .setContentTitle("DNS-Wächter aktiv")
            .setContentText("Beobachtet DNS-Anfragen auf Indikator-Domains")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    private fun postWarningNotification(hit: DnsHit) {
        val nm = getSystemService(NotificationManager::class.java)
        val title = when (hit.category) {
            Category.MERCENARY -> "Verdächtige Spyware-Domain"
            Category.STALKERWARE -> "Verdächtige Stalkerware-Domain"
        }
        val text = "${hit.domain} · ${hit.family}"
        val id = (hit.domain.hashCode() xor hit.family.hashCode()) and 0x7fffffff
        val notification = Notification.Builder(this, CHANNEL_ALERTS)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(Notification.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(id, notification)
    }

    companion object {
        /** Prepared for a future block mode; default is warn-only per spec. */
        const val BLOCK_ON_HIT = false

        private const val VIRTUAL_DNS_V4 = "10.111.222.1"
        private const val VIRTUAL_DNS_V6 = "fd00:5ec::1"
        private const val CHANNEL_ALERTS = "Warnungen"
        private const val CHANNEL_SERVICE = "dns_guard_service"
        private const val NOTIF_SERVICE = 4101

        private val FALLBACK_DNS = listOf(
            InetAddress.getByName("9.9.9.9"),
            InetAddress.getByName("2606:4700:4700::1111"),
        )
    }
}
