package com.opentvbox.app.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class TvAppItem(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val downloadUrl: String,
    val packageName: String,
    val isInstalled: Boolean = false
)

object AppHubManager {

    /**
     * Curated list of verified, working TV apps that run on Android 7/8/9 with 1GB/2GB RAM
     */
    fun getCuratedApps(context: Context): List<TvAppItem> {
        val pm = context.packageManager
        val apps = listOf(
            TvAppItem(
                id = "smarttube",
                name = "SmartTube (YouTube 4K)",
                category = "Video & Streaming",
                description = "Ad-free 4K YouTube client for Android TV with remote control support & SponsorBlock.",
                downloadUrl = "https://github.com/yuliskov/SmartTube/releases/download/latest/smarttube_stable.apk",
                packageName = "com.teamsmart.videomanager.tv"
            ),
            TvAppItem(
                id = "vlc",
                name = "VLC Media Player",
                category = "Media Player",
                description = "Plays all video & audio codecs from USB drives or local network shares.",
                downloadUrl = "https://get.videolan.org/vlc-android/3.5.4/VLC-Android-3.5.4-arm64-v8a.apk",
                packageName = "org.videolan.vlc"
            ),
            TvAppItem(
                id = "stremio",
                name = "Stremio for Android TV",
                category = "Streaming",
                description = "Organize, watch, and stream your favorite TV series and movies.",
                downloadUrl = "https://dl.strem.io/four/v1.6.12/stremio-1.6.12-arm64-v8a.apk",
                packageName = "com.stremio.one"
            ),
            TvAppItem(
                id = "nova",
                name = "Nova Video Player",
                category = "Media Player",
                description = "Lightweight open-source player with automatic movie poster and subtitle scrapers.",
                downloadUrl = "https://github.com/nova-video-player/aos-AVP/releases/download/v6.2.62/NovaVideoPlayer-v6.2.62-arm64-v8a.apk",
                packageName = "org.courville.nova"
            ),
            TvAppItem(
                id = "kodi",
                name = "Kodi 21 (Omega)",
                category = "Home Theater",
                description = "Ultimate home theater system for local media, network streaming, and addons.",
                downloadUrl = "https://mirrors.kodi.tv/releases/android/arm64-v8a/kodi-21.0-Omega-arm64-v8a.apk",
                packageName = "org.xbmc.kodi"
            )
        )

        return apps.map { app ->
            val installed = try {
                pm.getPackageInfo(app.packageName, 0)
                true
            } catch (e: Exception) {
                false
            }
            app.copy(isInstalled = installed)
        }
    }

    /**
     * Download APK file asynchronously with progress callback
     */
    suspend fun downloadApk(
        context: Context,
        appItem: TvAppItem,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val url = URL(appItem.downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.connect()

            val fileLength = connection.contentLength
            val cacheDir = context.externalCacheDir ?: context.cacheDir
            val outputFile = File(cacheDir, "${appItem.id}.apk")

            connection.inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val data = ByteArray(8192)
                    var total: Long = 0
                    var count: Int
                    while (input.read(data).also { count = it } != -1) {
                        total += count
                        if (fileLength > 0) {
                            onProgress((total * 100 / fileLength).toInt())
                        }
                        output.write(data, 0, count)
                    }
                }
            }
            outputFile
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Install downloaded APK file (either via root silent install or standard PackageInstaller Intent)
     */
    fun installApk(context: Context, apkFile: File) {
        // Try root silent install first
        if (RootExecutor.isRootAvailable()) {
            val cmd = "pm install -r \"${apkFile.absolutePath}\""
            val res = RootExecutor.execute(cmd, asRoot = true)
            if (res.success) return
        }

        // Standard Intent fallback
        val intent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
            } else {
                Uri.fromFile(apkFile)
            }
            setDataAndType(uri, "application/vnd.android.package-archive")
        }
        context.startActivity(intent)
    }
}
