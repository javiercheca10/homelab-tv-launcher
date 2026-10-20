package app.homelab.tvlauncher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
private data class JellyfinUserDto(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String = ""
)

@Serializable
private data class JellyfinResumeEnvelope(
    @SerialName("Items") val items: List<JellyfinItemDto> = emptyList()
)

class JellyfinRepository {

    @Volatile
    private var cachedUserId: String = ""

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun fetchLibrary(settings: LauncherSettings): Result<Pair<List<JellyfinMediaItem>, List<JellyfinMediaItem>>> =
        withContext(Dispatchers.IO) {
            if (settings.jellyfinApiKey.isBlank()) {
                return@withContext Result.failure(IllegalStateException("API Key de Jellyfin no configurada"))
            }

            val candidates = buildCandidateUrls(settings.jellyfinUrl, settings.tailscaleIp)
            if (candidates.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("URL de Jellyfin no configurada"))
            }

            var lastError: Throwable? = null

            for (baseUrl in candidates) {
                try {
                    val apiKey = settings.jellyfinApiKey
                    var userId = settings.jellyfinUserId.ifBlank { cachedUserId }

                    if (userId.isBlank()) {
                        val usersJson = httpGet("$baseUrl/Users", apiKey)
                        val users = json.decodeFromString(ListSerializer(JellyfinUserDto.serializer()), usersJson)
                        userId = users.firstOrNull { it.id.isNotBlank() }?.id ?: ""
                        if (userId.isNotBlank()) {
                            cachedUserId = userId
                        }
                    }

                    if (userId.isBlank()) {
                        throw IllegalStateException("No se pudo obtener ningún usuario de Jellyfin")
                    }

                    val resumeJson = httpGet(
                        "$baseUrl/Users/$userId/Items/Resume?Limit=12&Fields=Overview,PrimaryImageAspectRatio,UserData,ParentId&MediaTypes=Video",
                        apiKey
                    )
                    val resumeEnvelope = json.decodeFromString(JellyfinResumeEnvelope.serializer(), resumeJson)
                    val resumeItems = resumeEnvelope.items.map { mapDto(it, baseUrl, apiKey) }

                    val latestJson = httpGet(
                        "$baseUrl/Users/$userId/Items/Latest?Limit=16&Fields=Overview,PrimaryImageAspectRatio,UserData,ParentId&IncludeItemTypes=Movie,Episode",
                        apiKey
                    )
                    val latestDtos = json.decodeFromString(ListSerializer(JellyfinItemDto.serializer()), latestJson)
                    val latestItems = latestDtos.map { mapDto(it, baseUrl, apiKey) }

                    return@withContext Result.success(resumeItems to latestItems)
                } catch (t: Throwable) {
                    lastError = t
                }
            }

            Result.failure(lastError ?: IllegalStateException("Jellyfin no disponible"))
        }

    private fun mapDto(dto: JellyfinItemDto, baseUrl: String, apiKey: String): JellyfinMediaItem {
        val isEpisode = dto.type.equals("Episode", ignoreCase = true)
        val displayTitle = if (isEpisode && !dto.seriesName.isNullOrBlank()) {
            dto.seriesName
        } else {
            dto.name
        }

        val subtitle = when {
            isEpisode -> {
                val se = if (dto.seasonNumber != null && dto.episodeNumber != null) {
                    "T${dto.seasonNumber}:E${dto.episodeNumber} · "
                } else ""
                "$se${dto.name}"
            }
            dto.productionYear != null -> "Película · ${dto.productionYear}"
            else -> "Jellyfin · Biblioteca Local"
        }

        val backdropId = when {
            dto.backdropImageTags.isNotEmpty() -> dto.id
            !dto.parentBackdropItemId.isNullOrBlank() -> dto.parentBackdropItemId
            else -> dto.id
        }

        val posterUrl = "$baseUrl/Items/${dto.id}/Images/Primary?maxHeight=480&quality=85"
        val backdropUrl = "$baseUrl/Items/$backdropId/Images/Backdrop/0?maxWidth=1920&quality=85"
        val directStreamUrl = "$baseUrl/Videos/${dto.id}/stream?static=true&api_key=$apiKey"

        val resumeMs = (dto.userData?.playbackPositionTicks ?: 0L) / 10_000L
        val durationMs = (dto.runTimeTicks ?: 0L) / 10_000L
        val progress = when {
            dto.userData?.playedPercentage != null -> (dto.userData.playedPercentage / 100.0).toFloat().coerceIn(0f, 1f)
            durationMs > 0L -> (resumeMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            else -> 0f
        }

        return JellyfinMediaItem(
            id = dto.id,
            title = displayTitle,
            subtitle = subtitle,
            overview = dto.overview?.ifBlank { "Disponible en alta definición desde el servidor Jellyfin." }
                ?: "Disponible en alta definición desde el servidor Jellyfin.",
            year = dto.productionYear,
            type = dto.type,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            directStreamUrl = directStreamUrl,
            resumePositionMs = resumeMs,
            durationMs = durationMs,
            progressFraction = progress
        )
    }

    private fun buildCandidateUrls(primaryUrl: String, fallbackUrl: String): List<String> {
        val list = mutableListOf<String>()
        val normPrimary = normalizeUrl(primaryUrl, -1)
        val inferredPort = normPrimary?.let { runCatching { URL(it).port }.getOrDefault(-1) } ?: -1
        if (normPrimary != null) list.add(normPrimary)
        normalizeUrl(fallbackUrl, inferredPort)?.let { if (it !in list) list.add(it) }
        return list
    }

    private fun normalizeUrl(raw: String, defaultPort: Int): String? {
        val trimmed = raw.trim().removeSuffix("/")
        if (trimmed.isEmpty()) return null
        val withProtocol = if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            "http://$trimmed"
        } else {
            trimmed
        }
        val uri = runCatching { URL(withProtocol) }.getOrNull() ?: return withProtocol
        val isHttps = uri.protocol.equals("https", ignoreCase = true)
        return if (!isHttps && uri.port == -1 && defaultPort > 0 && uri.host != null && !uri.host.contains(":")) {
            val path = uri.path ?: ""
            "http://${uri.host}:$defaultPort$path"
        } else {
            withProtocol
        }
    }

    private fun httpGet(urlStr: String, apiKey: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 3000
            readTimeout = 3000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-Emby-Token", apiKey)
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
