package de.belkis.spyhound.shell

/** Why the built-in ADB setup stopped. It never retries on its own; the user restarts it. */
enum class AdbFailure { UNSUPPORTED, TOO_MANY_INVALID_CODES, PAIRING_REJECTED, CONNECT_FAILED, TIMEOUT, INTERNAL }

/** Where the pairing with the phone's own wireless-debugging service stands. */
sealed interface AdbSetupState {
    data object NeedsDevOptions : AdbSetupState
    data object NeedsWirelessDebugging : AdbSetupState
    data object WaitingForPairingCode : AdbSetupState
    data object Pairing : AdbSetupState
    data object Paired : AdbSetupState
    data object Connected : AdbSetupState
    data class Failed(val reason: AdbFailure) : AdbSetupState
}

sealed interface AdbSetupEvent {
    /** The phone's current settings. [paired] means a connection with our key was accepted before. */
    data class EnvironmentChecked(val devOptions: Boolean, val wirelessDebugging: Boolean, val paired: Boolean = false) : AdbSetupEvent
    data class PairingPortFound(val port: Int) : AdbSetupEvent
    data object PairingPortLost : AdbSetupEvent
    data class CodeEntered(val code: String) : AdbSetupEvent
    data class PairingFinished(val ok: Boolean) : AdbSetupEvent
    data class ConnectFinished(val ok: Boolean) : AdbSetupEvent
    data object WaitTimedOut : AdbSetupEvent
    /** An unexpected error in the background work; never leave the user on a stale screen. */
    data object InternalError : AdbSetupEvent
    data object Restart : AdbSetupEvent
}

/** The code shown in "Pair device with pairing code" is exactly six digits. Returns it trimmed, or null. */
fun validPairingCode(input: CharSequence?): String? =
    input?.toString()?.trim()?.takeIf { it.length == 6 && it.all { c -> c in '0'..'9' } }

/**
 * Pure state machine for the pairing flow, free of Android so every transition is testable on the JVM.
 * The caller does the work (discovering the port, pairing, connecting) and reports back with [on].
 * Every wait and every attempt is bounded, so a bad run ends in [AdbSetupState.Failed], never in a loop.
 */
class AdbSetup(private val sdkInt: Int) {
    var state: AdbSetupState = if (sdkInt >= MIN_SDK) AdbSetupState.NeedsDevOptions else AdbSetupState.Failed(AdbFailure.UNSUPPORTED)
        private set

    /** Port of the pairing service while the pairing dialog is open, else null. */
    var pairingPort: Int? = null
        private set

    var pairingAttempts = 0
        private set

    private var invalidCodes = 0

    /** The code can be sent: the pairing dialog is open and its port is known. */
    val canSubmitCode: Boolean get() = state == AdbSetupState.WaitingForPairingCode && pairingPort != null

    fun on(event: AdbSetupEvent): AdbSetupState {
        val current = state
        if (current is AdbSetupState.Failed) {
            // Only an explicit restart leaves a failure, and only if the phone can do it at all.
            if (event == AdbSetupEvent.Restart && current.reason != AdbFailure.UNSUPPORTED) reset()
            return state
        }
        when (event) {
            is AdbSetupEvent.Restart -> reset()
            is AdbSetupEvent.EnvironmentChecked -> environment(event)
            is AdbSetupEvent.PairingPortFound -> if (current == AdbSetupState.WaitingForPairingCode) pairingPort = event.port
            AdbSetupEvent.PairingPortLost -> if (current == AdbSetupState.WaitingForPairingCode) pairingPort = null
            is AdbSetupEvent.CodeEntered -> code(event.code)
            is AdbSetupEvent.PairingFinished -> if (current == AdbSetupState.Pairing) pairingDone(event.ok)
            is AdbSetupEvent.ConnectFinished -> if (current == AdbSetupState.Paired) {
                state = if (event.ok) AdbSetupState.Connected else AdbSetupState.Failed(AdbFailure.CONNECT_FAILED)
            }
            AdbSetupEvent.WaitTimedOut -> if (current == AdbSetupState.WaitingForPairingCode || current == AdbSetupState.Pairing) {
                state = AdbSetupState.Failed(AdbFailure.TIMEOUT)
            }
            AdbSetupEvent.InternalError -> state = AdbSetupState.Failed(AdbFailure.INTERNAL)
        }
        return state
    }

    private fun reset() {
        state = AdbSetupState.NeedsDevOptions
        pairingPort = null
        pairingAttempts = 0
        invalidCodes = 0
    }

    private fun environment(e: AdbSetupEvent.EnvironmentChecked) {
        val busy = state == AdbSetupState.Pairing || state == AdbSetupState.Paired
        state = when {
            busy -> state // a running step finishes on its own result
            // Newer Android versions can report developer options as off to apps; wireless debugging implies they are on.
            !(e.devOptions || e.wirelessDebugging) -> AdbSetupState.NeedsDevOptions
            !e.wirelessDebugging -> AdbSetupState.NeedsWirelessDebugging
            e.paired -> AdbSetupState.Paired
            state == AdbSetupState.Connected -> state
            else -> AdbSetupState.WaitingForPairingCode
        }
        if (state != AdbSetupState.WaitingForPairingCode) pairingPort = null
    }

    private fun code(raw: String) {
        if (!canSubmitCode) return
        if (validPairingCode(raw) == null) {
            if (++invalidCodes >= MAX_INVALID_CODES) state = AdbSetupState.Failed(AdbFailure.TOO_MANY_INVALID_CODES)
            return
        }
        pairingAttempts++
        state = AdbSetupState.Pairing
    }

    private fun pairingDone(ok: Boolean) {
        state = when {
            ok -> AdbSetupState.Paired
            pairingAttempts >= MAX_PAIRING_ATTEMPTS -> AdbSetupState.Failed(AdbFailure.PAIRING_REJECTED)
            else -> AdbSetupState.WaitingForPairingCode
        }
    }

    companion object {
        /** Wireless debugging with a pairing code exists from Android 11. */
        const val MIN_SDK = 30
        const val MAX_PAIRING_ATTEMPTS = 3
        const val MAX_INVALID_CODES = 5
    }
}

/** The built-in connection only ever talks to this phone: loopback or one of its own addresses. */
fun isLocalAdbHost(host: String, ownAddresses: Set<String> = emptySet()): Boolean {
    val h = host.trim().lowercase()
    return h == "localhost" || h == "::1" || h == "0:0:0:0:0:0:0:1" ||
        Regex("""127(\.\d{1,3}){3}""").matches(h) || h in ownAddresses
}
