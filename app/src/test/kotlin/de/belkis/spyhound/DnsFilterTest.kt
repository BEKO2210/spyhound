package de.belkis.spyhound

import de.belkis.spyhound.dns.DnsFilter
import de.belkis.spyhound.dns.DnsFilterSettings
import de.belkis.spyhound.dns.DnsHit
import de.belkis.spyhound.dns.DnsHitStore
import de.belkis.spyhound.dns.DnsPacket
import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.DomainList
import de.belkis.spyhound.ioc.DomainLists
import de.belkis.spyhound.ioc.FeedCatalog
import de.belkis.spyhound.ioc.Indicator
import de.belkis.spyhound.ioc.IocSet
import de.belkis.spyhound.ioc.IocType
import java.io.File
import java.security.MessageDigest
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** DNS watcher, roadmap step 4: tracker and phishing lists, per-category switches, local NXDOMAIN, speed. */
class DnsFilterTest {
    private val dir = File("src/main/assets/iocs")

    private val spyware = IocSet(
        listOf(Indicator("c2.spy.example", IocType.DOMAIN, Category.STALKERWARE, "Spyfam", "t")),
    )
    private val lists = DomainLists(
        mapOf(
            Category.PHISHING to listOf("Phishing" to DomainList.of(listOf("bank-login.example", "c2.spy.example"))),
            Category.TRACKER to listOf("Tracker" to DomainList.of(listOf("ads.example", "c2.spy.example"))),
        ),
    )

    // --- parser ---

    @Test fun parsesHostsFileAndPlainLists() {
        val text = "# comment\r\n\r\n0.0.0.0 Ads.Example.COM\r\n127.0.0.1\tlocalhost.example # note\r\n" +
            "plain.example\n0.0.0.0 1.2.3.4\n::1 ip6-localhost\nnodot\n0.0.0.0 bad_chars!.example\n"
        val list = DomainList.parse(text)
        assertEquals(3, list.size)
        assertTrue(list.contains("ads.example.com"))
        assertTrue(list.contains("localhost.example"))
        assertTrue(list.contains("plain.example"))
        assertFalse(list.contains("nodot"))
    }

    @Test fun duplicatesAndSharedServicesAreDropped() {
        val list = DomainList.parse("a.example\nA.EXAMPLE\nbit.ly\ngithub.io\nok.example\n")
        assertEquals(2, list.size)
        assertFalse(list.contains("bit.ly"))
    }

    @Test fun lookupMatchesSubdomainsButNotLookalikes() {
        val list = DomainList.of(listOf("evil.example", "deep.tracker.test"))
        assertEquals("evil.example", list.lookup("evil.example"))
        assertEquals("evil.example", list.lookup("WWW.Evil.Example."))
        assertEquals("evil.example", list.lookup("a.b.evil.example"))
        assertNull(list.lookup("notevil.example"))
        assertNull(list.lookup("example"))
        assertNull(list.lookup("tracker.test")) // the parent of a listed subdomain is not listed
        assertEquals("deep.tracker.test", list.lookup("x.deep.tracker.test"))
        assertNull(list.lookup(""))
        assertNull(DomainList.EMPTY.lookup("evil.example"))
    }

    @Test fun neverMatchesABareTld() {
        val list = DomainList.of(listOf("com", "co.uk", "example.com"))
        assertNull(list.lookup("other.com"))
        assertEquals("example.com", list.lookup("x.example.com"))
    }

    // --- speed and memory (3) ---

