package app.homelab.tvlauncher.ui

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import app.homelab.tvlauncher.data.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    favoriteApps: List<TvAppInfo>,
    allApps: List<TvAppInfo>,
    hiddenApps: List<TvAppInfo>,
    torrentSessions: List<TorrentSession>,
    jellyfinResume: List<JellyfinMediaItem>,
    jellyfinLatest: List<JellyfinMediaItem>,
    serverOnline: Boolean,
    settings: LauncherSettings,
    availableUpdate: AppUpdateInfo? = null,
    updateDownloadProgress: Int? = null,
    onInstallUpdate: () -> Unit = {},
    onLaunchApp: (TvAppInfo) -> Unit,
    onToggleFavorite: (TvAppInfo) -> Unit,
    onMoveApp: (TvAppInfo, Int, Boolean) -> Unit,
    onHideApp: (TvAppInfo, Boolean) -> Unit,
    onOpenAppSettings: (TvAppInfo) -> Unit,
    onOpenTvInputs: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onSaveSettings: (LauncherSettings) -> Unit,
    onRefreshNow: () -> Unit,
    onTorrentPauseResume: (TorrentSession) -> Unit,
    onTorrentSelectFile: (TorrentSession, Int) -> Unit
) {
    val context = LocalContext.current
    val initialFocusRequester = remember { FocusRequester() }
    var initialFocusDone by remember { mutableStateOf(false) }

    var currentHero by remember {
        mutableStateOf(
            HeroPreview(
                id = "welcome",
                title = "Homelab TV",
                badgeText = "CINEMATIC EDITION",
                subtitle = "Android TV · Colección y Streaming Local",
                description = "Navega con la cruceta por tus aplicaciones destacadas, sesiones en curso de Torrent Stream y tu catálogo de Jellyfin.",
                primaryColor = EditorialTheme.Ebony,
                accentColor = EditorialTheme.Amber
            )
        )
    }

    var selectedAppForMenu by remember { mutableStateOf<TvAppInfo?>(null) }
    var selectedAppInFavRow by remember { mutableStateOf(false) }
    var selectedTorrentForDialog by remember { mutableStateOf<TorrentSession?>(null) }
    var selectedJellyfinForDialog by remember { mutableStateOf<JellyfinMediaItem?>(null) }
    var showHiddenAppsDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Set initial hero and request initial D-Pad focus on the first card when loaded
    LaunchedEffect(favoriteApps.isNotEmpty(), allApps.isNotEmpty()) {
        if (currentHero.id == "welcome") {
            val first = favoriteApps.firstOrNull() ?: allApps.firstOrNull()
            if (first != null) {
                currentHero = appToHero(first, isFavRow = favoriteApps.isNotEmpty())
            }
        }
        if (!initialFocusDone && (favoriteApps.isNotEmpty() || allApps.isNotEmpty())) {
            initialFocusDone = true
            delay(80L)
            runCatching { initialFocusRequester.requestFocus() }
        }
    }

    // Keep Hero Banner progress and buffer stats live when focused on an active Torrent session
    LaunchedEffect(torrentSessions) {
        if (currentHero.id.startsWith("ts_")) {
            val activeId = currentHero.id.removePrefix("ts_")
            torrentSessions.firstOrNull { it.id == activeId }?.let { updatedSession ->
                currentHero = torrentToHero(updatedSession)
            }
        }
    }

    // Auto-close HiddenAppsDialog when all hidden apps have been restored
    LaunchedEffect(hiddenApps.size) {
        if (hiddenApps.isEmpty() && showHiddenAppsDialog) {
            showHiddenAppsDialog = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorialTheme.Obsidian)
    ) {
        // 1. Full-screen Immersive Editorial Backdrop
        ImmersiveBackdrop(hero = currentHero)

        // 2. 60/40 Vertical Layout: Top 55-60% open for artwork & hero typography, bottom 40-45% for carousels
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 18.dp)
        ) {
            TopNavigationBar(
                serverOnline = serverOnline,
                hiddenCount = hiddenApps.size,
                availableUpdate = availableUpdate,
                updateDownloadProgress = updateDownloadProgress,
                onInstallUpdate = onInstallUpdate,
                onOpenTvInputs = onOpenTvInputs,
                onOpenHiddenApps = { showHiddenAppsDialog = true },
                onOpenServerSettings = { showSettingsDialog = true },
                onOpenWifi = onOpenWifiSettings,
                onOpenAndroidSettings = onOpenSystemSettings,
                onFocusHero = { currentHero = it }
            )

            // Cinematic Editorial Hero Banner (Top 55-60% area)
            EditorialHeroBanner(
                hero = currentHero,
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .height(200.dp)
                    .padding(start = 48.dp, top = 8.dp, bottom = 4.dp)
            )

            // 4 Horizontal Carousel Rows (1 fully active in viewport + next row partially peeking from below)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(top = 6.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // ROW 1: FAVORITAS
                if (favoriteApps.isNotEmpty()) {
                    item(key = "row_favorites") {
                        SelectorRowSection(title = "Favoritas", accent = EditorialTheme.Amber) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 48.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                itemsIndexed(favoriteApps, key = { _, app -> "fav_${app.packageName}" }) { idx, app ->
                                    AppSelectorCard(
                                        app = app,
                                        cardWidth = 160.dp,
                                        cardHeight = 96.dp,
                                        modifier = if (idx == 0) Modifier.focusRequester(initialFocusRequester) else Modifier,
                                        onFocused = { currentHero = appToHero(app, isFavRow = true) },
                                        onClick = { onLaunchApp(app) },
                                        onLongClick = {
                                            selectedAppInFavRow = true
                                            selectedAppForMenu = app
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ROW 2: SEGUIR VIENDO (Torrent Stream Web Activo + Jellyfin Resume)
                if (torrentSessions.isNotEmpty() || jellyfinResume.isNotEmpty()) {
                    item(key = "row_continue_watching") {
                        SelectorRowSection(
                            title = "Seguir viendo",
                            subtitleBadge = if (torrentSessions.isNotEmpty()) "${torrentSessions.size} Torrent activo" else null,
                            accent = EditorialTheme.Amber
                        ) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 48.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Active Torrent Stream Web sessions first
                                items(torrentSessions, key = { "ts_${it.id}" }) { session ->
                                    TorrentSessionCard(
                                        session = session,
                                        onFocused = { currentHero = torrentToHero(session) },
                                        onClick = {
                                            handlePlayTorrent(
                                                context = context,
                                                session = session,
                                                preferredPlayer = settings.preferredPlayer,
                                                onAskDialog = { selectedTorrentForDialog = session }
                                            )
                                        },
                                        onLongClick = { selectedTorrentForDialog = session }
                                    )
                                }

                                // Jellyfin Resume items
                                items(jellyfinResume, key = { "resume_${it.id}" }) { media ->
                                    JellyfinMediaCard(
                                        item = media,
                                        isResume = true,
                                        onFocused = { currentHero = jellyfinToHero(media, isResume = true) },
                                        onClick = {
                                            handlePlayJellyfin(
                                                context = context,
                                                item = media,
                                                preferredPlayer = settings.preferredPlayer,
                                                onAskDialog = { selectedJellyfinForDialog = media }
                                            )
                                        },
                                        onLongClick = { selectedJellyfinForDialog = media }
                                    )
                                }
                            }
                        }
                    }
                }

                // ROW 3: APLICACIONES
                item(key = "row_all_apps") {
                    SelectorRowSection(
                        title = "Aplicaciones",
                        subtitleBadge = "Mantén OK para opciones",
                        accent = EditorialTheme.Amber
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 48.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(allApps, key = { _, app -> "app_${app.packageName}" }) { idx, app ->
                                AppSelectorCard(
                                    app = app,
                                    cardWidth = 160.dp,
                                    cardHeight = 96.dp,
                                    modifier = if (favoriteApps.isEmpty() && idx == 0) Modifier.focusRequester(initialFocusRequester) else Modifier,
                                    onFocused = { currentHero = appToHero(app, isFavRow = false) },
                                    onClick = { onLaunchApp(app) },
                                    onLongClick = {
                                        selectedAppInFavRow = false
                                        selectedAppForMenu = app
                                    }
                                )
                            }
                        }
                    }
                }

                // ROW 4: RECIENTES EN JELLYFIN
                if (jellyfinLatest.isNotEmpty()) {
                    item(key = "row_jellyfin_latest") {
                        SelectorRowSection(
                            title = "Recientes en Jellyfin",
                            subtitleBadge = "Biblioteca",
                            accent = EditorialTheme.Amber
                        ) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 48.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(jellyfinLatest, key = { "latest_${it.id}" }) { media ->
                                    JellyfinMediaCard(
                                        item = media,
                                        isResume = false,
                                        onFocused = { currentHero = jellyfinToHero(media, isResume = false) },
                                        onClick = {
                                            handlePlayJellyfin(
                                                context = context,
                                                item = media,
                                                preferredPlayer = settings.preferredPlayer,
                                                onAskDialog = { selectedJellyfinForDialog = media }
                                            )
                                        },
                                        onLongClick = { selectedJellyfinForDialog = media }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialogs
        selectedAppForMenu?.let { app ->
            AppOptionsDialog(
                app = app,
                onDismiss = { selectedAppForMenu = null },
                onToggleFavorite = {
                    onToggleFavorite(app)
                    selectedAppForMenu = null
                },
                onMoveLeft = {
                    onMoveApp(app, -1, selectedAppInFavRow)
                },
                onMoveRight = {
                    onMoveApp(app, 1, selectedAppInFavRow)
                },
                onHideApp = {
                    onHideApp(app, true)
                    selectedAppForMenu = null
                },
                onOpenSystemInfo = {
                    onOpenAppSettings(app)
                    selectedAppForMenu = null
                }
            )
        }

        selectedTorrentForDialog?.let { session ->
            TorrentActionDialog(
                session = session,
                onDismiss = { selectedTorrentForDialog = null },
                onPlayVlc = {
                    ExternalPlayerLauncher.openInVlc(context, session.playUrl, session.title)
                    selectedTorrentForDialog = null
                },
                onPlayKodi = {
                    ExternalPlayerLauncher.openInKodi(context, session.playUrl, session.title)
                    selectedTorrentForDialog = null
                },
                onPlayInternal = {
                    PlayerActivity.launch(context, session.playUrl, session.title)
                    selectedTorrentForDialog = null
                },
                onTogglePause = {
                    onTorrentPauseResume(session)
                    selectedTorrentForDialog = null
                },
                onSelectFile = { fileIndex ->
                    onTorrentSelectFile(session, fileIndex)
                    selectedTorrentForDialog = null
                }
            )
        }

        selectedJellyfinForDialog?.let { item ->
            JellyfinActionDialog(
                item = item,
                onDismiss = { selectedJellyfinForDialog = null },
                onOpenJellyfinApp = {
                    if (!ExternalPlayerLauncher.openJellyfinApp(context)) {
                        ExternalPlayerLauncher.openInVlc(context, item.directStreamUrl, item.title, item.resumePositionMs)
                    }
                    selectedJellyfinForDialog = null
                },
                onPlayVlc = {
                    ExternalPlayerLauncher.openInVlc(context, item.directStreamUrl, item.title, item.resumePositionMs)
                    selectedJellyfinForDialog = null
                },
                onPlayKodi = {
                    ExternalPlayerLauncher.openInKodi(context, item.directStreamUrl, item.title)
                    selectedJellyfinForDialog = null
                },
                onPlayInternal = {
                    PlayerActivity.launch(context, item.directStreamUrl, item.title, item.resumePositionMs)
                    selectedJellyfinForDialog = null
                }
            )
        }

        if (showHiddenAppsDialog) {
            HiddenAppsDialog(
                hiddenApps = hiddenApps,
                onRestoreApp = { onHideApp(it, false) },
                onDismiss = { showHiddenAppsDialog = false }
            )
        }

        if (showSettingsDialog) {
            ServerSettingsDialog(
                current = settings,
                onSave = {
                    onSaveSettings(it)
                    showSettingsDialog = false
                    onRefreshNow()
                },
                onInstallUpdate = {
                    showSettingsDialog = false
                    onInstallUpdate()
                },
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

private object EditorialTheme {
    val Obsidian = Color(0xFF090A0C)
    val Ebony = Color(0xFF0D0E11)
    val CardSurface = Color(0xFF13151A)
    val CardBorder = Color(0xFF222630)
    val FocusBorder = Color(0xD9EFECE6) // 85% Bone-white
    val Bone = Color(0xFFEFECE6)
    val BoneMuted = Color(0xFFC5C3BD)
    val Amber = Color(0xFFD4A373)
    val AmberMuted = Color(0x66D4A373)
    val AmberBg = Color(0x1AD4A373)
    val Slate = Color(0xFF8E929B)
    val SlateDark = Color(0xFF242832)
}

private data class BackdropVisualTarget(
    val id: String,
    val backdropUrl: String?,
    val fallbackDrawable: Drawable?,
    val primaryColor: Color,
    val accentColor: Color
)

@Composable
private fun ImmersiveBackdrop(hero: HeroPreview) {
    val context = LocalContext.current
    val visualTarget = remember(hero.id, hero.backdropUrl, hero.fallbackDrawable, hero.primaryColor, hero.accentColor) {
        BackdropVisualTarget(
            id = hero.id,
            backdropUrl = hero.backdropUrl,
            fallbackDrawable = hero.fallbackDrawable,
            primaryColor = hero.primaryColor,
            accentColor = hero.accentColor
        )
    }

    Crossfade(
        targetState = visualTarget,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "ImmersiveBackdropCrossfade"
    ) { state ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Organic, desaturated dark atmospheric bloom behind artwork
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val centerOffset = Offset(size.width * 0.72f, size.height * 0.22f)
                        val gradRadius = (size.width * 0.65f).coerceAtLeast(400f)
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    state.primaryColor.copy(alpha = 0.40f),
                                    state.accentColor.copy(alpha = 0.12f),
                                    EditorialTheme.Obsidian
                                ),
                                center = centerOffset,
                                radius = gradRadius
                            )
                        )
                    }
            )

            // 2. Full-bleed cinematic artwork (uncompressed, mature 42% opacity)
            if (!state.backdropUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(state.backdropUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.42f)
                )
            } else if (state.fallbackDrawable != null) {
                val safeDrawable = remember(state.id, state.fallbackDrawable) {
                    state.fallbackDrawable.constantState?.newDrawable()?.mutate() ?: state.fallbackDrawable
                }
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(safeDrawable)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(50.dp)
                        .alpha(0.25f)
                )
            }

            // 3. Progressive filmic horizontal vignette (deep obsidian solid on left for typography readability)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0.0f to EditorialTheme.Obsidian,
                            0.38f to Color(0xF2090A0C),
                            0.65f to Color(0x99090A0C),
                            1.0f to Color(0x22090A0C)
                        )
                    )
            )

            // 4. Filmic multi-stop vertical vignette (open artwork at top-center, 100% solid obsidian behind card rows)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color(0x77090A0C),
                            0.20f to Color.Transparent,
                            0.48f to Color(0x55090A0C),
                            0.70f to Color(0xE6090A0C),
                            0.85f to EditorialTheme.Obsidian,
                            1.0f to EditorialTheme.Obsidian
                        )
                    )
            )
        }
    }
}

