package com.xoomat.tgwsproxy

import android.animation.ValueAnimator
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import android.app.AlertDialog
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var stateText: TextView
    private lateinit var statusValueText: TextView
    private lateinit var versionText: TextView
    private lateinit var proxyToggleCard: FrameLayout
    private lateinit var stateIconPanel: LinearLayout
    private lateinit var stateIcon: ImageView
    private lateinit var addProxyCard: LinearLayout
    private lateinit var checkUpdateCard: LinearLayout
    private lateinit var homeTab: android.view.View
    private lateinit var settingsTab: android.view.View
    private lateinit var navHome: LinearLayout
    private lateinit var navSettings: LinearLayout
    private lateinit var navHomeText: TextView
    private lateinit var navSettingsText: TextView
    private lateinit var navHomeIcon: ImageView
    private lateinit var navSettingsIcon: ImageView
    private lateinit var hostInput: EditText
    private lateinit var portInput: EditText
    private lateinit var dcMappingsInput: EditText
    private lateinit var verboseCheck: CheckBox
    private lateinit var saveSettingsButton: Button
    private lateinit var resetSettingsButton: Button
    private val io = Executors.newSingleThreadExecutor()

    private enum class Tab {
        HOME,
        SETTINGS
    }

    data class UpdateInfo(
        val tag: String,
        val apkUrl: String?,
        val releaseUrl: String
    )

    private val uiHandler = Handler(Looper.getMainLooper())

    private val statusPoll = object : Runnable {
        override fun run() {
            renderState(ProxyService.isRunning && ProxyController.isRunning(), animate = false)
            uiHandler.postDelayed(this, 700)
        }
    }

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ProxyService.ACTION_STATUS) return
            val running = intent.getBooleanExtra(ProxyService.EXTRA_RUNNING, false)
            val error = intent.getStringExtra(ProxyService.EXTRA_ERROR).orEmpty()
            if (!running && error.isNotBlank()) {
                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
            }
            renderState(running, animate = true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        stateText = findViewById(R.id.stateText)
        statusValueText = findViewById(R.id.statusValueText)
        versionText = findViewById(R.id.versionText)
        proxyToggleCard = findViewById(R.id.proxyToggleCard)
        stateIconPanel = findViewById(R.id.stateIconPanel)
        stateIcon = findViewById(R.id.stateIcon)
        addProxyCard = findViewById(R.id.addProxyCard)
        checkUpdateCard = findViewById(R.id.checkUpdateCard)
        homeTab = findViewById(R.id.homeTab)
        settingsTab = findViewById(R.id.settingsTab)
        navHome = findViewById(R.id.navHome)
        navSettings = findViewById(R.id.navSettings)
        navHomeText = findViewById(R.id.navHomeText)
        navSettingsText = findViewById(R.id.navSettingsText)
        navHomeIcon = findViewById(R.id.navHomeIcon)
        navSettingsIcon = findViewById(R.id.navSettingsIcon)
        hostInput = findViewById(R.id.hostInput)
        portInput = findViewById(R.id.portInput)
        dcMappingsInput = findViewById(R.id.dcMappingsInput)
        verboseCheck = findViewById(R.id.verboseCheck)
        saveSettingsButton = findViewById(R.id.saveSettingsButton)
        resetSettingsButton = findViewById(R.id.resetSettingsButton)

        proxyToggleCard.setOnClickListener { toggleProxy() }
        addProxyCard.setOnClickListener { openTelegramProxyLink() }
        checkUpdateCard.setOnClickListener { checkForUpdates(true) }
        navHome.setOnClickListener { switchTab(Tab.HOME) }
        navSettings.setOnClickListener { switchTab(Tab.SETTINGS) }
        saveSettingsButton.setOnClickListener { saveSettings() }
        resetSettingsButton.setOnClickListener { resetSettings() }
        versionText.text = getString(R.string.version_current, currentVersionTag())

        fillSettingsFromSaved()
        switchTab(Tab.HOME)
        maybeRequestNotificationPermission()
        checkForUpdates(false)
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                statusReceiver,
                IntentFilter(ProxyService.ACTION_STATUS),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(statusReceiver, IntentFilter(ProxyService.ACTION_STATUS))
        }
        uiHandler.post(statusPoll)
        renderState(ProxyService.isRunning && ProxyController.isRunning(), animate = false)
    }

    override fun onPause() {
        super.onPause()
        runCatching { unregisterReceiver(statusReceiver) }
        uiHandler.removeCallbacks(statusPoll)
    }

    override fun onDestroy() {
        super.onDestroy()
        io.shutdownNow()
    }

    private fun toggleProxy() {
        val currentlyOn = ProxyService.isRunning && ProxyController.isRunning()
        if (currentlyOn) {
            renderState(false, animate = true)
            stopProxyService()
        } else {
            renderState(true, animate = true)
            startProxyService()
        }
    }

    private fun startProxyService() {
        val intent = Intent(this, ProxyService::class.java).apply { action = ProxyService.ACTION_START }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    private fun stopProxyService() {
        val intent = Intent(this, ProxyService::class.java).apply { action = ProxyService.ACTION_STOP }
        startService(intent)
    }

    private fun renderState(on: Boolean, animate: Boolean) {
        stateText.text = if (on) getString(R.string.state_on) else getString(R.string.state_off)
        statusValueText.text = if (on) getString(R.string.proxy_status_on) else getString(R.string.proxy_status_off)
        statusValueText.setTextColor(if (on) 0xFF2BFF5B.toInt() else 0xFFD12222.toInt())
        stateIcon.setImageResource(if (on) R.drawable.proxy_on_icon else R.drawable.proxy_off_icon)
        stateIconPanel.setBackgroundResource(if (on) R.drawable.bg_state_on else R.drawable.bg_state_off)

        if (animate) {
            animateToggle(on)
        } else {
            applyToggleLayout(on, 1f)
        }
    }

    private fun animateToggle(toOn: Boolean) {
        if (proxyToggleCard.width == 0) {
            proxyToggleCard.post { animateToggle(toOn) }
            return
        }
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 280
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener {
            val p = it.animatedValue as Float
            applyToggleLayout(toOn, p)
        }
        animator.start()
    }

    private fun applyToggleLayout(on: Boolean, progress: Float) {
        val horizontalPad = dp(12f)
        val containerW = proxyToggleCard.width - horizontalPad * 2
        if (containerW <= 0) return

        val startW = dp(150f)
        val endW = containerW - dp(12f)

        val fromW = if (on) startW else endW
        val toW = if (on) endW else startW
        val w = (fromW + (toW - fromW) * progress).toInt()

        val fromX = if (on) 0f else (containerW - endW) / 2f
        val toX = if (on) (containerW - endW) / 2f else 0f
        val tx = fromX + (toX - fromX) * progress

        val lp = stateIconPanel.layoutParams as FrameLayout.LayoutParams
        lp.width = w
        stateIconPanel.layoutParams = lp
        stateIconPanel.translationX = tx
    }

    private fun openTelegramProxyLink() {
        val settings = ProxySettings.load(this)
        val uri = Uri.parse("tg://socks?server=${settings.host}&port=${settings.port}")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.telegram_not_found), Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkForUpdates(userInitiated: Boolean) {
        if (userInitiated) {
            Toast.makeText(this, getString(R.string.checking_updates), Toast.LENGTH_SHORT).show()
        }
        io.execute {
            val info = fetchLatestRelease()
            runOnUiThread {
                if (info == null) {
                    if (userInitiated) {
                        Toast.makeText(this, getString(R.string.update_check_failed), Toast.LENGTH_SHORT).show()
                    }
                    return@runOnUiThread
                }
                val localTag = currentVersionTag()
                versionText.text = getString(R.string.version_with_latest, localTag, info.tag)
                if (isNewerVersion(info.tag, currentVersionName(), currentVersionCode())) {
                    if (userInitiated) {
                        showUpdateDialog(info, localTag)
                    }
                } else if (userInitiated) {
                    Toast.makeText(this, getString(R.string.latest_installed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchLatestRelease(): UpdateInfo? {
        val apiUrl = "https://api.github.com/repos/Xoomat/ProxyX/releases/latest"
        val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 7000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "proxyX-android")
        }
        return try {
            if (conn.responseCode !in 200..299) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tag = json.optString("tag_name").ifBlank { return null }
            val htmlUrl = json.optString("html_url")
            val assets = json.optJSONArray("assets")
            var apkUrl: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    val url = a.optString("browser_download_url")
                    if (url.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = url
                        break
                    }
                }
            }
            UpdateInfo(tag = normalizeTag(tag), apkUrl = apkUrl, releaseUrl = htmlUrl)
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun showUpdateDialog(info: UpdateInfo, currentTag: String) {
        val message = getString(R.string.update_available_body, info.tag, currentTag)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.update_available_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.update_now)) { _, _ ->
                val target = info.apkUrl ?: info.releaseUrl
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target))) }
            }
            .setNegativeButton(getString(R.string.update_later), null)
            .show()
    }

    private fun normalizeTag(raw: String): String {
        val t = raw.trim()
        val noPrefix = if (t.startsWith("v", ignoreCase = true)) t.drop(1) else t
        return "v$noPrefix"
    }

    private fun isNewerVersion(remoteTag: String, localVersionName: String, localVersionCode: Long): Boolean {
        val remoteCode = extractVersionCode(remoteTag)
        if (remoteCode != null) {
            if (remoteCode > localVersionCode) return true
            if (remoteCode < localVersionCode) return false
        }
        return compareVersionNames(extractVersionName(remoteTag), localVersionName) > 0
    }

    private fun compareVersionNames(a: String, b: String): Int {
        val ra = a.split('.', '-')
        val rb = b.split('.', '-')
        val max = maxOf(ra.size, rb.size)
        for (i in 0 until max) {
            val av = ra.getOrNull(i)?.toIntOrNull() ?: 0
            val bv = rb.getOrNull(i)?.toIntOrNull() ?: 0
            if (av > bv) return 1
            if (av < bv) return -1
        }
        return 0
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }
    }

    private fun currentVersionName(): String {
        return try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName ?: "0.0.0"
        } catch (_: Exception) {
            "0.0.0"
        }
    }

    private fun currentVersionCode(): Long {
        return try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (_: Exception) {
            0L
        }
    }

    private fun currentVersionTag(): String {
        return "v${currentVersionName()}+${currentVersionCode()}"
    }

    private fun extractVersionName(tag: String): String {
        return tag.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore('+')
    }

    private fun extractVersionCode(tag: String): Long? {
        val suffix = tag.substringAfter('+', "")
        return suffix.toLongOrNull()
    }

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()

    private fun switchTab(tab: Tab) {
        val homeSelected = tab == Tab.HOME
        homeTab.visibility = if (homeSelected) android.view.View.VISIBLE else android.view.View.GONE
        settingsTab.visibility = if (homeSelected) android.view.View.GONE else android.view.View.VISIBLE

        navHomeText.setTextColor(if (homeSelected) 0xFF4EA7FF.toInt() else 0xFF909AA8.toInt())
        navSettingsText.setTextColor(if (homeSelected) 0xFF909AA8.toInt() else 0xFF4EA7FF.toInt())
        navHomeIcon.alpha = if (homeSelected) 1f else 0.62f
        navSettingsIcon.alpha = if (homeSelected) 0.62f else 1f
    }

    private fun fillSettingsFromSaved() {
        val s = ProxySettings.load(this)
        hostInput.setText(s.host)
        portInput.setText(s.port.toString())
        dcMappingsInput.setText(s.dcMappings)
        verboseCheck.isChecked = s.verbose
    }

    private fun saveSettings() {
        val host = hostInput.text.toString().trim()
        val port = portInput.text.toString().trim().toIntOrNull()
        val dc = dcMappingsInput.text.toString().trim()
        val verbose = verboseCheck.isChecked

        if (host.isBlank()) {
            Toast.makeText(this, getString(R.string.settings_host_required), Toast.LENGTH_SHORT).show()
            return
        }
        if (port == null || port !in 1..65535) {
            Toast.makeText(this, getString(R.string.settings_port_invalid), Toast.LENGTH_SHORT).show()
            return
        }
        if (dc.isBlank()) {
            Toast.makeText(this, getString(R.string.settings_dc_required), Toast.LENGTH_SHORT).show()
            return
        }

        ProxySettings.save(this, ProxySettings(host = host, port = port, dcMappings = dc, verbose = verbose))
        Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
    }

    private fun resetSettings() {
        ProxySettings.reset(this)
        fillSettingsFromSaved()
        Toast.makeText(this, getString(R.string.settings_reset_done), Toast.LENGTH_SHORT).show()
    }
}
