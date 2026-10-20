package app.homelab.tvlauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.provider.Settings
import app.homelab.tvlauncher.BuildConfig

class AppRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("homelab_tv_launcher_prefs", Context.MODE_PRIVATE)
    private val pm: PackageManager = context.packageManager

    companion object {
        private const val KEY_FAVORITES = "favorite_packages"
        private const val KEY_HIDDEN = "hidden_packages"
        private const val KEY_ORDER = "ordered_packages"
        private const val KEY_INIT_FAVS = "initialized_default_favorites"

        private val DEFAULT_FAVORITE_CANDIDATES = listOf(
            "org.jellyfin.androidtv",
            "com.liskovsoft.smarttubetv.beta",
            "com.liskovsoft.smarttubetv",
            "org.smarttube.stable",
            "com.google.android.youtube.tv",
            "com.netflix.ninja",
            "org.videolan.vlc",
            "org.xbmc.kodi",
            "com.amazon.amazonvideo.livingroom",
            "com.disney.disneyplus",
            "com.spotify.tv.android",
            "tv.twitch.android.app"
        )

        private val ALWAYS_EXCLUDED = setOf(
            "app.homelab.tvlauncher",
            "com.google.android.tvlauncher",
            "com.google.android.apps.tv.launcherx",
            "com.google.android.tungsten.setupwraith",
            "com.google.android.tvrecommendations"
        )
    }

    fun loadApps(): Pair<List<TvAppInfo>, List<TvAppInfo>> {
        val discovered = linkedMapOf<String, ResolveInfo>()

        val leanbackIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        for (info in pm.queryIntentActivities(leanbackIntent, 0)) {
            val pkg = info.activityInfo?.packageName ?: continue
            if (pkg !in ALWAYS_EXCLUDED) {
                discovered[pkg] = info
            }
        }

        val standardIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        for (info in pm.queryIntentActivities(standardIntent, 0)) {
            val pkg = info.activityInfo?.packageName ?: continue
            if (pkg !in ALWAYS_EXCLUDED && pkg !in discovered) {
                discovered[pkg] = info
            }
        }

        // Initialize default favorites on first run
        if (!prefs.getBoolean(KEY_INIT_FAVS, false)) {
            val initialFavs = DEFAULT_FAVORITE_CANDIDATES.filter { it in discovered }.toSet()
            prefs.edit()
                .putStringSet(KEY_FAVORITES, initialFavs)
                .putBoolean(KEY_INIT_FAVS, true)
                .apply()
        }

        val favoritesSet = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        val hiddenSet = prefs.getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet()
        val savedOrder = getOrderedList()

        val allMapped = discovered.values.mapNotNull { info ->
            val actInfo = info.activityInfo ?: return@mapNotNull null
            val pkg = actInfo.packageName
            val label = info.loadLabel(pm)?.toString()?.ifBlank { pkg } ?: pkg
            val banner = actInfo.loadBanner(pm) ?: actInfo.applicationInfo?.loadBanner(pm)
            val icon = info.loadIcon(pm) ?: actInfo.applicationInfo?.loadIcon(pm)
            val isSystem = (actInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val theme = AppBackdropCatalog.resolveForPackage(pkg, label, banner ?: icon)

            TvAppInfo(
                packageName = pkg,
                activityName = actInfo.name,
                label = label,
                icon = icon,
                banner = banner,
                isFavorite = pkg in favoritesSet,
                isSystemApp = isSystem,
                dominantColor = theme.primaryColor,
                accentColor = theme.accentColor,
                curatedBackdropUrl = theme.backdropUrl,
                curatedSubtitle = theme.subtitle,
                curatedDescription = theme.description
            )
        }

        val orderMap = savedOrder.withIndex().associate { it.value to it.index }
        val sortedAll = allMapped.sortedWith(
            compareBy<TvAppInfo> { orderMap[it.packageName] ?: Int.MAX_VALUE }
                .thenBy { it.label.lowercase() }
        )

        val visibleApps = sortedAll.filter { it.packageName !in hiddenSet }
        val hiddenApps = sortedAll.filter { it.packageName in hiddenSet }
        return visibleApps to hiddenApps
    }

    fun toggleFavorite(packageName: String) {
        val current = (prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()).toMutableSet()
        if (!current.add(packageName)) {
            current.remove(packageName)
        }
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
    }

    fun setHidden(packageName: String, hidden: Boolean) {
        val current = (prefs.getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet()).toMutableSet()
        if (hidden) current.add(packageName) else current.remove(packageName)
        prefs.edit().putStringSet(KEY_HIDDEN, current).apply()
    }

    fun moveApp(packageName: String, direction: Int, currentPackages: List<String>) {
        val mutable = currentPackages.toMutableList()
        val idx = mutable.indexOf(packageName)
        if (idx == -1) return
        val newIdx = (idx + direction).coerceIn(0, mutable.lastIndex)
        if (newIdx == idx) return
        mutable.removeAt(idx)
        mutable.add(newIdx, packageName)
        prefs.edit().putString(KEY_ORDER, mutable.joinToString(",")).apply()
    }

    private fun getOrderedList(): List<String> =
        prefs.getString(KEY_ORDER, "")
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    fun launchApp(app: TvAppInfo): Boolean {
        return try {
            val leanback = pm.getLeanbackLaunchIntentForPackage(app.packageName)
            val fallback = pm.getLaunchIntentForPackage(app.packageName)
            val explicit = Intent(Intent.ACTION_MAIN).apply {
                component = ComponentName(app.packageName, app.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val intent = (leanback ?: fallback ?: explicit).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun openAppSystemSettings(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Throwable) {
        }
    }

    fun openSystemSettings() {
        try {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Throwable) {
        }
    }

    fun openWifiSettings() {
        try {
            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Throwable) {
            openSystemSettings()
        }
    }

    fun openTvInputs() {
        val candidateIntents = listOf(
            Intent("com.android.tv.action.VIEW_INPUTS"),
            Intent("android.media.tv.action.SETUP_INPUTS"),
            pm.getLeanbackLaunchIntentForPackage("com.google.android.tv")
        ).filterNotNull()

        for (intent in candidateIntents) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: Throwable) {
            }
        }
        openSystemSettings()
    }

    fun loadSettings(): LauncherSettings {
        val prefPlayerName = prefs.getString("preferred_player", PreferredPlayer.ASK.name) ?: PreferredPlayer.ASK.name
        val player = runCatching { PreferredPlayer.valueOf(prefPlayerName) }.getOrDefault(PreferredPlayer.ASK)
        return LauncherSettings(
            jellyfinUrl = prefs.getString("jellyfin_url", BuildConfig.DEFAULT_JELLYFIN_URL) ?: BuildConfig.DEFAULT_JELLYFIN_URL,
            jellyfinUserId = prefs.getString("jellyfin_user_id", BuildConfig.DEFAULT_JELLYFIN_USER_ID) ?: BuildConfig.DEFAULT_JELLYFIN_USER_ID,
            jellyfinApiKey = prefs.getString("jellyfin_api_key", BuildConfig.DEFAULT_JELLYFIN_API_KEY) ?: BuildConfig.DEFAULT_JELLYFIN_API_KEY,
            torrentStreamUrl = prefs.getString("torrent_stream_url", BuildConfig.DEFAULT_TORRENT_STREAM_URL) ?: BuildConfig.DEFAULT_TORRENT_STREAM_URL,
            tailscaleIp = prefs.getString("tailscale_ip", BuildConfig.DEFAULT_TAILSCALE_IP) ?: BuildConfig.DEFAULT_TAILSCALE_IP,
            preferredPlayer = player
        )
    }

    fun saveSettings(settings: LauncherSettings) {
        prefs.edit()
            .putString("jellyfin_url", settings.jellyfinUrl.trim().removeSuffix("/"))
            .putString("jellyfin_user_id", settings.jellyfinUserId.trim())
            .putString("jellyfin_api_key", settings.jellyfinApiKey.trim())
            .putString("torrent_stream_url", settings.torrentStreamUrl.trim().removeSuffix("/"))
            .putString("tailscale_ip", settings.tailscaleIp.trim())
            .putString("preferred_player", settings.preferredPlayer.name)
            .apply()
    }
}
