package de.belkis.spyhound

import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.FeedCatalog
import de.belkis.spyhound.ioc.IocSet
import de.belkis.spyhound.ioc.IocType
import de.belkis.spyhound.ioc.Parsers
import de.belkis.spyhound.ioc.SourceContext
import de.belkis.spyhound.scan.AppAnalyzer
import de.belkis.spyhound.scan.AppRecord
import de.belkis.spyhound.scan.ApkFile
import de.belkis.spyhound.scan.DeviceAnalyzer
import de.belkis.spyhound.scan.DeviceState
import de.belkis.spyhound.scan.Message
import de.belkis.spyhound.scan.MessageAnalyzer
import de.belkis.spyhound.scan.ShellAnalyzer
import de.belkis.spyhound.scan.TextScanner
import java.io.File
import java.time.LocalDate
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoreTest {
    private val ctx = SourceContext("test", Category.MERCENARY, "TestSpy")
    private val stix = """
        {"type":"bundle","objects":[
          {"type":"malware","id":"malware--1","name":"Predator"},
          {"type":"indicator","id":"indicator--1","pattern":"[domain-name:value='evil.example']"},
          {"type":"indicator","id":"indicator--2","pattern":"[app:id='com.spy.agent']"},
          {"type":"indicator","id":"indicator--3","pattern":"[file:hashes.sha256='${"a".repeat(64)}']"},
          {"type":"indicator","id":"indicator--4","pattern":"[app:cert.sha1='31:A6:EC:EC:D9:7C:F3:9B:C4:12:6B:87:45:CD:94:A7:C3:0B:F8:1C']"},
          {"type":"indicator","id":"indicator--5","pattern":"[android-property:name='sys.brand.note']"},
          {"type":"indicator","id":"indicator--6","pattern":"[process:name='fservernetd'] OR [file:path='/data/local/tmp/spy']"},
          {"type":"indicator","id":"indicator--7","pattern":"[domain-name:value='bit.ly']"},
          {"type":"relationship","relationship_type":"indicates","source_ref":"indicator--1","target_ref":"malware--1"}
        ]}
    """.trimIndent()
    private val iocs = IocSet(Parsers.stix2(stix, ctx))

    @Test fun stixPatternsBecomeTypedIndicators() {
        assertEquals("Predator", iocs.lookup(IocType.DOMAIN, "EVIL.example.")?.family)
        assertEquals("TestSpy", iocs.lookup(IocType.APP_ID, "com.spy.agent")?.family)
        assertTrue(iocs.lookup(IocType.APP_CERT, "31a6ececd97cf39bc4126b8745cd94a7c30bf81c") != null)
        assertTrue(iocs.lookup(IocType.PROCESS, "fservernetd") != null)
        assertTrue(iocs.lookup(IocType.FILE_PATH, "/data/local/tmp/spy") != null)
        // A whole shared service is never an indicator on its own.
        assertNull(iocs.lookup(IocType.DOMAIN, "bit.ly"))
    }

    @Test fun hostMatchingWalksParentsButNotSiblings() {
        assertEquals("evil.example", iocs.lookupHost("cdn.a.evil.example")?.value)
        assertNull(iocs.lookupHost("notevil.example"))
        assertNull(iocs.lookupHost("example"))
        val found = TextScanner(iocs).hosts("GET https://x.evil.example/p and notevil.example and evil.examples.org")
        assertEquals(listOf("x.evil.example"), found.map { it.token })
    }

    @Test fun iosOnlySourcesDoNotMatchAndroidArtifacts() {
        val ios = IocSet(Parsers.stix2(stix, SourceContext("ios", Category.MERCENARY, androidArtifacts = false)))
        assertNull(ios.lookup(IocType.APP_ID, "com.spy.agent"))
        assertNull(ios.lookup(IocType.PROCESS, "fservernetd"))
        assertTrue(ios.lookupHost("evil.example") != null)
    }

    @Test fun mispCsvDomainsTxtAndCuratedTsv() {
        val misp = "uuid,event_id,category,type,value,comment,to_ids,date\n" +
            "1,10,Network activity,domain,\"redcrossworld.com\",\"a, b\",1,20160824\n" +
            "2,10,Network activity,ip-dst,\"52.8.153.44\",\"\",1,20160824\n"
        val m = Parsers.mispCsv(misp, ctx)
        assertEquals(listOf(IocType.DOMAIN, IocType.IP), m.map { it.type })
        assertEquals(2, Parsers.domainsTxt("stopsms.biz\n# x\n\nrevolution-news.co\n", ctx).size)
        val tsv = Parsers.curatedTsv("# c\nawake-wood.io\tinfrastructure\tprimary\tcl-2025\tParagon Graphite\n${"b".repeat(64)}\tsample\tprimary\tcl\n", ctx)
        assertEquals("Paragon Graphite", tsv[0].family)
        assertEquals(IocType.SHA256, tsv[1].type)
    }

    @Test fun appAnalyzerFindsIdHashCertAndSideloadLeads() {
        val apps = listOf(
            AppRecord("com.spy.agent", installer = "com.android.vending"),
            AppRecord("com.ok", apks = listOf(ApkFile("/data/app/base.apk", "A".repeat(64))), installer = "com.android.vending"),
            AppRecord("com.signed", certSha1 = listOf("31a6ececd97cf39bc4126b8745cd94a7c30bf81c"), installer = "com.android.vending"),
            AppRecord("com.hand", installer = "com.google.android.apps.nbu.files", grantedPermissions = setOf("android.permission.READ_SMS"), accessibility = true),
            AppRecord("com.plain.sideload", installer = null),
            AppRecord("de.belkis.spyhound", installer = null, hiddenFromLauncher = true),
        )
        val r = AppAnalyzer.analyze(apps, iocs, "de.belkis.spyhound")
        assertEquals(3, r.findings.size)
        assertEquals(listOf("App com.hand (com.hand)"), r.leads.map { it.where })
        assertTrue(r.leads[0].detail.contains("SMS lesen"))
        assertEquals(5, r.examined)
    }

    @Test fun deviceAnalyzerFlagsPropsPatchAndIntegrity() {
        val state = DeviceState(
            props = mapOf("sys.brand.note" to "1", "ro.boot.verifiedbootstate" to "orange"),
            securityPatch = "2026-01-05", sdk = 35, adbEnabled = true, adbWifiEnabled = false,
            developerOptions = true, installSources = emptyList(), rootTraces = listOf("/system/bin/su"), selinux = "Enforcing",
        )
        val r = DeviceAnalyzer.analyze(state, iocs, LocalDate.of(2026, 11, 1))
        assertEquals(1, r.findings.size)
        val where = r.leads.map { it.where }
        assertTrue(where.containsAll(listOf("Sicherheitspatch 2026-01-05", "USB-Debugging", "Bootloader", "Root")), where.toString())
    }

    @Test fun shellChecks() {
        assertEquals(2, ShellAnalyzer.processes("NAME\ninit\nfservernetd\ncom.spy.agent:remote\n", iocs).findings.size)
        val pkgs = ShellAnalyzer.packages("package:com.spy.agent  installer=null\npackage:com.ok\n", setOf("com.ok"), iocs)
        assertEquals(1, pkgs.findings.size)
        val logs = ShellAnalyzer.logs(mapOf("logcat" to "I/Net: connect x.evil.example:443 from com.spy.agent /data/local/tmp/spy", "dumpsys" to ""), iocs)
        assertEquals(3, logs.findings.size)
        assertEquals(1, logs.gaps.size)
        assertEquals(1, ShellAnalyzer.files("/sdcard/a.txt\n/data/local/tmp/spy\n", iocs).findings.size)
        assertEquals(1, ShellAnalyzer.hashes("${"a".repeat(64)}  /data/local/tmp/x\n${"c".repeat(64)}  /y\n", iocs).findings.size)
    }

    @Test fun smsLinksAndMissingPermission() {
        val r = MessageAnalyzer.analyze(listOf(Message("+49", 0, "Paket: https://track.evil.example/abc"), Message("+1", 0, "hi")), iocs)
        assertEquals(1, r.findings.size)
        assertEquals(1, MessageAnalyzer.analyze(null, iocs).gaps.size)
    }

    /** The real bundled indicator files must all parse, and the catalog must stay on the allow-list. */
    @Test fun bundledAssetsParse() {
        val dir = File("src/main/assets/iocs")
        val sources = FeedCatalog.parse(File(dir, "sources.json").readText())
        assertEquals(29, sources.size)
        var total = 0
        for (s in sources.filter { it.bundled }) {
            val f = File(dir, s.fileName)
            assertTrue(f.isFile, "missing bundled ${f.name}")
            val list = Parsers.parse(s.format, f.readText(), SourceContext(s.id, s.category, s.family, s.androidArtifacts))
            assertTrue(list.isNotEmpty(), s.id)
            total += list.size
        }
        total += Parsers.curatedTsv(File(dir, "adfection_iocs.txt").readText(), ctx).size
        total += Parsers.curatedTsv(File(dir, "curated_mercenary.txt").readText(), ctx).size
        val set = IocSet(sources.filter { it.bundled }.flatMap {
            Parsers.parse(it.format, File(dir, it.fileName).readText(), SourceContext(it.id, it.category, it.family, it.androidArtifacts))
        })
        println("bundled indicators parsed=$total unique=${set.size} appIds=${set.count(IocType.APP_ID)} certs=${set.count(IocType.APP_CERT)}")
        assertTrue(set.count(IocType.APP_ID) > 500)
        assertTrue(set.lookup(IocType.APP_CERT, "31A6ECECD97CF39BC4126B8745CD94A7C30BF81C") != null)
    }


    @Test fun queriesDeclarationsAreNotEvidence() {
        val dump = "    queriesPackages=[com.android.vending, com.spy.agent, com.whatsapp]\n" +
            "  Package [com.spy.agent] (1a2b3c):\n"
        val r = ShellAnalyzer.logs(mapOf("dumpsys package" to dump), iocs)
        assertEquals(1, r.findings.size)
        val onlyDeclared = ShellAnalyzer.logs(mapOf("dumpsys package" to dump.lines().first()), iocs)
        assertEquals(0, onlyDeclared.findings.size)
    }
}
