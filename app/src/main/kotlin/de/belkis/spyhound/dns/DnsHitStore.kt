package de.belkis.spyhound.dns

import android.content.Context
import de.belkis.spyhound.ioc.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import java.io.File

data class DnsHit(
    val atMillis: Long,
    val domain: String,
    val family: String,
    val category: Category,
)

/** Append-only local list of indicator DNS hits (no benign logging). */
object DnsHitStore {
    private const val FILE = "dns_guard_hits.json"
    private const val MAX = 500
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun append(context: Context, hit: DnsHit) = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, FILE)
        val current = readLocked(file)
        val next = (listOf(hit) + current).take(MAX)
        file.writeText(encode(next))
    }

    fun recent(context: Context, limit: Int = 50): List<DnsHit> {
        val file = File(context.filesDir, FILE)
        if (!file.isFile) return emptyList()
        return readLocked(file).take(limit)
    }

    private fun readLocked(file: File): List<DnsHit> {
        val text = runCatching { file.readText() }.getOrNull().orEmpty()
        if (text.isBlank()) return emptyList()
        return runCatching {
            json.parseToJsonElement(text).jsonArray.mapNotNull { el ->
                val o = el.jsonObject
                val cat = runCatching { Category.valueOf(o["category"]!!.jsonPrimitive.content) }.getOrNull()
                    ?: return@mapNotNull null
                DnsHit(
                    atMillis = o["at"]?.jsonPrimitive?.long ?: return@mapNotNull null,
                    domain = o["domain"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                    family = o["family"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                    category = cat,
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encode(hits: List<DnsHit>): String {
        val arr = buildJsonArray {
            hits.forEach { h ->
                add(
                    buildJsonObject {
                        put("at", h.atMillis)
                        put("domain", h.domain)
                        put("family", h.family)
                        put("category", h.category.name)
                    },
                )
            }
        }
        return arr.toString()
    }
}
