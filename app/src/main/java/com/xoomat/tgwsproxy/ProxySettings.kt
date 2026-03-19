package com.xoomat.tgwsproxy

import android.content.Context

data class ProxySettings(
    val host: String,
    val port: Int,
    val dcMappings: String,
    val verbose: Boolean
) {
    companion object {
        const val DEFAULT_HOST = "127.0.0.1"
        const val DEFAULT_PORT = 1080
        const val DEFAULT_DC_MAPPINGS = "2:149.154.167.220\n4:149.154.167.220"
        const val PREFS_NAME = "proxy_settings"
        private const val KEY_HOST = "host"
        private const val KEY_PORT = "port"
        private const val KEY_DC_MAPPINGS = "dc_mappings"
        private const val KEY_VERBOSE = "verbose"

        fun load(context: Context): ProxySettings {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val host = prefs.getString(KEY_HOST, DEFAULT_HOST).orEmpty().ifBlank { DEFAULT_HOST }
            val port = prefs.getInt(KEY_PORT, DEFAULT_PORT)
            val dcMappings = prefs.getString(KEY_DC_MAPPINGS, DEFAULT_DC_MAPPINGS)
                .orEmpty()
                .ifBlank { DEFAULT_DC_MAPPINGS }
            val verbose = prefs.getBoolean(KEY_VERBOSE, false)
            return ProxySettings(host, port, dcMappings, verbose)
        }

        fun save(context: Context, settings: ProxySettings) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_HOST, settings.host)
                .putInt(KEY_PORT, settings.port)
                .putString(KEY_DC_MAPPINGS, settings.dcMappings)
                .putBoolean(KEY_VERBOSE, settings.verbose)
                .apply()
        }

        fun reset(context: Context) {
            save(
                context,
                ProxySettings(DEFAULT_HOST, DEFAULT_PORT, DEFAULT_DC_MAPPINGS, false)
            )
        }
    }
}
