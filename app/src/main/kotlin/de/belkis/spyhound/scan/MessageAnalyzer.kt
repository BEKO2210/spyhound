package de.belkis.spyhound.scan

import de.belkis.spyhound.ioc.IocSet

data class Message(val sender: String, val date: Long, val body: String)

/** Links in received SMS: the classic Pegasus / Predator delivery route. */
object MessageAnalyzer {
    const val NAME = "SMS-Links"

    fun analyze(messages: List<Message>?, iocs: IocSet, texts: Texts = TextsDe): CheckResult {
        val r = CheckResult(NAME, texts)
        if (messages == null) {
            r.gap("SMS wurden nicht geprüft: Die Berechtigung „SMS lesen“ fehlt.", GapFix.PERMISSION_SMS)
            return r
        }
        val scanner = TextScanner(iocs)
        for (msg in messages) {
            r.examined++
            for (m in scanner.hosts(msg.body) + scanner.urls(msg.body)) {
                r.hit(
                    "SMS von ${msg.sender}",
                    "Die Nachricht enthält einen Link zu ${m.token}, einer Adresse, die Forscher einer Spyware zuordnen. " +
                        "Wurde der Link geöffnet, kann das Gerät infiziert worden sein.",
                    m.indicator,
                )
            }
        }
        r.ran = true
        return r
    }
}
