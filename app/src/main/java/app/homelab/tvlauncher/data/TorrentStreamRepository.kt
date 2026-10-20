package app.homelab.tvlauncher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class TorrentStreamRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun fetchSessions(settings: LauncherSettings): Result<Pair<List<TorrentSession>, String>> =
        withContext(Dispatchers.IO) {
            val candidates = buildCandidateUrls(settings.torrentStreamUrl, settings.tailscaleIp)
            var lastError: Throwable? = null

            for (baseUrl in candidates) {
                try {
                    val body = httpGet("$baseUrl/api/tv/sessions", timeoutMs = 2500)
                    val list = json.decodeFromString(ListSerializer(TorrentSession.serializer()), body)
                    return@withContext Result.success(list to baseUrl)
                } catch (t: Throwable) {
                    lastError = t
                }
            }
            Result.failure(lastError ?: IllegalStateException("Servidor Torrent Stream no accesible"))
        }

    suspend fun pauseOrResume(baseUrl: String, sessionId: String, pause: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val action = if (pause) "pause" else "resume"
                httpPost("$baseUrl/api/tv/sessions/$sessionId/$action", "{}")
                true
            }.getOrDefault(false)
        }

    suspend fun selectFile(baseUrl: String, sessionId: String, fileIndex: Int): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                httpPost("$baseUrl/api/tv/sessions/$sessionId/select", """{"index":$fileIndex}""")
                true
            }.getOrDefault(false)
        }

    private fun buildCandidateUrls(primaryUrl: String, fallbackUrl: String): List<String> {
        val list = mutableListOf<String>()
        val cleanPrimary = primaryUrl.trim().removeSuffix("/")
        if (cleanPrimary.isNotEmpty()) list.add(cleanPrimary)
        val cleanFallback = fallbackUrl.trim().removeSuffix("/")
        if (cleanFallback.isNotEmpty() && cleanFallback !in list) list.add(cleanFallback)
        return list
    }

    private fun httpGet(urlStr: String, timeoutMs: Int): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun httpPost(urlStr: String, jsonBody: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 3500
            readTimeout = 3500
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(jsonBody) }
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
