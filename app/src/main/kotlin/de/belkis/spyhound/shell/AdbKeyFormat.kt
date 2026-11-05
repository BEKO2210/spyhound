package de.belkis.spyhound.shell

internal const val FORMAT_PLAIN: Byte = 0
internal const val FORMAT_WRAPPED: Byte = 1

/** What to do with a stored ADB key blob when it is read. */
internal sealed interface StoredKey {
    /** Usable key bytes; [rewrite] is non-null when the file on disk must be replaced (plain key moved to the Keystore). */
    class Ready(val pkcs8: ByteArray, val rewrite: ByteArray?) : StoredKey
    /** A plaintext key that cannot be protected: it must not stay on disk. */
    data object Discard : StoredKey
}

/**
 * Old versions stored the key unencrypted when the Android Keystore was unavailable. A plain key is re-wrapped
 * if the Keystore works now; otherwise it is discarded, because a plaintext private key on disk is worse than
 * pairing again once. [wrap] returns null when the Keystore cannot be used; [unwrapWrapped] decrypts a wrapped blob.
 */
internal fun readStoredKey(stored: ByteArray, wrap: (ByteArray) -> ByteArray?, unwrapWrapped: (ByteArray) -> ByteArray): StoredKey =
    when (stored[0]) {
        FORMAT_PLAIN -> {
            val pkcs8 = stored.copyOfRange(1, stored.size)
            wrap(pkcs8)?.let { StoredKey.Ready(pkcs8, it) } ?: StoredKey.Discard
        }
        FORMAT_WRAPPED -> StoredKey.Ready(unwrapWrapped(stored), null)
        else -> error("unknown key format")
    }
