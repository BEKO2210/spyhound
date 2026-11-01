package de.belkis.spyhound.shell

import java.io.ByteArrayOutputStream
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.math.BigInteger
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * Builds the self-signed certificate the TLS handshake with adbd presents. adbd only looks at the key inside it,
 * so a plain v3 certificate without extensions is enough. Pure Java crypto, no extra library.
 */
internal object SelfSignedCert {
    private val SHA256_WITH_RSA = byteArrayOf(0x2A, 0x86.toByte(), 0x48, 0x86.toByte(), 0xF7.toByte(), 0x0D, 0x01, 0x01, 0x0B)
    private val COMMON_NAME = byteArrayOf(0x55, 0x04, 0x03)

    fun create(publicKey: PublicKey, privateKey: PrivateKey, commonName: String, notBefore: Date, notAfter: Date, serial: BigInteger): X509Certificate {
        val algorithm = der(0x30, der(0x06, SHA256_WITH_RSA) + der(0x05, ByteArray(0)))
        val name = der(0x30, der(0x31, der(0x30, der(0x06, COMMON_NAME) + der(0x0C, commonName.toByteArray(Charsets.UTF_8)))))
        val tbs = der(
            0x30,
            der(0xA0, der(0x02, byteArrayOf(2))) + // version 3
                der(0x02, serial.toByteArray()) +
                algorithm + name +
                der(0x30, time(notBefore) + time(notAfter)) +
                name + publicKey.encoded,
        )
        val signature = Signature.getInstance("SHA256withRSA").run {
            initSign(privateKey)
            update(tbs)
            sign()
        }
        val cert = der(0x30, tbs + algorithm + der(0x03, byteArrayOf(0) + signature))
        return CertificateFactory.getInstance("X.509").generateCertificate(cert.inputStream()) as X509Certificate
    }

    private fun time(date: Date): ByteArray {
        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = date }
        val year = c.get(Calendar.YEAR)
        val body = "%02d%02d%02d%02d%02dZ".format(
            c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND),
        )
        // UTCTime covers 1950–2049; later dates must use GeneralizedTime.
        return if (year in 1950..2049) der(0x17, "%02d$body".format(year % 100).toByteArray())
        else der(0x18, "%04d$body".format(year).toByteArray())
    }

    private fun der(tag: Int, content: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(tag)
        val n = content.size
        when {
            n < 0x80 -> out.write(n)
            n < 0x100 -> { out.write(0x81); out.write(n) }
            else -> { out.write(0x82); out.write(n shr 8); out.write(n and 0xFF) }
        }
        out.write(content)
        return out.toByteArray()
    }
}
