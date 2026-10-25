package com.opentvbox.app.core

object PlayStoreUnlocker {

    private const val LEANBACK_XML_PATH = "/system/etc/permissions/android.software.leanback.xml"
    private const val LEANBACK_XML_CONTENT = """<?xml version="1.0" encoding="utf-8"?>
<permissions>
    <feature name="android.software.leanback" />
    <feature name="android.software.live_tv" />
    <feature name="android.hardware.type.television" />
</permissions>"""

    /**
     * Unlock Google Play Store compatibility in 3 automated steps
     */
    fun unlockCompatibility(): UnlockReport {
        val steps = mutableListOf<UnlockStep>()

        // 1. Remount /system as RW
        val rwOk = RootExecutor.remountSystemRw()
        steps.add(UnlockStep("Mount /system Read-Write", rwOk, if (rwOk) "System mounted as RW" else "Failed to mount RW (Root required)"))

        if (!rwOk) {
            return UnlockReport(false, steps, "Root access required to modify system permissions.")
        }

        // 2. Inject Android TV Leanback Permissions XML
        val injectCommand = "echo '$LEANBACK_XML_CONTENT' > $LEANBACK_XML_PATH && chmod 644 $LEANBACK_XML_PATH"
        val injectRes = RootExecutor.execute(injectCommand, asRoot = true)
        steps.add(UnlockStep("Inject Leanback Permissions", injectRes.success, if (injectRes.success) "Leanback XML installed at $LEANBACK_XML_PATH" else injectRes.stderr))

        // 3. Spoof TV characteristics in build.prop
        val spoofCommand = """
            cp /system/build.prop /system/build.prop.bak &&
            sed -i 's/ro.build.characteristics=.*/ro.build.characteristics=tv/' /system/build.prop || echo "ro.build.characteristics=tv" >> /system/build.prop
        """.trimIndent().replace("\n", " ")
        val spoofRes = RootExecutor.execute(spoofCommand, asRoot = true)
        steps.add(UnlockStep("Spoof Android TV Profile", spoofRes.success, if (spoofRes.success) "Characteristics set to 'tv'" else spoofRes.stderr))

        // 4. Flush Google Play Store and Services cache
        val clearCmd = "pm clear com.android.vending && pm clear com.google.android.gms"
        val clearRes = RootExecutor.execute(clearCmd, asRoot = true)
        steps.add(UnlockStep("Flush Play Store Cache", clearRes.success, "Cleared cached device incompatibility records."))

        // 5. Remount back to RO for safety
        RootExecutor.remountSystemRo()

        val allOk = steps.all { it.success }
        return UnlockReport(allOk, steps, if (allOk) "TV Box unlocked successfully! Please reboot your device." else "Some steps failed.")
    }

    /**
     * Quick check if Leanback feature is present on system
     */
    fun isLeanbackInjected(): Boolean {
        val res = RootExecutor.execute("cat $LEANBACK_XML_PATH", asRoot = false)
        return res.success && res.stdout.contains("android.software.leanback")
    }
}

data class UnlockStep(
    val title: String,
    val success: Boolean,
    val details: String
)

data class UnlockReport(
    val success: Boolean,
    val steps: List<UnlockStep>,
    val summary: String
)
