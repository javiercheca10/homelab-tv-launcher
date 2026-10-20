package app.homelab.tvlauncher.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import app.homelab.tvlauncher.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class TorrentStreamRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Volatile
    private var installedApkSha256: String = ""

    suspend fun checkAppUpdate(
        context: Context,
        settings: LauncherSettings,
        forceReturnIfSame: Boolean = false
    ): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val candidates = buildCandidateUrls(settings.torrentStreamUrl, settings.tailscaleIp)
        val localSha = getInstalledSha256(context)

        for (baseUrl in candidates) {
            try {
                val body = httpGet("$baseUrl/api/tv/update", timeoutMs = 2500)
                val info = json.decodeFromString(AppUpdateInfo.serializer(), body)
                if (!info.available) return@withContext null
                val isNewerOrDifferent = info.versionCode > BuildConfig.VERSION_CODE ||
                    (info.sha256.isNotBlank() && localSha.isNotBlank() && !info.sha256.equals(localSha, ignoreCase = true))
                return@withContext if (isNewerOrDifferent || forceReturnIfSame) {
                    info.copy(downloadUrl = info.downloadUrl.ifBlank { "$baseUrl/apk" })
                } else {
                    null
                }
            } catch (_: Throwable) {
            }
        }
        null
    }

    suspend fun downloadAndInstallUpdate(
        context: Context,
        settings: LauncherSettings,
        updateInfo: AppUpdateInfo?,
        onProgress: (Int) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                val permIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val launched = runCatching {
                    context.startActivity(permIntent)
                    true
                }.getOrDefault(false)
                if (launched) {
                    throw IllegalStateException("Autoriza 'Instalar aplicaciones desconocidas' y vuelve a pulsar Actualizar")
                }
            }

            val resolvedInfo = updateInfo ?: checkAppUpdate(context, settings, forceReturnIfSame = true)
            val candidateUrls = buildList {
                if (resolvedInfo != null && resolvedInfo.downloadUrl.isNotBlank()) {
                    add(resolvedInfo.downloadUrl)
                }
                buildCandidateUrls(settings.torrentStreamUrl, settings.tailscaleIp).forEach { base ->
                    val url = "$base/apk"
                    if (url !in this) add(url)
                }
            }
            if (candidateUrls.isEmpty()) {
                throw IllegalStateException("No hay URL de servidor configurada")
            }

            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val tmpFile = File(updatesDir, "homelab-tv-launcher.apk.tmp")
            val finalFile = File(updatesDir, "homelab-tv-launcher.apk")

            var downloaded = false
            var lastError: Throwable? = null

            for (downloadUrl in candidateUrls) {
                try {
                    val conn = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 5000
                        readTimeout = 15000
                    }
                    try {
                        val code = conn.responseCode
                        if (code !in 200..299) throw IllegalStateException("HTTP $code")
                        val totalBytes = conn.contentLengthLong.takeIf { it > 0L } ?: resolvedInfo?.sizeBytes ?: -1L
                        val digest = MessageDigest.getInstance("SHA-256")
                        var bytesReadTotal = 0L

                        conn.inputStream.use { input ->
                            FileOutputStream(tmpFile).use { output ->
                                val buffer = ByteArray(32 * 1024)
                                var read: Int
                                while (input.read(buffer).also { read = it } != -1) {
                                    output.write(buffer, 0, read)
                                    digest.update(buffer, 0, read)
                                    bytesReadTotal += read
                                    if (totalBytes > 0L) {
                                        val pct = ((bytesReadTotal * 100L) / totalBytes).toInt().coerceIn(1, 99)
                                        onProgress(pct)
                                    }
                                }
                                output.flush()
                            }
                        }

                        if (bytesReadTotal < 100_000L) {
                            throw IllegalStateException("Archivo APK descargado incompleto")
                        }
                        val calculatedSha = digest.digest().joinToString("") { "%02x".format(it) }
                        val expectedSha = resolvedInfo?.sha256.orEmpty()
                        if (expectedSha.isNotBlank() && !expectedSha.equals(calculatedSha, ignoreCase = true)) {
                            throw IllegalStateException("Hash SHA-256 del APK no coincide")
                        }

                        if (finalFile.exists()) finalFile.delete()
                        if (!tmpFile.renameTo(finalFile)) {
                            tmpFile.copyTo(finalFile, overwrite = true)
                            tmpFile.delete()
                        }
                        onProgress(100)
                        downloaded = true
                        break
                    } finally {
                        conn.disconnect()
                    }
                } catch (t: Throwable) {
                    lastError = t
                }
            }

            if (!downloaded) {
                throw lastError ?: IllegalStateException("No se pudo descargar el APK del servidor")
            }

            withContext(Dispatchers.Main) {
                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    finalFile
                )
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(installIntent)
            }
        }
    }

    private fun getInstalledSha256(context: Context): String {
        if (installedApkSha256.isNotBlank()) return installedApkSha256
        return runCatching {
            val apkPath = context.applicationInfo.sourceDir ?: return@runCatching ""
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(File(apkPath)).use { input ->
                val buf = ByteArray(32 * 1024)
                var r: Int
                while (input.read(buf).also { r = it } != -1) {
                    digest.update(buf, 0, r)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }.also {
                installedApkSha256 = it
            }
        }.getOrDefault("")
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
                val cleanBase = normalizeUrl(baseUrl, -1) ?: return@runCatching false
                val action = if (pause) "pause" else "resume"
                httpPost("$cleanBase/api/tv/sessions/$sessionId/$action", "{}")
                true
            }.getOrDefault(false)
        }

    suspend fun selectFile(baseUrl: String, sessionId: String, fileIndex: Int): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleanBase = normalizeUrl(baseUrl, -1) ?: return@runCatching false
                httpPost("$cleanBase/api/tv/sessions/$sessionId/select", """{"index":$fileIndex}""")
                true
            }.getOrDefault(false)
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
