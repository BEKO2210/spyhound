package de.belkis.spyhound

import de.belkis.spyhound.shell.SelfSignedCert
import org.junit.Test
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SelfSignedCertTest {
    @Test fun certificateParsesAndVerifiesAgainstItsOwnKey() {
        val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val now = Date()
        val cert = SelfSignedCert.create(pair.public, pair.private, "Spyhound", now, Date(now.time + 10L * 365 * 24 * 3600 * 1000), BigInteger.valueOf(123456789))
        cert.verify(pair.public)
        assertEquals("CN=Spyhound", cert.subjectX500Principal.name)
        assertEquals(cert.issuerX500Principal, cert.subjectX500Principal)
        assertEquals(pair.public, cert.publicKey)
        assertEquals(3, cert.version)
        assertTrue(cert.notAfter.after(cert.notBefore))
    }

    @Test fun datesAfter2049UseGeneralizedTime() {
        val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val past = Date(1_700_000_000_000)
        val far = Date(2_700_000_000_000) // year 2055
        val cert = SelfSignedCert.create(pair.public, pair.private, "Spyhound", past, far, BigInteger.TEN)
        cert.verify(pair.public)
        assertEquals(far.time / 1000, cert.notAfter.time / 1000)
    }
}
