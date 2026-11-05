package de.belkis.spyhound

import de.belkis.spyhound.background.BackgroundReportStore
import de.belkis.spyhound.scan.AccessLevel
import de.belkis.spyhound.scan.CheckResult
import de.belkis.spyhound.scan.MalwareCoverage
import de.belkis.spyhound.scan.MalwareFamily
import de.belkis.spyhound.scan.Report
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CoveragePersistenceTest {
    @Test fun backgroundReportRetainsCoverageAndFamilyOrdering() {
        val coverage = MalwareCoverage(1234, listOf("ESET", "Meta"),
            listOf(MalwareFamily("Hydra", 1000), MalwareFamily("TeaBot", 200)), 34)
        val check = CheckResult("Apps").apply { malwareCoverage = coverage; ran = true }
        val report = Report(0, 1, AccessLevel.APP, 1234, 2, 0, listOf(check), "phone")
        val context = RuntimeEnvironment.getApplication()
        BackgroundReportStore.save(context, report)
        val restored = assertNotNull(BackgroundReportStore.consume(context))
        assertEquals(coverage, restored.checks.single().malwareCoverage)
        assertEquals(emptyList(), restored.gaps)
    }
}
