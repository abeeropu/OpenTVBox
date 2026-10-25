package com.opentvbox.app.core

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

data class HardwareReport(
    val socModel: String,
    val androidVersion: String,
    val apiLevel: Int,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val totalStorageGb: Double,
    val freeStorageGb: Double,
    val isRooted: Boolean,
    val isLeanbackActive: Boolean,
    val playStoreVersion: String
)

object HardwareInspector {

    fun inspect(context: Context): HardwareReport {
        // 1. RAM info
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val totalRam = memInfo.totalMem / (1024 * 1024)
        val availRam = memInfo.availMem / (1024 * 1024)

        // 2. Storage info
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availBlocks = stat.availableBlocksLong
        val totalStorageGb = (blockSize * totalBlocks).toDouble() / (1024 * 1024 * 1024)
        val freeStorageGb = (blockSize * availBlocks).toDouble() / (1024 * 1024 * 1024)

        // 3. SoC detection
        val soc = detectSoc()

        // 4. Android Version
        val androidVer = "Android ${Build.VERSION.RELEASE} (${getAndroidCodename(Build.VERSION.SDK_INT)})"

        // 5. Root status
        val rooted = RootExecutor.isRootAvailable()

        // 6. Leanback XML status
        val leanback = PlayStoreUnlocker.isLeanbackInjected()

        // 7. Play Store version
        val playStoreVer = try {
            val pInfo = context.packageManager.getPackageInfo("com.android.vending", 0)
            pInfo.versionName ?: "Installed"
        } catch (e: PackageManager.NameNotFoundException) {
            "Not Installed / Corrupted"
        }

        return HardwareReport(
            socModel = soc,
            androidVersion = androidVer,
            apiLevel = Build.VERSION.SDK_INT,
            totalRamMb = totalRam,
            availableRamMb = availRam,
            totalStorageGb = Math.round(totalStorageGb * 10.0) / 10.0,
            freeStorageGb = Math.round(freeStorageGb * 10.0) / 10.0,
            isRooted = rooted,
            isLeanbackActive = leanback,
            playStoreVersion = playStoreVer
        )
    }

    private fun detectSoc(): String {
        // Try reading getprop ro.board.platform
        val platformProp = RootExecutor.execute("getprop ro.board.platform", asRoot = false).stdout
        if (platformProp.isNotBlank()) {
            return when {
                platformProp.contains("gxl", ignoreCase = true) -> "Amlogic S905W / S905X (GXL)"
                platformProp.contains("meson", ignoreCase = true) -> "Amlogic Meson ($platformProp)"
                platformProp.contains("rk", ignoreCase = true) -> "Rockchip ($platformProp)"
                platformProp.contains("sunxi", ignoreCase = true) -> "Allwinner ($platformProp)"
                else -> platformProp.uppercase()
            }
        }

        // Try reading /proc/cpuinfo Hardware line
        try {
            val cpuInfo = File("/proc/cpuinfo")
            if (cpuInfo.exists()) {
                BufferedReader(FileReader(cpuInfo)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        if (line!!.startsWith("Hardware", ignoreCase = true)) {
                            val parts = line!!.split(":")
                            if (parts.size > 1) return parts[1].trim()
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return Build.HARDWARE ?: "ARM Cortex-A53 (Generic)"
    }

    private fun getAndroidCodename(api: Int): String {
        return when (api) {
            21, 22 -> "Lollipop"
            23 -> "Marshmallow"
            24, 25 -> "Nougat"
            26, 27 -> "Oreo"
            28 -> "Pie"
            29 -> "Android 10"
            30 -> "Android 11"
            31, 32 -> "Android 12"
            33 -> "Android 13"
            34 -> "Android 14"
            else -> "API $api"
        }
    }
}