    @Test fun lookupIsFastWithHundredThousandEntries() {
        val entries = (0 until 100_000).map { "host$it.group${it % 977}.example${it % 13}.test" }
        val list = DomainList.of(entries)
        assertEquals(100_000, list.size)
        val queries = (0 until 20_000).map { i ->
            if (i % 2 == 0) "www.host${i * 5}.group${(i * 5) % 977}.example${(i * 5) % 13}.test" else "clean$i.other.org"
        }
        repeat(2) { queries.forEach { list.lookup(it) } } // warm up the JIT
        val start = System.nanoTime()
        var hits = 0
        for (q in queries) if (list.lookup(q) != null) hits++
        val perQueryMs = (System.nanoTime() - start) / 1e6 / queries.size
        assertEquals(10_000, hits)
        assertTrue(perQueryMs < 1.0, "lookup took $perQueryMs ms per query")
    }

    // --- source separation ---

    @Test fun domainListsAreSeparateFromSpywareSources() {
        val spywareSources = FeedCatalog.parse(File(dir, "sources.json").readText())
        assertTrue(spywareSources.none { it.category == Category.TRACKER || it.category == Category.PHISHING })
        val domainSources = FeedCatalog.parse(File(dir, "domainlists.json").readText())
        assertEquals(setOf(Category.TRACKER, Category.PHISHING), domainSources.map { it.category }.toSet())
        assertTrue(domainSources.all { it.commit.length == 40 && it.sha256.length == 64 })
        assertTrue(domainSources.none { it.category == Category.PHISHING && it.bundled }) // too large and short-lived
    }

    @Test fun bundledDomainListsMatchTheirPin() {
        for (s in FeedCatalog.parse(File(dir, "domainlists.json").readText()).filter { it.bundled }) {
            val bytes = File(dir, s.fileName).readBytes()
            val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            assertEquals(s.sha256, sha, s.id)
            val list = DomainList.parse(bytes)
            assertTrue(list.size > 50, s.id)
        }
    }

    @Test fun categoryFlagsSeparateEvidenceFromLists() {
        assertTrue(Category.MERCENARY.isEvidence && Category.STALKERWARE.isEvidence && Category.MALWARE.isEvidence)
        assertFalse(Category.TRACKER.isEvidence)
        assertFalse(Category.PHISHING.isEvidence)
    }

    // --- defaults and decisions ---

    @Test fun defaults() {
        val d = DnsFilterSettings()
        assertTrue(d.warnPhishing)
        assertFalse(d.warnTracker)
        assertFalse(d.blockPhishing)
        assertFalse(d.blockTracker)
        for (c in Category.entries) assertEquals(c != Category.TRACKER, d.warns(c))
        for (c in Category.entries) assertFalse(d.blocks(c))
    }

    @Test fun defaultsWarnAboutPhishingOnly() {
        val d = DnsFilterSettings()
        val phishing = DnsFilter.evaluate(listOf("login.bank-login.example"), spyware, lists, d)
        assertEquals(listOf(Category.PHISHING), phishing.matches.map { it.category })
        assertFalse(phishing.block)
        assertTrue(DnsFilter.evaluate(listOf("cdn.ads.example"), spyware, lists, d).matches.isEmpty())
        assertTrue(DnsFilter.evaluate(listOf("fine.example"), spyware, lists, d).matches.isEmpty())
    }

    @Test fun trackerWarnAndBlockAreIndependent() {
        val warn = DnsFilterSettings(warnTracker = true)
        val v = DnsFilter.evaluate(listOf("ads.example"), spyware, lists, warn)
        assertEquals(Category.TRACKER, v.matches.single().category)
        assertFalse(v.block)
        val block = DnsFilterSettings(warnTracker = false, blockTracker = true)
        val b = DnsFilter.evaluate(listOf("ads.example"), spyware, lists, block)
        assertTrue(b.block)
        assertTrue(b.matches.single().blocked)
    }

    @Test fun phishingBlockOnlyWhenSwitchedOn() {
        val v = DnsFilter.evaluate(listOf("bank-login.example"), spyware, lists, DnsFilterSettings(blockPhishing = true))
        assertTrue(v.block)
        assertFalse(DnsFilter.evaluate(listOf("bank-login.example"), spyware, lists, DnsFilterSettings()).block)
    }