@Composable
private fun TopNavigationBar(
    serverOnline: Boolean,
    hiddenCount: Int,
    availableUpdate: AppUpdateInfo?,
    updateDownloadProgress: Int?,
    onInstallUpdate: () -> Unit,
    onOpenTvInputs: () -> Unit,
    onOpenHiddenApps: () -> Unit,
    onOpenServerSettings: () -> Unit,
    onOpenWifi: () -> Unit,
    onOpenAndroidSettings: () -> Unit,
    onFocusHero: (HeroPreview) -> Unit
) {
    var timeString by remember {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
    }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            timeString = sdf.format(Date())
            delay(15_000L)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Understated Cinematic Brand Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(2.5.dp, 16.dp)
                        .background(EditorialTheme.Amber)
                )
                Text(
                    text = "HOMELAB TV",
                    color = EditorialTheme.Bone,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.5.sp
                )
            }

            // Minimalist Server Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF14161C))
                    .border(1.dp, EditorialTheme.SlateDark, RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (serverOnline) Color(0xFF22C55E) else EditorialTheme.Amber)
                )
                Text(
                    text = if (serverOnline) "CONECTADO" else "LOCAL",
                    color = EditorialTheme.Slate,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
        }

        // Quick Hardware & System Actions + Clock
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (availableUpdate != null || updateDownloadProgress != null) {
                val updateLabel = when {
                    updateDownloadProgress != null -> "⏳ Actualizando ${updateDownloadProgress}%"
                    availableUpdate?.versionName?.isNotBlank() == true -> "⬇ Actualizar v${availableUpdate.versionName}"
                    else -> "⬇ Actualizar App"
                }
                TopActionChip(
                    label = updateLabel,
                    highlightColor = EditorialTheme.Amber,
                    onFocus = {
                        onFocusHero(
                            HeroPreview(
                                id = "sys_ota_update",
                                title = "Nueva versión disponible (${availableUpdate?.versionName ?: "OTA"})",
                                badgeText = "ACTUALIZACIÓN LOCAL",
                                subtitle = "Descarga directa desde tu servidor Homelab",
                                description = "Pulsa OK para descargar e instalar automáticamente la nueva versión del Launcher.",
                                primaryColor = EditorialTheme.Ebony,
                                accentColor = EditorialTheme.Amber
                            )
                        )
                    },
                    onClick = onInstallUpdate
                )
            }

            TopActionChip(
                label = "Entradas",
                onFocus = {
                    onFocusHero(
                        HeroPreview(
                            id = "sys_inputs",
                            title = "Entradas HDMI / TV",
                            badgeText = "SEÑAL DE VÍDEO",
                            subtitle = "Selector de Entrada de Vídeo",
                            description = "Cambia directamente a la entrada HDMI o sintonizador de televisión.",
                            primaryColor = EditorialTheme.Ebony,
                            accentColor = EditorialTheme.Slate
                        )
                    )
                },
                onClick = onOpenTvInputs
            )

            if (hiddenCount > 0) {
                TopActionChip(
                    label = "Ocultas ($hiddenCount)",
                    onFocus = {
                        onFocusHero(
                            HeroPreview(
                                id = "sys_hidden",
                                title = "Aplicaciones Ocultas ($hiddenCount)",
                                badgeText = "BIBLIOTECA",
                                subtitle = "Gestionar aplicaciones archivadas",
                                description = "Revisa o restaura cualquiera de las aplicaciones que has ocultado.",
                                primaryColor = EditorialTheme.Ebony,
                                accentColor = EditorialTheme.Slate
                            )
                        )
                    },
                    onClick = onOpenHiddenApps
                )
            }

            TopActionChip(
                label = "Servidor",
                onFocus = {
                    onFocusHero(
                        HeroPreview(
                            id = "sys_server",
                            title = "Ajustes de Servidor",
                            badgeText = "SISTEMA",
                            subtitle = "Jellyfin y Torrent Stream Web",
                            description = "Configura el reproductor predeterminado (VLC, Kodi, ExoPlayer) y las rutas de tu red.",
                            primaryColor = EditorialTheme.Ebony,
                            accentColor = EditorialTheme.Amber
                        )
                    )
                },
                onClick = onOpenServerSettings
            )

            TopActionChip(
                label = "Red",
                onFocus = {
                    onFocusHero(
                        HeroPreview(
                            id = "sys_wifi",
                            title = "Conectividad y Red",
                            badgeText = "SISTEMA",
                            subtitle = "Wi-Fi y Ethernet",
                            description = "Abre los ajustes de red inalámbrica o cableada del televisor.",
                            primaryColor = EditorialTheme.Ebony,
                            accentColor = EditorialTheme.Slate
                        )
                    )
                },
                onClick = onOpenWifi
            )

            TopActionChip(
                label = "Ajustes TV",
                onFocus = {
                    onFocusHero(
                        HeroPreview(
                            id = "sys_android",
                            title = "Ajustes del Sistema Android TV",
                            badgeText = "SISTEMA",
                            subtitle = "Imagen, Audio, Mandos y Dispositivo",
                            description = "Abre el panel de configuración nativo de Android TV.",
                            primaryColor = EditorialTheme.Ebony,
                            accentColor = EditorialTheme.Slate
                        )
                    )
                },
                onClick = onOpenAndroidSettings
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = timeString,
                color = EditorialTheme.Bone,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopActionChip(
    label: String,
    highlightColor: Color? = null,
    onFocus: () -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.04f else 1.0f, label = "chipScale")
    val bgColor = when {
        focused -> highlightColor ?: EditorialTheme.Bone
        highlightColor != null -> highlightColor.copy(alpha = 0.20f)
        else -> Color(0xFF14161C)
    }
    val borderColor = when {
        focused -> highlightColor ?: EditorialTheme.Bone
        highlightColor != null -> highlightColor.copy(alpha = 0.60f)
        else -> EditorialTheme.SlateDark
    }

    Box(
        modifier = Modifier
            .scale(scale)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(6.dp)
            )
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (focused) EditorialTheme.Obsidian else EditorialTheme.BoneMuted,
            fontSize = 11.5.sp,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
            letterSpacing = 0.4.sp
        )
    }
}

