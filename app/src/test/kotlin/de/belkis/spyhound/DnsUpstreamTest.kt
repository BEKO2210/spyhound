package de.belkis.spyhound

import de.belkis.spyhound.dns.DnsUpstream
import java.net.InetAddress
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DnsUpstreamTest {
    private fun ip(s: String) = InetAddress.getByName(s)
    private val own = setOf(ip("10.111.222.1"), ip("fd00:5ec::1"))

    @Test fun ownVirtualAddressesAreExcluded() {
        val usable = DnsUpstream.usable(listOf(ip("10.111.222.1"), ip("192.168.1.1"), ip("fd00:5ec:0:0:0:0:0:1")), own)
        assertEquals(listOf(ip("192.168.1.1")), usable)
    }

    @Test fun onlyOwnAddressesLeaveNothing() {
        assertTrue(DnsUpstream.usable(listOf(ip("10.111.222.1"), ip("fd00:5ec::1")), own).isEmpty())
    }

    @Test fun linkLocalComesLast() {
        val ordered = DnsUpstream.order(listOf(ip("fe80::1"), ip("2001:db8::53"), ip("169.254.0.1"), ip("192.168.1.1")), ipv6Query = true)
        assertEquals(listOf(ip("2001:db8::53"), ip("192.168.1.1"), ip("fe80::1"), ip("169.254.0.1")), ordered)
    }

    @Test fun sameFamilyFirst() {
        val servers = listOf(ip("192.168.1.1"), ip("2001:db8::53"), ip("192.168.1.2"))
        assertEquals(listOf(ip("2001:db8::53"), ip("192.168.1.1"), ip("192.168.1.2")), DnsUpstream.order(servers, ipv6Query = true))
        assertEquals(listOf(ip("192.168.1.1"), ip("192.168.1.2"), ip("2001:db8::53")), DnsUpstream.order(servers, ipv6Query = false))
    }

    @Test fun routerOrderIsKeptWithinGroups() {
        val servers = listOf(ip("192.168.1.9"), ip("192.168.1.2"), ip("192.168.1.5"))
        assertEquals(servers, DnsUpstream.order(servers, ipv6Query = false))
    }
}
