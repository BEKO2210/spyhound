package de.belkis.spyhound.scan

enum class SmsRequestOutcome { GRANTED, DENIED, BLOCKED_BY_ANDROID }

/**
 * Result of a READ_SMS request. Android shows no dialog when "restricted settings" blocks an app that
 * was not installed through a store: the answer is "denied" at once and no rationale is offered.
 * The same signal appears after "don't ask again", and the fix (app info) helps in both cases.
 */
fun smsRequestOutcome(granted: Boolean, shouldShowRationale: Boolean): SmsRequestOutcome = when {
    granted -> SmsRequestOutcome.GRANTED
    shouldShowRationale -> SmsRequestOutcome.DENIED
    else -> SmsRequestOutcome.BLOCKED_BY_ANDROID
}
