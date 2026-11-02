package de.belkis.spyhound.dns

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

/** Which resolvers of the real network the DNS watch may forward a query to, and in which order. */
object DnsUpstream {
    /** Drops the watch's own virtual resolvers (asking them would loop) and anything that is not IPv4/IPv6. */
    fun usable(candidates: List<InetAddress>, own: Set<InetAddress>): List<InetAddress> =
        candidates.filter { (it is Inet4Address || it is Inet6Address) && it !in own }

    /**
     * Same address family as the query first, the other one as fallback (a v6 query may still be answered by a v4
     * resolver). Link-local resolvers (fe80::1 on many routers) often do not answer, so they always come last.
     * The router's own order is kept within each group.
     */
    fun order(servers: List<InetAddress>, ipv6Query: Boolean): List<InetAddress> =
        servers.sortedWith(
            compareBy<InetAddress> { it.isLinkLocalAddress }.thenBy { if (ipv6Query == (it is Inet6Address)) 0 else 1 },
        )
}