@Composable
private fun EditorialHeroBanner(
    hero: HeroPreview,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        // Tag / Meta category badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(EditorialTheme.AmberBg)
                    .border(1.dp, EditorialTheme.AmberMuted, RoundedCornerShape(3.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = hero.badgeText.uppercase(),
                    color = EditorialTheme.Amber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.8.sp
                )
            }

            Text(
                text = hero.subtitle,
                color = EditorialTheme.BoneMuted,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.4.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Cinema Grotesque headline title
        Text(
            text = hero.title,
            color = EditorialTheme.Bone,
            fontSize = 35.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Description
        Text(
            text = hero.description,
            color = Color(0xFFA2A6B0),
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            letterSpacing = 0.2.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Slim 3dp Amber progress bar for live Torrent / Jellyfin resume
        if (hero.progressFraction != null) {
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { hero.progressFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .width(260.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp)),
                color = EditorialTheme.Amber,
                trackColor = Color(0x28FFFFFF)
            )
        }
    }
}

@Composable
private fun SelectorRowSection(
    title: String,
    subtitleBadge: String? = null,
    accent: Color,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 48.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(2.dp, 13.dp)
                    .background(accent)
            )
            Text(
                text = title,
                color = EditorialTheme.Bone,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )
            if (!subtitleBadge.isNullOrBlank()) {
                Text(
                    text = "·  $subtitleBadge",
                    color = EditorialTheme.Slate,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.3.sp
                )
            }
        }
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppSelectorCard(
    app: TvAppInfo,
    cardWidth: Dp,
    cardHeight: Dp,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var centerKeyDown by remember { mutableStateOf(false) }
    var longPressTriggered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "appCardScale"
    )

    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(cardWidth)
    ) {
        Box(
            modifier = modifier
                .size(cardWidth, cardHeight)
                .scale(scale)
                .onFocusChanged {
                    isFocused = it.isFocused
                    if (!it.isFocused) {
                        centerKeyDown = false
                        longPressTriggered = false
                    }
                    if (it.isFocused) onFocused()
                }
                .onPreviewKeyEvent { event ->
                    when (event.type) {
                        KeyEventType.KeyDown -> {
                            if (event.key == Key.Menu || event.key == Key.Bookmark || event.key == Key.Info) {
                                onLongClick()
                                return@onPreviewKeyEvent true
                            }
                            if (event.key == Key.DirectionCenter || event.key == Key.Enter) {
                                if (event.nativeKeyEvent.repeatCount == 0) {
                                    centerKeyDown = true
                                    longPressTriggered = false
                                } else if (centerKeyDown && !longPressTriggered &&
                                    (event.nativeKeyEvent.isLongPress || event.nativeKeyEvent.repeatCount >= 1)
                                ) {
                                    longPressTriggered = true
                                    onLongClick()
                                }
                                return@onPreviewKeyEvent true
                            }
                        }
                        KeyEventType.KeyUp -> {
                            if (event.key == Key.DirectionCenter || event.key == Key.Enter) {
                                val shouldClick = centerKeyDown && !longPressTriggered
                                centerKeyDown = false
                                longPressTriggered = false
                                if (shouldClick) onClick()
                                return@onPreviewKeyEvent true
                            }
                        }
                    }
                    false
                }
                .clip(RoundedCornerShape(6.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            app.dominantColor.copy(alpha = 0.40f),
                            EditorialTheme.CardSurface
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (isFocused) EditorialTheme.FocusBorder else EditorialTheme.CardBorder,
                    shape = RoundedCornerShape(6.dp)
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            contentAlignment = Alignment.Center
        ) {
            val visualData = remember(app.packageName, app.banner, app.icon) {
                val raw = app.banner ?: app.icon
                raw?.constantState?.newDrawable()?.mutate() ?: raw
            }
            if (visualData != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(visualData)
                        .crossfade(true)
                        .build(),
                    contentDescription = app.label,
                    contentScale = if (app.banner != null) ContentScale.Crop else ContentScale.Fit,
                    modifier = if (app.banner != null) Modifier.fillMaxSize() else Modifier.size(52.dp),
                    alpha = if (isFocused) 1.0f else 0.88f
                )
            } else {
                Text(
                    text = app.label.take(2).uppercase(),
                    color = EditorialTheme.Bone,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }

            // Subtle bottom gradient inside card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.6f to Color.Transparent,
                            1.0f to Color(0x99090A0C)
                        )
                    )
            )

            // Minimalist amber dot favorite badge
            if (app.isFavorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(EditorialTheme.Amber)
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = app.label,
            color = if (isFocused) EditorialTheme.Bone else EditorialTheme.Slate,
            fontSize = 11.5.sp,
            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TorrentSessionCard(
    session: TorrentSession,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var centerKeyDown by remember { mutableStateOf(false) }
    var longPressTriggered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "torrentCardScale"
    )

    val pct = (session.bufferProgress * 100).toInt()
    val statusText = when {
        session.paused -> "PAUSADO"
        session.bufferReady -> "LISTO PARA REPRODUCIR"
        else -> "BUFFER $pct%"
    }

    Column(
        modifier = Modifier.width(220.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(220.dp, 108.dp)
                .scale(scale)
                .onFocusChanged {
                    isFocused = it.isFocused
                    if (!it.isFocused) {
                        centerKeyDown = false
                        longPressTriggered = false
                    }
                    if (it.isFocused) onFocused()
                }
                .onPreviewKeyEvent { event ->
                    when (event.type) {
                        KeyEventType.KeyDown -> {
                            if (event.key == Key.Menu || event.key == Key.Bookmark || event.key == Key.Info) {
                                onLongClick()
                                return@onPreviewKeyEvent true
                            }
                            if (event.key == Key.DirectionCenter || event.key == Key.Enter) {
                                if (event.nativeKeyEvent.repeatCount == 0) {
                                    centerKeyDown = true
                                    longPressTriggered = false
                                } else if (centerKeyDown && !longPressTriggered &&
                                    (event.nativeKeyEvent.isLongPress || event.nativeKeyEvent.repeatCount >= 1)
                                ) {
                                    longPressTriggered = true
                                    onLongClick()
                                }
                                return@onPreviewKeyEvent true
                            }
                        }
                        KeyEventType.KeyUp -> {
                            if (event.key == Key.DirectionCenter || event.key == Key.Enter) {
                                val shouldClick = centerKeyDown && !longPressTriggered
                                centerKeyDown = false
                                longPressTriggered = false
                                if (shouldClick) onClick()
                                return@onPreviewKeyEvent true
                            }
                        }
                    }
                    false
                }
                .clip(RoundedCornerShape(6.dp))
                .background(EditorialTheme.CardSurface)
                .border(
                    width = 1.dp,
                    color = if (isFocused) EditorialTheme.FocusBorder else EditorialTheme.CardBorder,
                    shape = RoundedCornerShape(6.dp)
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(EditorialTheme.Amber)
                    )
                    Text(
                        text = statusText,
                        color = EditorialTheme.Amber,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    )
                }

                Text(
                    text = session.selectedFile?.fileName ?: session.title,
                    color = EditorialTheme.Bone,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                LinearProgressIndicator(
                    progress = { session.bufferProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp)),
                    color = EditorialTheme.Amber,
                    trackColor = Color(0x28FFFFFF)
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = "Torrent · ${formatBytes(session.bufferedBytes + session.prefetchedBytes)}",
            color = if (isFocused) EditorialTheme.Bone else EditorialTheme.Slate,
            fontSize = 11.5.sp,
            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun JellyfinMediaCard(
    item: JellyfinMediaItem,
    isResume: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }
    var centerKeyDown by remember { mutableStateOf(false) }
    var longPressTriggered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "jfCardScale"
    )

    val cardWidth = if (isResume) 200.dp else 132.dp
    val cardHeight = if (isResume) 112.dp else 198.dp
    val imageUrl = if (isResume) item.backdropUrl else item.posterUrl

    Column(
        modifier = Modifier.width(cardWidth),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(cardWidth, cardHeight)
                .scale(scale)
                .onFocusChanged {
                    isFocused = it.isFocused
                    if (!it.isFocused) {
                        centerKeyDown = false
                        longPressTriggered = false
                    }
                    if (it.isFocused) onFocused()
                }
                .onPreviewKeyEvent { event ->
                    when (event.type) {
                        KeyEventType.KeyDown -> {
                            if (event.key == Key.Menu || event.key == Key.Bookmark || event.key == Key.Info) {
                                onLongClick()
                                return@onPreviewKeyEvent true
                            }
                            if (event.key == Key.DirectionCenter || event.key == Key.Enter) {
                                if (event.nativeKeyEvent.repeatCount == 0) {
                                    centerKeyDown = true
                                    longPressTriggered = false
                                } else if (centerKeyDown && !longPressTriggered &&
                                    (event.nativeKeyEvent.isLongPress || event.nativeKeyEvent.repeatCount >= 1)
                                ) {
                                    longPressTriggered = true
                                    onLongClick()
                                }
                                return@onPreviewKeyEvent true
                            }
                        }
                        KeyEventType.KeyUp -> {
                            if (event.key == Key.DirectionCenter || event.key == Key.Enter) {
                                val shouldClick = centerKeyDown && !longPressTriggered
                                centerKeyDown = false
                                longPressTriggered = false
                                if (shouldClick) onClick()
                                return@onPreviewKeyEvent true
                            }
                        }
                    }
                    false
                }
                .clip(RoundedCornerShape(6.dp))
                .background(EditorialTheme.CardSurface)
                .border(
                    width = 1.dp,
                    color = if (isFocused) EditorialTheme.FocusBorder else EditorialTheme.CardBorder,
                    shape = RoundedCornerShape(6.dp)
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = if (isFocused) 1.0f else 0.88f
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.5f to Color.Transparent,
                            1.0f to Color(0xCC090A0C)
                        )
                    )
            )

            if (item.progressFraction > 0f) {
                LinearProgressIndicator(
                    progress = { item.progressFraction },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp),
                    color = EditorialTheme.Amber,
                    trackColor = Color(0x28FFFFFF)
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = item.title,
            color = if (isFocused) EditorialTheme.Bone else EditorialTheme.Slate,
            fontSize = 11.5.sp,
            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AppOptionsDialog(
    app: TvAppInfo,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onHideApp: () -> Unit,
    onOpenSystemInfo: () -> Unit
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { firstFocus.requestFocus() }
    }
    BackHandler { onDismiss() }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0F1117),
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(420.dp)
                .border(1.dp, EditorialTheme.CardBorder, RoundedCornerShape(8.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = app.label,
                    color = EditorialTheme.Bone,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = app.packageName,
                    color = EditorialTheme.Slate,
                    fontSize = 11.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                DialogActionButton(
                    text = if (app.isFavorite) "★ Quitar de Favoritas" else "☆ Añadir a Favoritas",
                    modifier = Modifier.focusRequester(firstFocus),
                    onClick = onToggleFavorite
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        DialogActionButton(text = "← Mover Izq.", onClick = onMoveLeft)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        DialogActionButton(text = "Mover Der. →", onClick = onMoveRight)
                    }
                }
                DialogActionButton(
                    text = "Ocultar aplicación",
                    onClick = onHideApp
                )
                DialogActionButton(
                    text = "Detalles del sistema",
                    onClick = onOpenSystemInfo
                )
                DialogActionButton(
                    text = "Cerrar",
                    onClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun TorrentActionDialog(
    session: TorrentSession,
    onDismiss: () -> Unit,
    onPlayVlc: () -> Unit,
    onPlayKodi: () -> Unit,
    onPlayInternal: () -> Unit,
    onTogglePause: () -> Unit,
    onSelectFile: (Int) -> Unit
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { firstFocus.requestFocus() }
    }
    BackHandler { onDismiss() }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0F1117),
            modifier = Modifier
                .width(480.dp)
                .border(1.dp, EditorialTheme.CardBorder, RoundedCornerShape(8.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = session.title,
                    color = EditorialTheme.Bone,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                    maxLines = 2
                )
                Text(
                    text = "Buffer: ${(session.bufferProgress * 100).toInt()}% (${formatBytes(session.bufferedBytes)} / ${formatBytes(session.bufferTarget)}) · Caché: ${formatBytes(session.prefetchedBytes)}",
                    color = EditorialTheme.Amber,
                    fontSize = 11.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                DialogActionButton(
                    text = "▶ Reproducir en VLC",
                    modifier = Modifier.focusRequester(firstFocus),
                    onClick = onPlayVlc
                )
                DialogActionButton(text = "▶ Reproductor Integrado (ExoPlayer)", onClick = onPlayInternal)
                DialogActionButton(text = "▶ Reproducir en Kodi", onClick = onPlayKodi)
                DialogActionButton(
                    text = if (session.paused) "⏯ Reanudar descarga" else "⏸ Pausar descarga",
                    onClick = onTogglePause
                )

                if (session.files.size > 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Seleccionar archivo (${session.files.size}):",
                        color = EditorialTheme.BoneMuted,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                        items(session.files, key = { it.id }) { file ->
                            val isSelected = file.id == session.selected
                            DialogActionButton(
                                text = (if (isSelected) "✓ " else "") + file.fileName,
                                onClick = { onSelectFile(file.id) }
                            )
                        }
                    }
                }

                DialogActionButton(text = "Cerrar", onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun JellyfinActionDialog(
    item: JellyfinMediaItem,
    onDismiss: () -> Unit,
    onOpenJellyfinApp: () -> Unit,
    onPlayVlc: () -> Unit,
    onPlayKodi: () -> Unit,
    onPlayInternal: () -> Unit
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { firstFocus.requestFocus() }
    }
    BackHandler { onDismiss() }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0F1117),
            modifier = Modifier
                .width(450.dp)
                .border(1.dp, EditorialTheme.CardBorder, RoundedCornerShape(8.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = item.title,
                    color = EditorialTheme.Bone,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp
                )
                Text(
                    text = item.subtitle,
                    color = EditorialTheme.Amber,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                val resumeLabel = if (item.resumePositionMs > 0L) {
                    " (${formatDuration(item.resumePositionMs)})"
                } else ""

                DialogActionButton(
                    text = "▶ Reproducir en VLC$resumeLabel",
                    modifier = Modifier.focusRequester(firstFocus),
                    onClick = onPlayVlc
                )
                DialogActionButton(text = "▶ Reproductor Integrado$resumeLabel", onClick = onPlayInternal)
                DialogActionButton(text = "▶ Abrir en App Jellyfin TV", onClick = onOpenJellyfinApp)
                DialogActionButton(text = "▶ Reproducir en Kodi", onClick = onPlayKodi)
                DialogActionButton(text = "Cerrar", onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun HiddenAppsDialog(
    hiddenApps: List<TvAppInfo>,
    onRestoreApp: (TvAppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val closeFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { closeFocus.requestFocus() }
    }
    BackHandler { onDismiss() }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0F1117),
            modifier = Modifier
                .width(440.dp)
                .border(1.dp, EditorialTheme.CardBorder, RoundedCornerShape(8.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Aplicaciones Archivadas (${hiddenApps.size})",
                    color = EditorialTheme.Bone,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp
                )
                Text(
                    text = "Selecciona una aplicación para restaurarla en la cuadrícula principal:",
                    color = EditorialTheme.Slate,
                    fontSize = 12.sp
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(hiddenApps, key = { it.packageName }) { app ->
                        DialogActionButton(
                            text = "Restaurar: ${app.label}",
                            onClick = { onRestoreApp(app) }
                        )
                    }
                }
                DialogActionButton(
                    text = "Cerrar",
                    modifier = Modifier.focusRequester(closeFocus),
                    onClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun ServerSettingsDialog(
    current: LauncherSettings,
    onSave: (LauncherSettings) -> Unit,
    onInstallUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { firstFocus.requestFocus() }
    }
    BackHandler { onDismiss() }
    var selectedPlayer by remember { mutableStateOf(current.preferredPlayer) }
    var jfUrl by remember { mutableStateOf(current.jellyfinUrl) }
    var tsUrl by remember { mutableStateOf(current.torrentStreamUrl) }
    var jfApiKey by remember { mutableStateOf(current.jellyfinApiKey) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0F1117),
            modifier = Modifier
                .width(520.dp)
                .border(1.dp, EditorialTheme.CardBorder, RoundedCornerShape(8.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Ajustes de Servidor y Reproducción",
                    color = EditorialTheme.Bone,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp
                )
                Text(
                    text = "Reproductor predeterminado al pulsar OK:",
                    color = EditorialTheme.Slate,
                    fontSize = 12.5.sp
                )

                PreferredPlayer.entries.forEachIndexed { index, option ->
                    val prefix = if (selectedPlayer == option) "● " else "○ "
                    DialogActionButton(
                        text = "$prefix${option.label}",
                        modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                        onClick = { selectedPlayer = option }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Servidores Homelab:",
                    color = EditorialTheme.BoneMuted,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = tsUrl,
                    onValueChange = { tsUrl = it },
                    label = { Text("URL Torrent Stream Web", color = EditorialTheme.Slate) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = EditorialTheme.Bone,
                        unfocusedTextColor = EditorialTheme.BoneMuted,
                        focusedBorderColor = EditorialTheme.Amber,
                        unfocusedBorderColor = EditorialTheme.CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = jfUrl,
                    onValueChange = { jfUrl = it },
                    label = { Text("URL Jellyfin", color = EditorialTheme.Slate) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = EditorialTheme.Bone,
                        unfocusedTextColor = EditorialTheme.BoneMuted,
                        focusedBorderColor = EditorialTheme.Amber,
                        unfocusedBorderColor = EditorialTheme.CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = jfApiKey,
                    onValueChange = { jfApiKey = it },
                    label = { Text("API Key Jellyfin", color = EditorialTheme.Slate) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = EditorialTheme.Bone,
                        unfocusedTextColor = EditorialTheme.BoneMuted,
                        focusedBorderColor = EditorialTheme.Amber,
                        unfocusedBorderColor = EditorialTheme.CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                DialogActionButton(
                    text = "⬇ Comprobar e instalar actualización del servidor",
                    onClick = onInstallUpdate
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        DialogActionButton(
                            text = "Guardar",
                            onClick = {
                                onSave(
                                    current.copy(
                                        preferredPlayer = selectedPlayer,
                                        jellyfinUrl = jfUrl.trim(),
                                        torrentStreamUrl = tsUrl.trim(),
                                        jellyfinApiKey = jfApiKey.trim()
                                    )
                                )
                            }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        DialogActionButton(text = "Cancelar", onClick = onDismiss)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialogActionButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(6.dp))
            .background(if (focused) EditorialTheme.Bone else Color(0xFF161820))
            .border(
                width = 1.dp,
                color = if (focused) EditorialTheme.Bone else EditorialTheme.SlateDark,
                shape = RoundedCornerShape(6.dp)
            )
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = if (focused) EditorialTheme.Obsidian else EditorialTheme.Bone,
            fontSize = 13.sp,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
            letterSpacing = 0.3.sp
        )
    }
}

private fun handlePlayTorrent(
    context: android.content.Context,
    session: TorrentSession,
    preferredPlayer: PreferredPlayer,
    onAskDialog: () -> Unit
) {
    when (preferredPlayer) {
        PreferredPlayer.ASK -> onAskDialog()
        PreferredPlayer.VLC -> ExternalPlayerLauncher.openInVlc(context, session.playUrl, session.title)
        PreferredPlayer.KODI -> ExternalPlayerLauncher.openInKodi(context, session.playUrl, session.title)
        PreferredPlayer.EXOPLAYER, PreferredPlayer.JELLYFIN ->
            PlayerActivity.launch(context, session.playUrl, session.title)
    }
}

private fun handlePlayJellyfin(
    context: android.content.Context,
    item: JellyfinMediaItem,
    preferredPlayer: PreferredPlayer,
    onAskDialog: () -> Unit
) {
    when (preferredPlayer) {
        PreferredPlayer.ASK -> onAskDialog()
        PreferredPlayer.VLC -> ExternalPlayerLauncher.openInVlc(context, item.directStreamUrl, item.title, item.resumePositionMs)
        PreferredPlayer.KODI -> ExternalPlayerLauncher.openInKodi(context, item.directStreamUrl, item.title)
        PreferredPlayer.EXOPLAYER -> PlayerActivity.launch(context, item.directStreamUrl, item.title, item.resumePositionMs)
        PreferredPlayer.JELLYFIN -> {
            if (!ExternalPlayerLauncher.openJellyfinApp(context)) {
                PlayerActivity.launch(context, item.directStreamUrl, item.title, item.resumePositionMs)
            }
        }
    }
}

private fun appToHero(app: TvAppInfo, isFavRow: Boolean = false): HeroPreview =
    HeroPreview(
        id = "app_${app.packageName}",
        title = app.label,
        badgeText = if (isFavRow || app.isFavorite) "FAVORITA" else "APLICACIÓN",
        subtitle = app.curatedSubtitle ?: "Aplicación Android TV",
        description = app.curatedDescription ?: "Pulsa OK para iniciar ${app.label}. Mantén pulsado OK para opciones.",
        backdropUrl = app.curatedBackdropUrl,
        fallbackDrawable = app.banner ?: app.icon,
        primaryColor = app.dominantColor,
        accentColor = app.accentColor
    )

private fun torrentToHero(session: TorrentSession): HeroPreview {
    val pct = (session.bufferProgress * 100).toInt()
    val badge = when {
        session.paused -> "TORRENT STREAM · PAUSADO"
        session.bufferReady -> "TORRENT STREAM · BUFFER 100% LISTO"
        else -> "TORRENT STREAM · CARGANDO BUFFER ($pct%)"
    }
    val fileInfo = session.selectedFile?.let { "${it.fileName} (${formatBytes(it.length)})" } ?: session.title
    return HeroPreview(
        id = "ts_${session.id}",
        title = session.title,
        badgeText = badge,
        subtitle = fileInfo,
        description = "Caché acumulada: ${formatBytes(session.bufferedBytes + session.prefetchedBytes)}. Pulsa OK para reproducir en VLC, Kodi o reproductor integrado.",
        backdropUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=1920&q=80",
        primaryColor = Color(0xFF0F2942),
        accentColor = Color(0xFF38BDF8),
        progressFraction = session.bufferProgress
    )
}

private fun jellyfinToHero(item: JellyfinMediaItem, isResume: Boolean): HeroPreview {
    val badge = if (isResume && item.resumePositionMs > 0L) {
        "SEGUIR VIENDO · ${formatDuration(item.resumePositionMs)}"
    } else {
        "JELLYFIN · RECIENTE"
    }
    return HeroPreview(
        id = "jf_${item.id}",
        title = item.title,
        badgeText = badge,
        subtitle = item.subtitle,
        description = item.overview,
        backdropUrl = item.backdropUrl,
        primaryColor = Color(0xFF0E2238),
        accentColor = Color(0xFF00A4DC),
        progressFraction = if (item.progressFraction > 0f) item.progressFraction else null
    )
}


private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        String.format(Locale.US, "%.2f GB", mb / 1024.0)
    } else {
        String.format(Locale.US, "%.0f MB", mb)
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSec / 3600L
    val minutes = (totalSec % 3600L) / 60L
    return if (hours > 0L) "${hours}h ${minutes}m" else "${minutes} min"
}
