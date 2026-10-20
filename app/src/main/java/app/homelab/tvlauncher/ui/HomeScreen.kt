package app.homelab.tvlauncher.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
    onLaunchApp: (TvAppInfo) -> Unit,
    onToggleFavorite: (TvAppInfo) -> Unit,
    onMoveApp: (TvAppInfo, Int) -> Unit,
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

    var currentHero by remember {
        mutableStateOf(
            HeroPreview(
                id = "welcome",
                title = "Homelab TV",
                badgeText = "LAUNCHER NATIVO · SIN PUBLICIDAD",
                subtitle = "Android TV · Google TV · Streaming Local",
                description = "Desplázate con la cruceta por tus aplicaciones, sesiones activas de Torrent Stream y tu biblioteca de Jellyfin.",
                primaryColor = Color(0xFF141B2D),
                accentColor = Color(0xFFE50914)
            )
        )
    }

    var selectedAppForMenu by remember { mutableStateOf<TvAppInfo?>(null) }
    var selectedTorrentForDialog by remember { mutableStateOf<TorrentSession?>(null) }
    var selectedJellyfinForDialog by remember { mutableStateOf<JellyfinMediaItem?>(null) }
    var showHiddenAppsDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Set initial hero to first favorite or first app when loaded
    LaunchedEffect(favoriteApps, allApps) {
        if (currentHero.id == "welcome") {
            val first = favoriteApps.firstOrNull() ?: allApps.firstOrNull()
            if (first != null) {
                currentHero = appToHero(first)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07090E))
    ) {
        // 1. Full-screen Immersive Netflix-style Backdrop (subtle, low protagonism)
        ImmersiveBackdrop(hero = currentHero)

        // 2. Foreground Content: Top Bar + Top-Left Hero Details + Smooth Rows
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 20.dp)
        ) {
            TopNavigationBar(
                serverOnline = serverOnline,
                hiddenCount = hiddenApps.size,
                onOpenTvInputs = onOpenTvInputs,
                onOpenHiddenApps = { showHiddenAppsDialog = true },
                onOpenServerSettings = { showSettingsDialog = true },
                onOpenWifi = onOpenWifiSettings,
                onOpenAndroidSettings = onOpenSystemSettings,
                onFocusHero = { currentHero = it }
            )

            // Netflix-style Top-Left Hero Banner
            NetflixHeroBanner(
                hero = currentHero,
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .height(178.dp)
                    .padding(start = 44.dp, top = 10.dp, bottom = 6.dp)
            )

            // 4 Smooth Horizontal Rows with D-Pad Pivot Scrolling
            TvLazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                pivotOffsets = PivotOffsets(0.08f, 0.08f),
                contentPadding = PaddingValues(top = 4.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // ROW 1: FAVORITAS
                if (favoriteApps.isNotEmpty()) {
                    item(key = "row_favorites") {
                        SelectorRowSection(title = "Favoritas", accent = Color(0xFFE50914)) {
                            TvLazyRow(
                                pivotOffsets = PivotOffsets(0.08f, 0.08f),
                                contentPadding = PaddingValues(horizontal = 44.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(favoriteApps, key = { "fav_${it.packageName}" }) { app ->
                                    AppSelectorCard(
                                        app = app,
                                        cardWidth = 172.dp,
                                        cardHeight = 100.dp,
                                        onFocused = { currentHero = appToHero(app, isFavRow = true) },
                                        onClick = { onLaunchApp(app) },
                                        onLongClick = { selectedAppForMenu = app }
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
                            accent = Color(0xFF38BDF8)
                        ) {
                            TvLazyRow(
                                pivotOffsets = PivotOffsets(0.08f, 0.08f),
                                contentPadding = PaddingValues(horizontal = 44.dp),
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
                        subtitleBadge = "Mantén OK o pulsa MENÚ para organizar u ocultar",
                        accent = Color(0xFFA855F7)
                    ) {
                        TvLazyRow(
                            pivotOffsets = PivotOffsets(0.08f, 0.08f),
                            contentPadding = PaddingValues(horizontal = 44.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(allApps, key = { "app_${it.packageName}" }) { app ->
                                AppSelectorCard(
                                    app = app,
                                    cardWidth = 158.dp,
                                    cardHeight = 94.dp,
                                    onFocused = { currentHero = appToHero(app, isFavRow = false) },
                                    onClick = { onLaunchApp(app) },
                                    onLongClick = { selectedAppForMenu = app }
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
                            subtitleBadge = "Biblioteca Multimedia",
                            accent = Color(0xFF00A4DC)
                        ) {
                            TvLazyRow(
                                pivotOffsets = PivotOffsets(0.08f, 0.08f),
                                contentPadding = PaddingValues(horizontal = 44.dp),
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
                    onMoveApp(app, -1)
                },
                onMoveRight = {
                    onMoveApp(app, 1)
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
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

@Composable
private fun ImmersiveBackdrop(hero: HeroPreview) {
    val context = LocalContext.current

    Crossfade(
        targetState = hero,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "ImmersiveBackdropCrossfade"
    ) { state ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Base atmospheric radial gradient matching the focused app/movie colors
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                state.primaryColor.copy(alpha = 0.85f),
                                state.accentColor.copy(alpha = 0.22f),
                                Color(0xFF07090E)
                            ),
                            center = Offset(1400f, 260f),
                            radius = 1250f
                        )
                    )
            )

            // Full-screen artwork on the upper-right with subtle protagonism (36% opacity)
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
                        .alpha(0.38f)
                )
            } else if (state.fallbackDrawable != null) {
                val bmp = remember(state.id) { drawableToBitmap(state.fallbackDrawable, 320, 180) }
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(55.dp)
                            .alpha(0.26f)
                    )
                }
            }

            // Left-to-Right Netflix Vignette (keeps left side dark for Hero text legibility)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0.0f to Color(0xFF07090E),
                            0.35f to Color(0xE807090E),
                            0.68f to Color(0x8807090E),
                            1.0f to Color(0x3307090E)
                        )
                    )
            )

            // Top-to-Bottom Netflix Vignette (keeps bottom half dark for the card rows)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color(0x6607090E),
                            0.28f to Color.Transparent,
                            0.58f to Color(0xCC07090E),
                            0.82f to Color(0xF507090E),
                            1.0f to Color(0xFF07090E)
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
    onOpenTvInputs: () -> Unit,
    onOpenHiddenApps: () -> Unit,
    onOpenServerSettings: () -> Unit,
    onOpenWifi: () -> Unit,
    onOpenAndroidSettings: () -> Unit,
    onFocusHero: (HeroPreview) -> Unit
) {
    var timeString by remember { mutableStateOf("") }
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
            .padding(horizontal = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Brand + Server Status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp, 24.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFFE50914))
                )
                Text(
                    text = "HOMELAB TV",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
            }

            // Server pill
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (serverOnline) Color(0xFF22C55E) else Color(0xFFF59E0B))
                )
                Text(
                    text = if (serverOnline) "Servidor Conectado" else "Modo Local",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Quick Hardware & System Actions + Clock
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TopActionChip(
                label = "Entradas HDMI",
                onFocus = {
                    onFocusHero(
                        HeroPreview(
                            id = "sys_inputs",
                            title = "Entradas HDMI / TV",
                            badgeText = "FUENTE DE VÍDEO",
                            subtitle = "Selector de Entrada de Vídeo",
                            description = "Cambia directamente a la entrada HDMI o sintonizador de televisión.",
                            primaryColor = Color(0xFF141E30),
                            accentColor = Color(0xFF38BDF8)
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
                                badgeText = "FILTRO ANTI-BLOATWARE",
                                subtitle = "Gestionar aplicaciones ocultadas del selector",
                                description = "Revisa o restaura cualquiera de las $hiddenCount aplicaciones que has ocultado.",
                                primaryColor = Color(0xFF1F1633),
                                accentColor = Color(0xFFA855F7)
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
                            title = "Conexión con Servidor",
                            badgeText = "JELLYFIN & TORRENT STREAM",
                            subtitle = "Reproductor Predeterminado y Endpoints",
                            description = "Configura el reproductor predeterminado (VLC, Kodi, ExoPlayer o Jellyfin) y las direcciones del servidor.",
                            primaryColor = Color(0xFF0F2537),
                            accentColor = Color(0xFF00A4DC)
                        )
                    )
                },
                onClick = onOpenServerSettings
            )

            TopActionChip(
                label = "Wi-Fi",
                onFocus = {
                    onFocusHero(
                        HeroPreview(
                            id = "sys_wifi",
                            title = "Red y Wi-Fi",
                            badgeText = "CONECTIVIDAD",
                            subtitle = "Ajustes de red inalámbrica y Ethernet",
                            description = "Abre la configuración de red inalámbrica o Ethernet del dispositivo.",
                            primaryColor = Color(0xFF122620),
                            accentColor = Color(0xFF10B981)
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
                            subtitle = "Imagen, Sonido, Mandos Bluetooth y Dispositivo",
                            description = "Abre el panel de configuración nativo de Android TV.",
                            primaryColor = Color(0xFF1E222D),
                            accentColor = Color(0xFF94A3B8)
                        )
                    )
                },
                onClick = onOpenAndroidSettings
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = timeString,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopActionChip(
    label: String,
    onFocus: () -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.08f else 1.0f, label = "chipScale")

    Box(
        modifier = Modifier
            .scale(scale)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .clip(RoundedCornerShape(50))
            .background(if (focused) Color.White else Color.White.copy(alpha = 0.10f))
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) Color.White else Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(50)
            )
            .combinedClickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (focused) Color(0xFF090D16) else Color.White.copy(alpha = 0.90f),
            fontSize = 12.sp,
            fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun NetflixHeroBanner(
    hero: HeroPreview,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        // Category Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(hero.accentColor.copy(alpha = 0.90f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = hero.badgeText.uppercase(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }

            Text(
                text = hero.subtitle,
                color = Color.White.copy(alpha = 0.80f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Large Netflix-style Title
        Text(
            text = hero.title,
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Description
        Text(
            text = hero.description,
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 14.sp,
            lineHeight = 19.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Optional Progress Bar for Continue Watching / Torrent Buffer
        if (hero.progressFraction != null) {
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { hero.progressFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .width(260.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = hero.accentColor,
                trackColor = Color.White.copy(alpha = 0.18f)
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
            modifier = Modifier.padding(start = 44.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp, 18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
            )
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (!subtitleBadge.isNullOrBlank()) {
                Text(
                    text = "·  $subtitleBadge",
                    color = Color.White.copy(alpha = 0.50f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
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
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.11f else 1.0f,
        animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
        label = "appCardScale"
    )

    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(cardWidth)
    ) {
        Box(
            modifier = Modifier
                .size(cardWidth, cardHeight)
                .scale(scale)
                .onFocusChanged {
                    isFocused = it.isFocused
                    if (it.isFocused) onFocused()
                }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        (event.key == Key.Menu || event.key == Key.Bookmark || event.key == Key.Info)
                    ) {
                        onLongClick()
                        true
                    } else {
                        false
                    }
                }
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            app.dominantColor.copy(alpha = 0.95f),
                            Color(0xFF111622)
                        )
                    )
                )
                .border(
                    width = if (isFocused) 2.5.dp else 1.dp,
                    color = if (isFocused) Color.White else Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp)
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .focusable(),
            contentAlignment = Alignment.Center
        ) {
            val visualData = app.banner ?: app.icon
            if (visualData != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(visualData)
                        .crossfade(true)
                        .build(),
                    contentDescription = app.label,
                    contentScale = if (app.banner != null) ContentScale.Crop else ContentScale.Fit,
                    modifier = if (app.banner != null) Modifier.fillMaxSize() else Modifier.size(56.dp)
                )
            } else {
                Text(
                    text = app.label.take(2).uppercase(),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Subtle bottom gradient inside card when focused
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.55f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.65f)
                            )
                        )
                )
            }

            // Favorite star badge
            if (app.isFavorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE50914))
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = app.label,
            color = if (isFocused) Color.White else Color.White.copy(alpha = 0.68f),
            fontSize = 12.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
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
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.09f else 1.0f,
        animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
        label = "torrentCardScale"
    )

    val pct = (session.bufferProgress * 100).toInt()
    val statusColor = when {
        session.paused -> Color(0xFFF59E0B)
        session.bufferReady -> Color(0xFF22C55E)
        else -> Color(0xFF38BDF8)
    }

    Column(
        modifier = Modifier.width(230.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(230.dp, 118.dp)
                .scale(scale)
                .onFocusChanged {
                    isFocused = it.isFocused
                    if (it.isFocused) onFocused()
                }
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF0F2942), Color(0xFF091326))
                    )
                )
                .border(
                    width = if (isFocused) 2.5.dp else 1.dp,
                    color = if (isFocused) Color.White else statusColor.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp)
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .focusable()
                .padding(14.dp)
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
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = when {
                            session.paused -> "PAUSADO"
                            session.bufferReady -> "LISTO PARA REPRODUCIR"
                            else -> "PREPARANDO BUFFER $pct%"
                        },
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Text(
                    text = session.selectedFile?.fileName ?: session.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                LinearProgressIndicator(
                    progress = { session.bufferProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = statusColor,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Torrent Stream · ${formatBytes(session.bufferedBytes + session.prefetchedBytes)} en caché",
            color = if (isFocused) Color.White else Color.White.copy(alpha = 0.68f),
            fontSize = 12.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
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
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.09f else 1.0f,
        animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
        label = "jfCardScale"
    )

    val cardWidth = if (isResume) 210.dp else 148.dp
    val cardHeight = if (isResume) 118.dp else 210.dp
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
                    if (it.isFocused) onFocused()
                }
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF131B2E))
                .border(
                    width = if (isFocused) 2.5.dp else 1.dp,
                    color = if (isFocused) Color.White else Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp)
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .focusable()
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.5f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.80f)
                        )
                    )
            )

            if (item.progressFraction > 0f) {
                LinearProgressIndicator(
                    progress = { item.progressFraction },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(4.dp),
                    color = Color(0xFF00A4DC),
                    trackColor = Color.White.copy(alpha = 0.22f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.title,
            color = if (isFocused) Color.White else Color.White.copy(alpha = 0.75f),
            fontSize = 12.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
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
    BackHandler { onDismiss() }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF111726),
            tonalElevation = 8.dp,
            modifier = Modifier
                .width(420.dp)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = app.label,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = app.packageName,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                DialogActionButton(
                    text = if (app.isFavorite) "★ Quitar de Favoritas" else "☆ Añadir a Favoritas",
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
                    text = "Ocultar aplicación (Bloatware)",
                    onClick = onHideApp
                )
                DialogActionButton(
                    text = "Información del sistema / Forzar detención",
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
    BackHandler { onDismiss() }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF111726),
            modifier = Modifier
                .width(480.dp)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = session.title,
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Text(
                    text = "Buffer inicial: ${(session.bufferProgress * 100).toInt()}% (${formatBytes(session.bufferedBytes)} / ${formatBytes(session.bufferTarget)}) · Prefetch: ${formatBytes(session.prefetchedBytes)}",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                DialogActionButton(text = "▶ Reproducir en VLC", onClick = onPlayVlc)
                DialogActionButton(text = "▶ Reproducir en Reproductor Integrado (ExoPlayer)", onClick = onPlayInternal)
                DialogActionButton(text = "▶ Reproducir en Kodi", onClick = onPlayKodi)
                DialogActionButton(
                    text = if (session.paused) "⏯ Reanudar descarga de sesión" else "⏸ Pausar sesión en el servidor",
                    onClick = onTogglePause
                )

                if (session.files.size > 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Seleccionar capítulo / archivo (${session.files.size}):",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    TvLazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
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
    BackHandler { onDismiss() }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF111726),
            modifier = Modifier
                .width(450.dp)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.subtitle,
                    color = Color(0xFF00A4DC),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                val resumeLabel = if (item.resumePositionMs > 0L) {
                    " (${formatDuration(item.resumePositionMs)})"
                } else ""

                DialogActionButton(text = "▶ Reproducir en VLC$resumeLabel", onClick = onPlayVlc)
                DialogActionButton(text = "▶ Reproducir en Reproductor Integrado$resumeLabel", onClick = onPlayInternal)
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
    BackHandler { onDismiss() }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF111726),
            modifier = Modifier
                .width(440.dp)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Aplicaciones Ocultas (${hiddenApps.size})",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pulsa sobre cualquier aplicación para volver a mostrarla en la pantalla principal:",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp
                )
                TvLazyColumn(
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
                DialogActionButton(text = "Cerrar", onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun ServerSettingsDialog(
    current: LauncherSettings,
    onSave: (LauncherSettings) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }
    var selectedPlayer by remember { mutableStateOf(current.preferredPlayer) }
    var jfUrl by remember { mutableStateOf(current.jellyfinUrl) }
    var tsUrl by remember { mutableStateOf(current.torrentStreamUrl) }
    var jfApiKey by remember { mutableStateOf(current.jellyfinApiKey) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF111726),
            modifier = Modifier
                .width(520.dp)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Ajustes del Servidor y Reproductor",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Reproductor preferido al pulsar OK:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )

                PreferredPlayer.entries.forEach { option ->
                    val prefix = if (selectedPlayer == option) "● " else "○ "
                    DialogActionButton(
                        text = "$prefix${option.label}",
                        onClick = { selectedPlayer = option }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Servidores Homelab:",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = tsUrl,
                    onValueChange = { tsUrl = it },
                    label = { Text("URL Torrent Stream Web", color = Color.White.copy(alpha = 0.6f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = jfUrl,
                    onValueChange = { jfUrl = it },
                    label = { Text("URL Jellyfin", color = Color.White.copy(alpha = 0.6f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00A4DC),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = jfApiKey,
                    onValueChange = { jfApiKey = it },
                    label = { Text("API Key de Jellyfin", color = Color.White.copy(alpha = 0.6f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00A4DC),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

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
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) Color.White else Color.White.copy(alpha = 0.08f))
            .combinedClickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 16.dp, vertical = 11.dp)
    ) {
        Text(
            text = text,
            color = if (focused) Color(0xFF090D16) else Color.White,
            fontSize = 14.sp,
            fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium
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

private fun drawableToBitmap(drawable: Drawable, maxW: Int, maxH: Int): Bitmap? {
    return try {
        val w = drawable.intrinsicWidth.takeIf { it > 0 }?.coerceAtMost(maxW) ?: maxW
        val h = drawable.intrinsicHeight.takeIf { it > 0 }?.coerceAtMost(maxH) ?: maxH
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    } catch (_: Throwable) {
        null
    }
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
