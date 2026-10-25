package com.opentvbox.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.opentvbox.app.R
import com.opentvbox.app.core.AppHubManager
import com.opentvbox.app.core.HardwareInspector
import com.opentvbox.app.core.PlayStoreUnlocker
import com.opentvbox.app.core.RootExecutor
import com.opentvbox.app.core.TvAppItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val activityScope = CoroutineScope(Dispatchers.Main + Job())

    private lateinit var tvDeviceSoc: TextView
    private lateinit var tvDeviceOs: TextView
    private lateinit var tvRootStatus: TextView
    private lateinit var tvConsoleLog: TextView
    private lateinit var scrollConsole: ScrollView
    private lateinit var progressBar: ProgressBar
    private lateinit var containerAppHub: LinearLayout

    private lateinit var cardUnlockPlaystore: View
    private lateinit var cardInstallSmarttube: View
    private lateinit var cardSystemUtils: View
    private lateinit var btnRunUnlock: Button
    private lateinit var btnRunSmarttube: Button
    private lateinit var btnRunReboot: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        loadHardwareSpecs()
        setupActionListeners()
        populateAppHub()

        // Give initial TV focus to the Play Store Unlock Card
        cardUnlockPlaystore.requestFocus()
    }

    private fun initViews() {
        tvDeviceSoc = findViewById(R.id.tv_device_soc)
        tvDeviceOs = findViewById(R.id.tv_device_os)
        tvRootStatus = findViewById(R.id.tv_root_status)
        tvConsoleLog = findViewById(R.id.tv_console_log)
        scrollConsole = findViewById(R.id.scroll_console)
        progressBar = findViewById(R.id.progress_bar)
        containerAppHub = findViewById(R.id.container_app_hub)

        cardUnlockPlaystore = findViewById(R.id.card_unlock_playstore)
        cardInstallSmarttube = findViewById(R.id.card_install_smarttube)
        cardSystemUtils = findViewById(R.id.card_system_utils)

        btnRunUnlock = findViewById(R.id.btn_run_unlock)
        btnRunSmarttube = findViewById(R.id.btn_run_smarttube)
        btnRunReboot = findViewById(R.id.btn_run_reboot)
    }

    private fun loadHardwareSpecs() {
        val hw = HardwareInspector.inspect(this)
        tvDeviceSoc.text = "SoC: ${hw.socModel}"
        tvDeviceOs.text = "${hw.androidVersion} | RAM: ${hw.totalRamMb}MB (${hw.availableRamMb}MB free)"

        if (hw.isRooted) {
            tvRootStatus.text = "Root: Active (SU binary found)\nStorage: ${hw.freeStorageGb}GB free / ${hw.totalStorageGb}GB"
            tvRootStatus.setTextColor(getColor(R.color.tv_accent_emerald))
        } else {
            tvRootStatus.text = "Root: Not detected (Standard mode)\nStorage: ${hw.freeStorageGb}GB free"
            tvRootStatus.setTextColor(getColor(R.color.tv_accent_amber))
        }

        log("Device detected: ${hw.socModel} running ${hw.androidVersion} (API ${hw.apiLevel})")
        log("Google Play Store: ${hw.playStoreVersion} | Leanback XML: ${if (hw.isLeanbackActive) "INJECTED" else "NOT FOUND"}")
    }

    private fun setupActionListeners() {
        val runUnlockAction = View.OnClickListener {
            runPlayStoreUnlock()
        }
        cardUnlockPlaystore.setOnClickListener(runUnlockAction)
        btnRunUnlock.setOnClickListener(runUnlockAction)

        val runSmarttubeAction = View.OnClickListener {
            val smarttube = AppHubManager.getCuratedApps(this).firstOrNull { it.id == "smarttube" }
            if (smarttube != null) {
                downloadAndInstallApp(smarttube)
            }
        }
        cardInstallSmarttube.setOnClickListener(runSmarttubeAction)
        btnRunSmarttube.setOnClickListener(runSmarttubeAction)

        val runRebootAction = View.OnClickListener {
            log("Attempting soft reboot...")
            activityScope.launch(Dispatchers.IO) {
                val res = RootExecutor.execute("reboot || setprop ctl.restart zygote", asRoot = true)
                withContext(Dispatchers.Main) {
                    if (!res.success) {
                        log("Reboot command failed: ${res.stderr}. Please power cycle using remote or plug.")
                        Toast.makeText(this@MainActivity, "Reboot failed. Power cycle device manually.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        cardSystemUtils.setOnClickListener(runRebootAction)
        btnRunReboot.setOnClickListener(runRebootAction)
    }

    private fun runPlayStoreUnlock() {
        log("=== Starting 1-Click Play Store Compatibility Unlock ===")
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 20

        activityScope.launch(Dispatchers.IO) {
            val report = PlayStoreUnlocker.unlockCompatibility()
            withContext(Dispatchers.Main) {
                progressBar.progress = 100
                report.steps.forEach { step ->
                    val status = if (step.success) "[SUCCESS]" else "[FAILED]"
                    log("$status ${step.title}: ${step.details}")
                }
                log("Result: ${report.summary}")
                Toast.makeText(this@MainActivity, report.summary, Toast.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
                loadHardwareSpecs()
            }
        }
    }

    private fun downloadAndInstallApp(app: TvAppItem) {
        log("=== Downloading ${app.name} ===")
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0

        activityScope.launch(Dispatchers.IO) {
            val apk = AppHubManager.downloadApk(this@MainActivity, app) { progress ->
                activityScope.launch(Dispatchers.Main) {
                    progressBar.progress = progress
                    if (progress % 25 == 0) {
                        log("Downloading ${app.name}: $progress%")
                    }
                }
            }

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                if (apk != null && apk.exists()) {
                    log("Download complete: ${apk.name} (${apk.length() / (1024 * 1024)} MB)")
                    log("Triggering installer...")
                    AppHubManager.installApk(this@MainActivity, apk)
                    populateAppHub()
                } else {
                    log("Download failed for ${app.name}. Please check internet connection.")
                    Toast.makeText(this@MainActivity, "Download failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun populateAppHub() {
        containerAppHub.removeAllViews()
        val inflater = LayoutInflater.from(this)
        val apps = AppHubManager.getCuratedApps(this)

        for (app in apps) {
            val view = inflater.inflate(R.layout.item_tv_app, containerAppHub, false)
            val tvCategory: TextView = view.findViewById(R.id.tv_app_category)
            val tvTitle: TextView = view.findViewById(R.id.tv_app_title)
            val tvDesc: TextView = view.findViewById(R.id.tv_app_desc)
            val btnAction: Button = view.findViewById(R.id.btn_app_action)

            tvCategory.text = app.category.uppercase()
            tvTitle.text = app.name
            tvDesc.text = app.description

            if (app.isInstalled) {
                btnAction.text = "Open App"
                btnAction.setTextColor(getColor(R.color.tv_accent_emerald))
            } else {
                btnAction.text = "Install"
                btnAction.setTextColor(getColor(R.color.tv_text_primary))
            }

            val clickListener = View.OnClickListener {
                if (app.isInstalled) {
                    val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
                    if (launchIntent != null) {
                        log("Launching ${app.name}...")
                        startActivity(launchIntent)
                    } else {
                        log("Unable to find launch Intent for ${app.packageName}")
                    }
                } else {
                    downloadAndInstallApp(app)
                }
            }

            view.setOnClickListener(clickListener)
            btnAction.setOnClickListener(clickListener)
            containerAppHub.addView(view)
        }
    }

    private fun log(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val current = tvConsoleLog.text.toString()
        val updated = "$current\n[$time] $message"
        tvConsoleLog.text = updated
        scrollConsole.post { scrollConsole.fullScroll(View.FOCUS_DOWN) }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // TV Remote D-Pad Center or Enter triggers click on currently focused view
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            currentFocus?.performClick()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
