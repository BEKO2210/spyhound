package de.belkis.spyhound

import de.belkis.spyhound.background.BackgroundFindingDiff
import de.belkis.spyhound.ioc.Category
import de.belkis.spyhound.ioc.Indicator
import de.belkis.spyhound.ioc.IocType
import de.belkis.spyhound.scan.Finding
import kotlin.test.assertEquals
import org.junit.Test

class BackgroundFindingDiffTest {
    @Test
    fun onlyReturnsFindingsNotSeenBefore() {
        val existing = finding(where = "Paket com.example.spy", indicatorValue = "com.example.spy")
        val fresh = finding(where = "Paket com.example.new", indicatorValue = "com.example.new")
        val previous = setOf(BackgroundFindingDiff.keyOf(existing))

        val result = BackgroundFindingDiff.newFindings(previous, listOf(existing, fresh))

        assertEquals(listOf(fresh), result)
    }

    private fun finding(where: String, indicatorValue: String): Finding = Finding(
        check = "Apps",
        where = where,
        detail = "Treffer",
        indicator = Indicator(
            value = indicatorValue,
            type = IocType.APP_ID,
            category = Category.MERCENARY,
            family = "Demo",
            sourceId = "demo",
        ),
    )
}
