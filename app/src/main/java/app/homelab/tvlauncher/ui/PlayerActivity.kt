package app.homelab.tvlauncher.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : ComponentActivity() {

    private var player: ExoPlayer? = null

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL) ?: run {
            finish()
            return
        }
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reproduciendo"
        val startPositionMs = intent.getLongExtra(EXTRA_START_MS, 0L)

        val playerView = PlayerView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            useController = true
            setShowSubtitleButton(true)
            setShowNextButton(false)
            setShowPreviousButton(false)
            keepScreenOn = true
        }
        setContentView(playerView)

        val exoPlayer = ExoPlayer.Builder(this).build().also { exo ->
            playerView.player = exo
            val item = MediaItem.Builder()
                .setUri(Uri.parse(streamUrl))
                .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
                .build()
            exo.setMediaItem(item)
            if (startPositionMs > 0L) {
                exo.seekTo(startPositionMs)
            }
            exo.prepare()
            exo.playWhenReady = true
        }
        player = exoPlayer
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }

    companion object {
        private const val EXTRA_STREAM_URL = "extra_stream_url"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_START_MS = "extra_start_ms"

        fun launch(context: Context, streamUrl: String, title: String, startPositionMs: Long = 0L) {
            val intent = Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_STREAM_URL, streamUrl)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_START_MS, startPositionMs)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}

object ExternalPlayerLauncher {

    private const val PKG_VLC = "org.videolan.vlc"
    private const val PKG_KODI = "org.xbmc.kodi"
    private const val PKG_JELLYFIN_TV = "org.jellyfin.androidtv"

    fun isPackageInstalled(context: Context, packageName: String): Boolean =
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Throwable) {
            false
        }

    fun openInVlc(context: Context, streamUrl: String, title: String, startPositionMs: Long = 0L): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setPackage(PKG_VLC)
                setDataAndTypeAndNormalize(Uri.parse(streamUrl), "video/*")
                putExtra("title", title)
                if (startPositionMs > 0L) {
                    putExtra("from_start", false)
                    putExtra("position", startPositionMs)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Throwable) {
            openInSystemVideoPlayer(context, streamUrl, title, startPositionMs)
        }
    }

    fun openInKodi(context: Context, streamUrl: String, title: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setPackage(PKG_KODI)
                setDataAndTypeAndNormalize(Uri.parse(streamUrl), "video/*")
                putExtra("title", title)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Throwable) {
            openInSystemVideoPlayer(context, streamUrl, title, 0L)
        }
    }

    fun openJellyfinApp(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            val intent = pm.getLeanbackLaunchIntentForPackage(PKG_JELLYFIN_TV)
                ?: pm.getLaunchIntentForPackage(PKG_JELLYFIN_TV)
                ?: return false
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun openInSystemVideoPlayer(context: Context, streamUrl: String, title: String, startPositionMs: Long): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndTypeAndNormalize(Uri.parse(streamUrl), "video/*")
                putExtra("title", title)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Throwable) {
            PlayerActivity.launch(context, streamUrl, title, startPositionMs)
            true
        }
    }
}
