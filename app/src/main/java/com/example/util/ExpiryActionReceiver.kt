package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.ProxyRepository
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExpiryActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "ACTION_GENERATE_NEW_KEY") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val repo = ProxyRepository(db.proxyKeyDao())
                    val currentDomain = AppPreferences.getBaseDomain(context)
                    val result = repo.generateNewKey(currentDomain)

                    withContext(Dispatchers.Main) {
                        if (result.isSuccess) {
                            val newKey = result.getOrNull()
                            if (newKey != null && newKey.subUrl != null) {
                                // Auto copy to clipboard
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Sub URL", newKey.subUrl)
                                clipboard.setPrimaryClip(clip)

                                ExpiryNotificationReceiver.scheduleExpiryReminder(context, newKey.id, newKey.expiresAt)
                                Toast.makeText(context, "Новая подписка создана и скопирована в буфер!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Подписка создана!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Ошибка создания: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
