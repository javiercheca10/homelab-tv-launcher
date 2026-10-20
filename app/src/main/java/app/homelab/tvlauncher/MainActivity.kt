package app.homelab.tvlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.homelab.tvlauncher.data.*
import app.homelab.tvlauncher.ui.HomeScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var appRepo: AppRepository
    private val torrentRepo = TorrentStreamRepository()
    private val jellyfinRepo = JellyfinRepository()

    private var visibleAppsState by mutableStateOf<List<TvAppInfo>>(emptyList())
    private var hiddenAppsState by mutableStateOf<List<TvAppInfo>>(emptyList())
    private var torrentSessionsState by mutableStateOf<List<TorrentSession>>(emptyList())
    private var jellyfinResumeState by mutableStateOf<List<JellyfinMediaItem>>(emptyList())
    private var jellyfinLatestState by mutableStateOf<List<JellyfinMediaItem>>(emptyList())
    private var serverOnlineState by mutableStateOf(false)
    private var activeStreamHostState by mutableStateOf("")
    private var settingsState by mutableStateOf<LauncherSettings?>(null)
    private var availableUpdateState by mutableStateOf<AppUpdateInfo?>(null)
    private var updateProgressState by mutableStateOf<Int?>(null)

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshAppsAsync()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appRepo = AppRepository(applicationContext)
        settingsState = appRepo.loadSettings()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Keep launcher active on Home screen
            }
        })

        runCatching {
            val pkgFilter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addDataScheme("package")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.registerReceiver(
                    this,
                    packageReceiver,
                    pkgFilter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            } else {
                registerReceiver(packageReceiver, pkgFilter)
            }
        }

        refreshAppsAsync()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                var consecutiveFailures = 0
                var tick = 0

                while (true) {
                    val currentSettings = settingsState ?: appRepo.loadSettings()
                    val hasTs = currentSettings.torrentStreamUrl.isNotBlank() || currentSettings.tailscaleIp.isNotBlank()
                    val hasJf = currentSettings.jellyfinUrl.isNotBlank() || (currentSettings.tailscaleIp.isNotBlank() && currentSettings.jellyfinApiKey.isNotBlank())

                    var anyAttempted = false
                    var anySuccess = false

                    if (hasTs) {
                        anyAttempted = true
                        val tsResult = torrentRepo.fetchSessions(currentSettings)
                        tsResult.onSuccess { (sessions, baseUrl) ->
                            torrentSessionsState = sessions
                            activeStreamHostState = baseUrl
                            anySuccess = true
                        }.onFailure {
                            torrentSessionsState = emptyList()
                        }
                        if (tick % 8 == 0) {
                            availableUpdateState = torrentRepo.checkAppUpdate(applicationContext, currentSettings)
                        }
                    }

                    if (hasJf && (tick % 6 == 0 || !hasTs)) {
                        anyAttempted = true
                        val jfResult = jellyfinRepo.fetchLibrary(currentSettings)
                        jfResult.onSuccess { (resume, latest) ->
                            jellyfinResumeState = resume
                            jellyfinLatestState = latest
                            anySuccess = true
                        }.onFailure {
                            jellyfinResumeState = emptyList()
                            jellyfinLatestState = emptyList()
                        }
                    }

                    if (anyAttempted) {
                        if (anySuccess) {
                            consecutiveFailures = 0
                            serverOnlineState = true
                        } else {
                            consecutiveFailures++
                            serverOnlineState = false
                        }
                    } else {
                        serverOnlineState = false
                    }

                    tick++

                    // Exponential backoff when server is unreachable (avoids CPU/battery drain on TV)
                    val delayMs = when {
                        consecutiveFailures >= 5 -> 30_000L
                        consecutiveFailures >= 2 -> 12_000L
                        else -> 4_500L
                    }
                    delay(delayMs)
                }
            }
        }

        setContent {
            val currentSettings = settingsState ?: appRepo.loadSettings()
            val favorites = remember(visibleAppsState) { visibleAppsState.filter { it.isFavorite } }

            HomeScreen(
                favoriteApps = favorites,
                allApps = visibleAppsState,
                hiddenApps = hiddenAppsState,
                torrentSessions = torrentSessionsState,
                jellyfinResume = jellyfinResumeState,
                jellyfinLatest = jellyfinLatestState,
                serverOnline = serverOnlineState,
                settings = currentSettings,
                availableUpdate = availableUpdateState,
                updateDownloadProgress = updateProgressState,
                onInstallUpdate = {
                    triggerAppUpdate()
                },
                onLaunchApp = { appRepo.launchApp(it) },
                onToggleFavorite = { app ->
                    appRepo.toggleFavorite(app.packageName)
                    refreshAppsAsync()
                },
                onMoveApp = { app, dir, inFavRow ->
                    appRepo.moveApp(app.packageName, dir, visibleAppsState, inFavRow)
                    refreshAppsAsync()
                },
                onHideApp = { app, hide ->
                    appRepo.setHidden(app.packageName, hide)
                    refreshAppsAsync()
                },
                onOpenAppSettings = { app ->
                    appRepo.openAppSystemSettings(app.packageName)
                },
                onOpenTvInputs = { appRepo.openTvInputs() },
                onOpenWifiSettings = { appRepo.openWifiSettings() },
                onOpenSystemSettings = { appRepo.openSystemSettings() },
                onSaveSettings = { newSettings ->
                    appRepo.saveSettings(newSettings)
                    settingsState = newSettings
                },
                onRefreshNow = {
                    refreshServerNow()
                },
                onTorrentPauseResume = { session ->
                    val host = activeStreamHostState.ifBlank { currentSettings.torrentStreamUrl }
                    lifecycleScope.launch {
                        torrentRepo.pauseOrResume(host, session.id, pause = !session.paused)
                        refreshServerNow()
                    }
                },
                onTorrentSelectFile = { session, fileIdx ->
                    val host = activeStreamHostState.ifBlank { currentSettings.torrentStreamUrl }
                    lifecycleScope.launch {
                        torrentRepo.selectFile(host, session.id, fileIdx)
                        refreshServerNow()
                    }
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshAppsAsync()
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(packageReceiver) }
    }

    private fun refreshAppsAsync() {
        lifecycleScope.launch {
            val (visible, hidden) = withContext(Dispatchers.IO) {
                appRepo.loadApps()
            }
            visibleAppsState = visible
            hiddenAppsState = hidden
        }
    }

    private fun refreshServerNow() {
        val currentSettings = settingsState ?: appRepo.loadSettings()
        lifecycleScope.launch {
            var tsOk = false
            var jfOk = false
            val hasTs = currentSettings.torrentStreamUrl.isNotBlank() || currentSettings.tailscaleIp.isNotBlank()
            val hasJf = currentSettings.jellyfinUrl.isNotBlank() || (currentSettings.tailscaleIp.isNotBlank() && currentSettings.jellyfinApiKey.isNotBlank())

            if (hasTs) {
                torrentRepo.fetchSessions(currentSettings).onSuccess { (sessions, baseUrl) ->
                    torrentSessionsState = sessions
                    activeStreamHostState = baseUrl
                    tsOk = true
                }.onFailure {
                    torrentSessionsState = emptyList()
                }
                availableUpdateState = torrentRepo.checkAppUpdate(applicationContext, currentSettings)
            }

            if (hasJf) {
                jellyfinRepo.fetchLibrary(currentSettings).onSuccess { (resume, latest) ->
                    jellyfinResumeState = resume
                    jellyfinLatestState = latest
                    jfOk = true
                }.onFailure {
                    jellyfinResumeState = emptyList()
                    jellyfinLatestState = emptyList()
                }
            }

            serverOnlineState = tsOk || jfOk
        }
    }

    private fun triggerAppUpdate() {
        if (updateProgressState != null) return
        val currentSettings = settingsState ?: appRepo.loadSettings()
        lifecycleScope.launch {
            updateProgressState = 1
            val result = torrentRepo.downloadAndInstallUpdate(
                context = applicationContext,
                settings = currentSettings,
                updateInfo = availableUpdateState,
                onProgress = { pct -> updateProgressState = pct }
            )
            updateProgressState = null
            result.onFailure { err ->
                Toast.makeText(
                    applicationContext,
                    err.message ?: "Error al descargar la actualización",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
