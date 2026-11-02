package de.belkis.spyhound.background

import de.belkis.spyhound.scan.Finding

/** Compares stable finding identities, independent from UI language. */
object BackgroundFindingDiff {
    fun keyOf(finding: Finding): String =
        buildString {
            append(finding.indicator.sourceId)
            append('|')
            append(finding.indicator.type.name)
            append('|')
            append(finding.indicator.value)
            append('|')
            append(finding.where)
        }

    fun newFindings(previousKeys: Set<String>, current: List<Finding>): List<Finding> =
        current.filter { keyOf(it) !in previousKeys }
}
