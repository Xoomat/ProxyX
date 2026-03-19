package com.xoomat.tgwsproxy

import android.content.Context
import com.chaquo.python.PyException
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

object ProxyController {
    private const val MODULE_NAME = "android_proxy"
    private var module: PyObject? = null

    private fun ensurePython(context: Context) {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context.applicationContext))
        }
        if (module == null) {
            module = Python.getInstance().getModule(MODULE_NAME)
        }
    }

    fun start(context: Context): Result<Unit> {
        return try {
            ensurePython(context)
            val settings = ProxySettings.load(context)
            val dcList = settings.dcMappings
                .replace("\r", "")
                .lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString(",")
            val started = module?.callAttr(
                "start",
                settings.port,
                settings.host,
                dcList,
                settings.verbose
            )?.toBoolean() ?: false
            if (!started) {
                val err = module?.callAttr("last_error")?.toString().orEmpty()
                return Result.failure(
                    IllegalStateException(err.ifBlank { "proxy thread failed to start" })
                )
            }
            Result.success(Unit)
        } catch (e: PyException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun stop(): Result<Unit> {
        return try {
            module?.callAttr("stop")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isRunning(): Boolean {
        return try {
            module?.callAttr("is_running")?.toBoolean() ?: false
        } catch (_: Exception) {
            false
        }
    }

    fun lastError(): String {
        return try {
            module?.callAttr("last_error")?.toString().orEmpty()
        } catch (_: Exception) {
            ""
        }
    }
}
