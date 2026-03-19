package com.xoomat.tgwsproxy

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var statusView: TextView
    private lateinit var toggleButton: Button

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ProxyService.ACTION_STATUS) return
            val running = intent.getBooleanExtra(ProxyService.EXTRA_RUNNING, false)
            val error = intent.getStringExtra(ProxyService.EXTRA_ERROR).orEmpty()
            renderState(running)
            if (!running && error.isNotBlank()) {
                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupMinimalUi()
        maybeRequestNotificationPermission()
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
        renderState(ProxyService.isRunning && ProxyController.isRunning())
    }

    override fun onPause() {
        super.onPause()
        runCatching { unregisterReceiver(statusReceiver) }
    }

    private fun setupMinimalUi() {
        val pad = (20 * resources.displayMetrics.density).toInt()
        val spacing = (10 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        val title = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 24f
        }

        statusView = TextView(this).apply {
            textSize = 18f
        }

        toggleButton = Button(this).apply {
            setOnClickListener { toggleProxy() }
        }

        val openTelegramButton = Button(this).apply {
            text = getString(R.string.action_open_telegram)
            setOnClickListener { openTelegramProxyLink() }
        }

        val openSettingsButton = Button(this).apply {
            text = getString(R.string.action_open_settings)
            setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
        }

        root.addView(title)
        root.addView(space(this, spacing))
        root.addView(statusView)
        root.addView(space(this, spacing))
        root.addView(toggleButton)
        root.addView(space(this, spacing))
        root.addView(openTelegramButton)
        root.addView(space(this, spacing))
        root.addView(openSettingsButton)

        setContentView(root)
    }

    private fun toggleProxy() {
        val running = ProxyService.isRunning && ProxyController.isRunning()
        val intent = Intent(this, ProxyService::class.java).apply {
            action = if (running) ProxyService.ACTION_STOP else ProxyService.ACTION_START
        }
        if (!running && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        renderState(!running)
    }

    private fun renderState(running: Boolean) {
        statusView.text = if (running) {
            getString(R.string.status_running)
        } else {
            getString(R.string.status_stopped)
        }
        toggleButton.text = if (running) {
            getString(R.string.action_stop_proxy)
        } else {
            getString(R.string.action_start_proxy)
        }
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

    private fun space(context: Context, heightPx: Int): TextView {
        return TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                heightPx
            )
        }
    }
}
