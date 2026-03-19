package com.xoomat.tgwsproxy

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var hostInput: EditText
    private lateinit var portInput: EditText
    private lateinit var dcMappingsInput: EditText
    private lateinit var verboseCheck: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        hostInput = findViewById(R.id.hostInput)
        portInput = findViewById(R.id.portInput)
        dcMappingsInput = findViewById(R.id.dcMappingsInput)
        verboseCheck = findViewById(R.id.verboseCheck)
        findViewById<Button>(R.id.saveButton).setOnClickListener { saveSettings() }
        findViewById<Button>(R.id.resetButton).setOnClickListener { resetSettings() }
        fillFromSaved()
    }

    private fun fillFromSaved() {
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
            toast(getString(R.string.settings_host_required))
            return
        }
        if (port == null || port !in 1..65535) {
            toast(getString(R.string.settings_port_invalid))
            return
        }
        if (dc.isBlank()) {
            toast(getString(R.string.settings_dc_required))
            return
        }

        ProxySettings.save(
            this,
            ProxySettings(host = host, port = port, dcMappings = dc, verbose = verbose)
        )
        toast(getString(R.string.settings_saved))
    }

    private fun resetSettings() {
        ProxySettings.reset(this)
        fillFromSaved()
        toast(getString(R.string.settings_reset_done))
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
