package app.homelab.tvlauncher.data

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class TvAppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: Drawable?,
    val banner: Drawable?,
    val isFavorite: Boolean = false,
    val isSystemApp: Boolean = false,
    val dominantColor: Color = Color(0xFF14161D),
    val accentColor: Color = Color(0xFFD4A373),
    val curatedBackdropUrl: String? = null,
    val curatedSubtitle: String? = null,
    val curatedDescription: String? = null
)

@Serializable
data class TorrentFile(
    val id: Int = 0,
    val path: String = "",
    val length: Long = 0L
) {
    val fileName: String
        get() = path.substringAfterLast('/')
}

@Serializable
data class TorrentSession(
    val id: String,
    val title: String = "Torrent Stream",
    val state: String = "ready",
    val created: Double = 0.0,
    @SerialName("expires_at") val expiresAt: Double = 0.0,
    @SerialName("active_streams") val activeStreams: Int = 0,
    val paused: Boolean = false,
    @SerialName("paused_until") val pausedUntil: Double? = null,
    @SerialName("buffered_bytes") val bufferedBytes: Long = 0L,
    @SerialName("buffer_target") val bufferTarget: Long = 1L,
    @SerialName("buffer_ready") val bufferReady: Boolean = false,
    @SerialName("prefetched_bytes") val prefetchedBytes: Long = 0L,
    @SerialName("play_url") val playUrl: String = "",
    val selected: Int? = null,
    val files: List<TorrentFile> = emptyList()
) {
    val bufferProgress: Float
        get() = if (bufferReady || bufferTarget <= 0L) 1f else (bufferedBytes.toFloat() / bufferTarget.toFloat()).coerceIn(0f, 1f)

    val selectedFile: TorrentFile?
        get() = files.firstOrNull { it.id == selected } ?: files.maxByOrNull { it.length }
}

@Serializable
data class JellyfinUserData(
    @SerialName("PlaybackPositionTicks") val playbackPositionTicks: Long = 0L,
    @SerialName("PlayedPercentage") val playedPercentage: Double? = null,
    @SerialName("Played") val played: Boolean = false
)

@Serializable
data class JellyfinItemDto(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String,
    @SerialName("SeriesName") val seriesName: String? = null,
    @SerialName("ParentIndexNumber") val seasonNumber: Int? = null,
    @SerialName("IndexNumber") val episodeNumber: Int? = null,
    @SerialName("ProductionYear") val productionYear: Int? = null,
    @SerialName("Overview") val overview: String? = null,
    @SerialName("Type") val type: String = "Movie",
    @SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerialName("UserData") val userData: JellyfinUserData? = null,
    @SerialName("ParentBackdropItemId") val parentBackdropItemId: String? = null,
    @SerialName("BackdropImageTags") val backdropImageTags: List<String> = emptyList(),
    @SerialName("ParentBackdropImageTags") val parentBackdropImageTags: List<String> = emptyList()
)

data class JellyfinMediaItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val overview: String,
    val year: Int?,
    val type: String,
    val posterUrl: String,
    val backdropUrl: String,
    val directStreamUrl: String,
    val resumePositionMs: Long,
    val durationMs: Long,
    val progressFraction: Float
)

data class HeroPreview(
    val id: String,
    val title: String,
    val badgeText: String,
    val subtitle: String,
    val description: String,
    val backdropUrl: String? = null,
    val fallbackDrawable: Drawable? = null,
    val primaryColor: Color = Color(0xFF0D0E11),
    val accentColor: Color = Color(0xFFD4A373),
    val progressFraction: Float? = null
)

data class LauncherSettings(
    val jellyfinUrl: String,
    val jellyfinUserId: String,
    val jellyfinApiKey: String,
    val torrentStreamUrl: String,
    val tailscaleIp: String,
    val preferredPlayer: PreferredPlayer = PreferredPlayer.ASK
)

enum class PreferredPlayer(val label: String) {
    ASK("Preguntar al abrir"),
    VLC("VLC Media Player"),
    KODI("Kodi"),
    EXOPLAYER("Reproductor Integrado"),
    JELLYFIN("App Oficial Jellyfin")
}

@Serializable
data class AppUpdateInfo(
    val available: Boolean = false,
    @SerialName("version_code") val versionCode: Int = 0,
    @SerialName("version_name") val versionName: String = "",
    val sha256: String = "",
    @SerialName("size_bytes") val sizeBytes: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
    @SerialName("download_url") val downloadUrl: String = ""
)
