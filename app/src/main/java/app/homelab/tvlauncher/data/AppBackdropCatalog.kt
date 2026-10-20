package app.homelab.tvlauncher.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette

data class CuratedAppTheme(
    val primaryColor: Color,
    val accentColor: Color,
    val subtitle: String,
    val description: String,
    val backdropUrl: String? = null
)

object AppBackdropCatalog {

    private val catalog = listOf(
        // Netflix
        listOf("com.netflix.ninja", "com.netflix.mediaclient") to CuratedAppTheme(
            primaryColor = Color(0xFF1A1214),
            accentColor = Color(0xFFD4A373),
            subtitle = "Streaming · Cine y Series Originales",
            description = "Acceso directo a Netflix en 4K HDR sin pasar por el menú promocional de Google TV.",
            backdropUrl = "https://images.unsplash.com/photo-1574375927938-d5a98e8ffe85?auto=format&fit=crop&w=1920&q=80"
        ),
        // YouTube & SmartTube
        listOf(
            "com.google.android.youtube.tv",
            "com.liskovsoft.smarttubetv",
            "com.liskovsoft.smarttubetv.beta",
            "org.smarttube.stable",
            "com.teamsmart.videomanager.tv"
        ) to CuratedAppTheme(
            primaryColor = Color(0xFF1A1313),
            accentColor = Color(0xFFE07A5F),
            subtitle = "Vídeo bajo demanda · Suscripciones y Directos",
            description = "Reproducción fluida de canales, suscripciones y vídeos en alta definición hasta 4K 60fps.",
            backdropUrl = "https://images.unsplash.com/photo-1611162616475-46b635cb6868?auto=format&fit=crop&w=1920&q=80"
        ),
        // Jellyfin
        listOf("org.jellyfin.androidtv", "org.jellyfin.mobile") to CuratedAppTheme(
            primaryColor = Color(0xFF0E141D),
            accentColor = Color(0xFF7A9BB8),
            subtitle = "Servidor Multimedia Local · Biblioteca en Red",
            description = "Biblioteca privada de cine y series servida en red local sin compresión.",
            backdropUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=1920&q=80"
        ),
        // VLC
        listOf("org.videolan.vlc") to CuratedAppTheme(
            primaryColor = Color(0xFF1A1510),
            accentColor = Color(0xFFD4A373),
            subtitle = "Reproductor Universal · MKV / HTTP Range / Subtítulos",
            description = "Motor de reproducción directa compatible con streams HTTP de Torrent Stream Web y red local.",
            backdropUrl = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&fit=crop&w=1920&q=80"
        ),
        // Kodi
        listOf("org.xbmc.kodi") to CuratedAppTheme(
            primaryColor = Color(0xFF10161C),
            accentColor = Color(0xFF82A6B3),
            subtitle = "Centro Multimedia · Home Theater",
            description = "Centro de cine en casa avanzado con passthrough de audio HD y complementos de red.",
            backdropUrl = "https://images.unsplash.com/photo-1595769816263-9b910be24d5f?auto=format&fit=crop&w=1920&q=80"
        ),
        // Prime Video
        listOf("com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient") to CuratedAppTheme(
            primaryColor = Color(0xFF0F161C),
            accentColor = Color(0xFF88A9BF),
            subtitle = "Amazon Prime Video · Cine y Deportes",
            description = "Catálogo de películas, series y retransmisiones en directo de Amazon Prime Video.",
            backdropUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=1920&q=80"
        ),
        // Disney+
        listOf("com.disney.disneyplus") to CuratedAppTheme(
            primaryColor = Color(0xFF10131C),
            accentColor = Color(0xFF7B8DB8),
            subtitle = "Disney · Pixar · Marvel · Star Wars",
            description = "Streaming en formato IMAX Enhanced y Dolby Vision.",
            backdropUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=1920&q=80"
        ),
        // Max / HBO
        listOf("com.wbd.stream", "com.hbo.hbonow") to CuratedAppTheme(
            primaryColor = Color(0xFF14111C),
            accentColor = Color(0xFF9E8DB8),
            subtitle = "HBO · Warner Bros · Cine de Estreno",
            description = "Series originales de HBO y estrenos cinematográficos de Warner Bros.",
            backdropUrl = "https://images.unsplash.com/photo-1478720568477-152d9b164e26?auto=format&fit=crop&w=1920&q=80"
        ),
        // Spotify
        listOf("com.spotify.tv.android", "com.spotify.music") to CuratedAppTheme(
            primaryColor = Color(0xFF101713),
            accentColor = Color(0xFF7BA888),
            subtitle = "Música y Podcasts · Spotify Connect",
            description = "Reproduce música en el sistema de sonido del salón o controla la reproducción desde el móvil.",
            backdropUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=1920&q=80"
        ),
        // Twitch
        listOf("tv.twitch.android.app", "tv.twitch.android.viewer", "com.fgl27.twitch") to CuratedAppTheme(
            primaryColor = Color(0xFF14121A),
            accentColor = Color(0xFF9E8FB3),
            subtitle = "Streaming en Directo · Gaming y Eventos",
            description = "Canales en vivo y retransmisiones de eSports a pantalla completa.",
            backdropUrl = "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=1920&q=80"
        ),
        // Moonlight / Steam Link
        listOf("com.limelight", "com.valvesoftware.steamlink") to CuratedAppTheme(
            primaryColor = Color(0xFF11151A),
            accentColor = Color(0xFF8FA9BA),
            subtitle = "Game Streaming · Baja Latencia en Red Local",
            description = "Juega en el salón transmitiendo por red Gigabit con decodificación por hardware.",
            backdropUrl = "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?auto=format&fit=crop&w=1920&q=80"
        ),
        // Stremio / Plex / TiviMate
        listOf("com.stremio.one", "com.plexapp.android", "ar.tvplayer.tv") to CuratedAppTheme(
            primaryColor = Color(0xFF14111B),
            accentColor = Color(0xFF9F8DBF),
            subtitle = "Centro de Medios y Televisión en Vivo",
            description = "Reproducción directa de canales y bibliotecas multimedia.",
            backdropUrl = "https://images.unsplash.com/photo-1522869635100-9f4c5e86aa37?auto=format&fit=crop&w=1920&q=80"
        )
    )