    @Test fun spywareIsNeverBlocked() {
        val all = DnsFilterSettings(true, true, true, true)
        // c2.spy.example is a spyware indicator and also (by mistake) on both lists.
        val v = DnsFilter.evaluate(listOf("c2.spy.example"), spyware, lists, all)
        assertFalse(v.block)
        assertEquals(Category.STALKERWARE, v.matches.first().category)
        assertTrue(v.matches.none { it.blocked })
        // A query that mixes a blockable name with spyware is still answered normally.
        assertFalse(DnsFilter.evaluate(listOf("ads.example", "c2.spy.example"), spyware, lists, all).block)
    }

    @Test fun spywareWarnsEvenWithEverythingOff() {
        val off = DnsFilterSettings(false, false, false, false)
        val v = DnsFilter.evaluate(listOf("c2.spy.example"), spyware, DomainLists.EMPTY, off)
        assertEquals(Category.STALKERWARE, v.matches.single().category)
    }

    // --- block answer (4) ---

    @Test fun nxdomainAnswersTheSameQuestion() {
        val query = DnsPacket.buildDnsQuery("ads.example", id = 0x4242)
        val reply = DnsPacket.buildNxdomain(query)
        assertNotNull(reply)
        assertEquals(query.size, reply.size)
        assertEquals(0x4242, ((reply[0].toInt() and 0xff) shl 8) or (reply[1].toInt() and 0xff))
        val flags = ((reply[2].toInt() and 0xff) shl 8) or (reply[3].toInt() and 0xff)
        assertEquals(0x8000, flags and 0x8000) // response
        assertEquals(3, flags and 0xf) // NXDOMAIN
        assertEquals(0x0100, flags and 0x0100) // recursion desired is copied from the query (set by buildDnsQuery)
        assertEquals(1, ((reply[4].toInt() and 0xff) shl 8) or (reply[5].toInt() and 0xff))
        for (i in 6..11) assertEquals(0, reply[i].toInt())
        assertContentEquals(query.copyOfRange(12, query.size), reply.copyOfRange(12, reply.size))
        assertEquals(listOf("ads.example"), DnsPacket.questionNames(reply))
    }

    @Test fun nxdomainDatagramReachesTheClient() {
        val client = DnsPacket.ipBytes("192.0.2.10")
        val resolver = DnsPacket.ipBytes("10.111.222.1")
        val query = DnsPacket.buildDnsQuery("ads.example")
        val parsed = DnsPacket.parse(DnsPacket.buildIpv4UdpQuery(client, resolver, 44000, 53, query))
        assertNotNull(parsed)
        val reply = DnsPacket.parse(DnsPacket.buildReply(parsed, DnsPacket.buildNxdomain(parsed.payload)!!)!!)
        assertNotNull(reply)
        assertEquals(44000, reply.dstPort)
        assertContentEquals(client, reply.dstIp)
        assertEquals(3, reply.payload[3].toInt() and 0xf)
    }

    @Test fun nxdomainRejectsBrokenQueries() {
        assertNull(DnsPacket.buildNxdomain(ByteArray(5)))
        val truncated = DnsPacket.buildDnsQuery("ads.example").copyOf(16)
        assertNull(DnsPacket.buildNxdomain(truncated))
    }

    // --- evidence is never pushed out by list hits ---

    @Test fun fullHitListDropsTrackerBeforeSpyware() {
        fun hit(t: Long, c: Category) = DnsHit(t, "d$t.example", "f", c)
        val hits = listOf(hit(5, Category.TRACKER), hit(4, Category.STALKERWARE), hit(3, Category.TRACKER), hit(2, Category.MERCENARY), hit(1, Category.PHISHING))
        val kept = DnsHitStore.trim(hits, 3)
        assertEquals(listOf(5L, 4L, 2L), kept.map { it.atMillis })
        assertEquals(hits, DnsHitStore.trim(hits, 10))
    }
}
