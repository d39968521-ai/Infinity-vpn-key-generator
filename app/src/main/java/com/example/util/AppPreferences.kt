package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.theme.AppColorTheme

object AppPreferences {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_BASE_DOMAIN = "base_domain"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_COLOR_THEME = "color_theme"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_DEVICE_MODE = "device_layout_mode"

    const val DEFAULT_DOMAIN = "https://straustopp.online"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getBaseDomain(context: Context): String {
        val saved = getPrefs(context).getString(KEY_BASE_DOMAIN, null)
        return if (!saved.isNullOrBlank()) {
            normalizeDomain(saved)
        } else {
            DEFAULT_DOMAIN
        }
    }

    fun setBaseDomain(context: Context, domain: String) {
        val normalized = normalizeDomain(domain.trim())
        getPrefs(context).edit().putString(KEY_BASE_DOMAIN, normalized).apply()
    }

    fun resetBaseDomain(context: Context) {
        getPrefs(context).edit().remove(KEY_BASE_DOMAIN).apply()
    }

    private fun normalizeDomain(domain: String): String {
        var clean = domain.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "https://$clean"
        }
        return clean.removeSuffix("/")
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun getColorTheme(context: Context): AppColorTheme {
        val saved = getPrefs(context).getString(KEY_COLOR_THEME, AppColorTheme.SKY_BLUE.name)
        return try {
            AppColorTheme.valueOf(saved ?: AppColorTheme.SKY_BLUE.name)
        } catch (_: Exception) {
            AppColorTheme.SKY_BLUE
        }
    }

    fun setColorTheme(context: Context, colorTheme: AppColorTheme) {
        getPrefs(context).edit().putString(KEY_COLOR_THEME, colorTheme.name).apply()
    }

    fun getDeviceLayoutMode(context: Context): DeviceLayoutMode {
        val saved = getPrefs(context).getString(KEY_DEVICE_MODE, DeviceLayoutMode.AUTO.name)
        return try {
            DeviceLayoutMode.valueOf(saved ?: DeviceLayoutMode.AUTO.name)
        } catch (_: Exception) {
            DeviceLayoutMode.AUTO
        }
    }

    fun setDeviceLayoutMode(context: Context, mode: DeviceLayoutMode) {
        getPrefs(context).edit().putString(KEY_DEVICE_MODE, mode.name).apply()
    }
}
