package com.example.util

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

enum class DeviceLayoutMode(val title: String) {
    AUTO("Авто-определение (ТВ или Смартфон)"),
    TV("Режим Smart TV (Большой экран, Д-пад)"),
    MOBILE("Режим Смартфона (Сенсорный экран)")
}

object DeviceTypeHelper {

    /**
     * Checks if the device is a Smart TV / Android TV / Set-top box.
     */
    fun isTelevision(context: Context): Boolean {
        // 1. UiModeManager check (Standard Android recommendation)
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            return true
        }

        // 2. PackageManager feature checks
        val pm = context.packageManager
        if (pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
            return true
        }
        if (pm.hasSystemFeature("android.hardware.type.television")) {
            return true
        }

        return false
    }

    /**
     * Determines whether TV UI should be rendered given user preference and hardware.
     */
    fun shouldShowTvLayout(context: Context, mode: DeviceLayoutMode): Boolean {
        return when (mode) {
            DeviceLayoutMode.TV -> true
            DeviceLayoutMode.MOBILE -> false
            DeviceLayoutMode.AUTO -> isTelevision(context)
        }
    }
}
