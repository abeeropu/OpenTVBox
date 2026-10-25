package com.opentvbox.app.core

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader

object RootExecutor {

    private val suPaths = arrayOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/data/local/su"
    )

    /**
     * Check if the device has a working root binary (almost all TV boxes are pre-rooted)
     */
    fun isRootAvailable(): Boolean {
        for (path in suPaths) {
            if (File(path).exists()) return true
        }
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine() != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Execute a shell command with root (su) or standard sh fallback
     */
    fun execute(command: String, asRoot: Boolean = true): CommandResult {
        val shell = if (asRoot && isRootAvailable()) "su" else "sh"
        val output = StringBuilder()
        val error = StringBuilder()

        return try {
            val process = Runtime.getRuntime().exec(shell)
            val os = DataOutputStream(process.outputStream)
            val isReader = BufferedReader(InputStreamReader(process.inputStream))
            val esReader = BufferedReader(InputStreamReader(process.errorStream))

            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()

            var line: String?
            while (isReader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            while (esReader.readLine().also { line = it } != null) {
                error.append(line).append("\n")
            }

            val exitCode = process.waitFor()
            CommandResult(exitCode == 0, output.toString().trim(), error.toString().trim(), exitCode)
        } catch (e: Exception) {
            CommandResult(false, "", e.message ?: "Unknown error", -1)
        }
    }

    /**
     * Remount /system partition as read-write
     */
    fun remountSystemRw(): Boolean {
        val commands = listOf(
            "mount -o remount,rw /system",
            "mount -o remount,rw /",
            "busybox mount -o remount,rw /system"
        )
        for (cmd in commands) {
            val res = execute(cmd, asRoot = true)
            if (res.success) return true
        }
        return false
    }

    /**
     * Remount /system partition back to read-only
     */
    fun remountSystemRo(): Boolean {
        val res = execute("mount -o remount,ro /system", asRoot = true)
        return res.success
    }
}

data class CommandResult(
    val success: Boolean,
    val stdout: String,
    val stderr: String,
    val exitCode: Int
)
