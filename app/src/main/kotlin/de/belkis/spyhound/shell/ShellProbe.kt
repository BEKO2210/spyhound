package de.belkis.spyhound.shell

enum class ProbeOutcome { ALIVE, ALIVE_AFTER_RECONNECT, DEAD }

/**
 * probe → (reconnect → probe) → verdict. The reconnect happens at most once, so a truly dead shell
 * ends in exactly one [ProbeOutcome.DEAD] and never in a retry loop.
 */
suspend fun probeWithReconnect(probe: suspend () -> Boolean, reconnect: suspend () -> Boolean): ProbeOutcome {
    if (probe()) return ProbeOutcome.ALIVE
    if (!reconnect()) return ProbeOutcome.DEAD
    return if (probe()) ProbeOutcome.ALIVE_AFTER_RECONNECT else ProbeOutcome.DEAD
}
