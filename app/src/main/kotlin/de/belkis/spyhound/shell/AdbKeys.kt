package de.belkis.spyhound.shell

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The app's own ADB identity: an RSA key and a self-signed certificate that exist only in app-private storage.
 * The key never leaves this class except to the connection library in memory. It is never exported or logged.
 *
 * libadb needs the raw key for TLS, so an Android Keystore key cannot be the ADB key itself. Instead the file is
 * encrypted with a non-exportable Keystore AES key; if the Keystore is unavailable the file stays app-private plain.
 */
internal class AdbKeys private constructor(val privateKey: PrivateKey, val certificate: Certificate) {

    companion object {
        private const val KEY_FILE = "adb_key.bin"
        private const val CERT_FILE = "adb_cert.der"
        private const val WRAP_ALIAS = "spyhound_adb_wrap"
        private const val FORMAT_PLAIN: Byte = 0
        private const val FORMAT_WRAPPED: Byte = 1
        private const val VALID_MS = 10L * 365 * 24 * 3600 * 1000

        fun exists(context: Context): Boolean =
            File(context.filesDir, KEY_FILE).isFile && File(context.filesDir, CERT_FILE).isFile

        fun loadOrCreate(context: Context): AdbKeys = load(context) ?: create(context)

        /** Forgets the identity. The phone then needs a new pairing. */
        fun delete(context: Context) {
            File(context.filesDir, KEY_FILE).delete()
            File(context.filesDir, CERT_FILE).delete()
            runCatching { KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(WRAP_ALIAS) }
        }

        private fun load(context: Context): AdbKeys? = runCatching {
            val keyFile = File(context.filesDir, KEY_FILE)
            val certFile = File(context.filesDir, CERT_FILE)
            if (!keyFile.isFile || !certFile.isFile) return null
            val pkcs8 = unwrap(keyFile.readBytes())
            val key = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(pkcs8))
            val cert = certFile.inputStream().use { CertificateFactory.getInstance("X.509").generateCertificate(it) }
            AdbKeys(key, cert)
        }.getOrNull()

        private fun create(context: Context): AdbKeys {
            val gen = KeyPairGenerator.getInstance("RSA")
            gen.initialize(2048, SecureRandom())
            val pair = gen.generateKeyPair()
            val now = Date()
            val cert = SelfSignedCert.create(
                pair.public, pair.private, "Spyhound", now, Date(now.time + VALID_MS), BigInteger(63, SecureRandom()).add(BigInteger.ONE),
            )
            // Private mode and no backup (allowBackup=false); the files are replaced as a whole.
            writePrivate(File(context.filesDir, KEY_FILE), wrap(pair.private.encoded))
            writePrivate(File(context.filesDir, CERT_FILE), cert.encoded)
            return AdbKeys(pair.private, cert)
        }

        private fun writePrivate(file: File, bytes: ByteArray) {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeBytes(bytes)
            tmp.setReadable(false, false); tmp.setReadable(true, true)
            tmp.setWritable(false, false); tmp.setWritable(true, true)
            check(tmp.renameTo(file)) { "key file could not be stored" }
        }

        private fun wrapKey(): SecretKey? = runCatching {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (ks.getKey(WRAP_ALIAS, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                .apply {
                    init(
                        KeyGenParameterSpec.Builder(WRAP_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setKeySize(256)
                            .build(),
                    )
                }.generateKey()
        }.getOrNull()

        private fun wrap(plain: ByteArray): ByteArray {
            val key = wrapKey() ?: return byteArrayOf(FORMAT_PLAIN) + plain
            return runCatching {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
                byteArrayOf(FORMAT_WRAPPED, cipher.iv.size.toByte()) + cipher.iv + cipher.doFinal(plain)
            }.getOrElse { byteArrayOf(FORMAT_PLAIN) + plain }
        }

        private fun unwrap(stored: ByteArray): ByteArray = when (stored[0]) {
            FORMAT_PLAIN -> stored.copyOfRange(1, stored.size)
            FORMAT_WRAPPED -> {
                val ivLen = stored[1].toInt()
                val iv = stored.copyOfRange(2, 2 + ivLen)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, checkNotNull(wrapKey()), GCMParameterSpec(128, iv))
                cipher.doFinal(stored, 2 + ivLen, stored.size - 2 - ivLen)
            }
            else -> error("unknown key format")
        }
    }
}
