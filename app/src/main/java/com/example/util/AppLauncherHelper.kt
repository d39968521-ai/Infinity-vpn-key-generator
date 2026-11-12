package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object AppLauncherHelper {
    // Known packages for Happ / Hiddify proxy clients
    private val HAPP_PACKAGE_CANDIDATES = listOf(
        "com.happproxy",
        "io.nekohasekai.sagernet",
        "io.github.hiddify.app",
        "com.v2ray.ang",
        "com.github.kr328.clash"
    )

    /**
     * Copies subUrl to clipboard, then attempts to launch the proxy app (happ/hiddify/etc)
     * either via URI scheme or directly by package name.
     */
    fun copyAndLaunchHapp(context: Context, subUrl: String, onCopied: () -> Unit) {
        onCopied()

        val packageManager = context.packageManager
        
        // 1. First, check if any target package is installed
        var targetPackage: String? = null
        for (pkg in HAPP_PACKAGE_CANDIDATES) {
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                targetPackage = pkg
                break
            }
        }

        // 2. If target package is found, launch it via its package launch intent
        if (targetPackage != null) {
            val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launchIntent != null) {
                try {
                    context.startActivity(launchIntent)
                    Toast.makeText(context, "Sub URL скопирован! Запуск Happ...", Toast.LENGTH_SHORT).show()
                    return
                } catch (_: Exception) { }
            }
        }

        // 3. Fallback: Try launching via deep link intent (hiddify:// or http)
        try {
            val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse("hiddify://install-config?url=$subUrl")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(deepLinkIntent)
            Toast.makeText(context, "Sub URL скопирован! Открытие в приложении...", Toast.LENGTH_SHORT).show()
            return
        } catch (_: Exception) { }

        // 4. If nothing was found to launch, let the user know it's safely copied
        Toast.makeText(context, "Sub URL скопирован в буфер обмена", Toast.LENGTH_SHORT).show()
    }
}