    fun resolveForPackage(packageName: String, label: String, drawable: Drawable?): CuratedAppTheme {
        val pkgLower = packageName.lowercase()
        val labelLower = label.lowercase()

        for ((keys, theme) in catalog) {
            if (keys.any { pkgLower.contains(it) }) {
                return theme
            }
        }

        // Match by name heuristic if package name varies by region/OEM
        when {
            labelLower.contains("netflix") -> return catalog[0].second
            labelLower.contains("youtube") || labelLower.contains("smarttube") -> return catalog[1].second
            labelLower.contains("jellyfin") -> return catalog[2].second
            labelLower.contains("vlc") -> return catalog[3].second
            labelLower.contains("kodi") -> return catalog[4].second
            labelLower.contains("prime video") -> return catalog[5].second
            labelLower.contains("disney") -> return catalog[6].second
            labelLower.contains("spotify") -> return catalog[8].second
            labelLower.contains("twitch") -> return catalog[9].second
            pkgLower.contains("tvinput") -> return CuratedAppTheme(
                primaryColor = Color(0xFF10141C),
                accentColor = Color(0xFF88A9BA),
                subtitle = "Entradas de Televisión · HDMI / Sintonizador",
                description = "Cambio rápido de fuente HDMI, consola o sintonizador de televisión.",
                backdropUrl = null
            )
        }

        // Dynamic palette extraction from the app's own banner or icon
        val extracted = extractColors(drawable)
        return CuratedAppTheme(
            primaryColor = extracted.first,
            accentColor = extracted.second,
            subtitle = "Aplicación Android TV · Inicio Rápido",
            description = "Pulsa OK para abrir $label o mantén pulsado OK para fijar en Favoritas, mover u ocultar.",
            backdropUrl = null
        )
    }

    private fun extractColors(drawable: Drawable?): Pair<Color, Color> {
        if (drawable == null) return Color(0xFF12141B) to Color(0xFFD4A373)
        return try {
            val width = drawable.intrinsicWidth.coerceIn(32, 160)
            val height = drawable.intrinsicHeight.coerceIn(32, 160)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            val palette = Palette.from(bitmap).maximumColorCount(10).generate()
            val dark = palette.getDarkMutedColor(palette.getDarkVibrantColor(0xFF12141B.toInt()))
            val accent = palette.getMutedColor(palette.getLightMutedColor(0xFFD4A373.toInt()))
            bitmap.recycle()
            Color(dark) to Color(accent)
        } catch (_: Throwable) {
            Color(0xFF12141B) to Color(0xFFD4A373)
        }
    }
}
