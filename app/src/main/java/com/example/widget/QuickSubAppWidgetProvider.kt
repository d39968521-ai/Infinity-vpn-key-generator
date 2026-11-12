package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import android.widget.Toast
import com.example.MainActivity
import com.example.R
import com.example.data.ProxyRepository
import com.example.data.db.AppDatabase
import com.example.util.AppPreferences
import com.example.util.ExpiryNotificationReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickSubAppWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_GENERATE_FROM_WIDGET = "com.example.widget.ACTION_GENERATE_FROM_WIDGET"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            statusText: String = "Нажмите + для создания",
            subKeyPreview: String = ""
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_sub)

            views.setTextViewText(R.id.widget_status_text, statusText)
            views.setTextViewText(R.id.widget_preview_text, subKeyPreview)

            // Intent for clicking the + button
            val generateIntent = Intent(context, QuickSubAppWidgetProvider::class.java).apply {
                action = ACTION_GENERATE_FROM_WIDGET
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val generatePendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                generateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_add_button, generatePendingIntent)

            // Intent for clicking the widget body to open app
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId + 10000,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, openAppPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context, statusText: String, subKeyPreview: String) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, QuickSubAppWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (id in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, id, statusText, subKeyPreview)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_GENERATE_FROM_WIDGET) {
            updateAllWidgets(context, "Генерация...", "Создание подписки...")

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val repo = ProxyRepository(db.proxyKeyDao())
                    val baseDomain = AppPreferences.getBaseDomain(context)
                    val result = repo.generateNewKey(baseDomain)

                    withContext(Dispatchers.Main) {
                        if (result.isSuccess) {
                            val entity = result.getOrNull()
                            if (entity != null && entity.subUrl != null) {
                                // Auto-copy to clipboard
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Sub URL", entity.subUrl)
                                clipboard.setPrimaryClip(clip)

                                ExpiryNotificationReceiver.scheduleExpiryReminder(context, entity.id, entity.expiresAt)

                                val shortSub = if (entity.subUrl!!.length > 32) "${entity.subUrl!!.take(28)}..." else entity.subUrl!!
                                updateAllWidgets(
                                    context,
                                    "✓ Скопировано в буфер!",
                                    shortSub
                                )
                                Toast.makeText(context, "Подписка создана и скопирована в буфер!", Toast.LENGTH_LONG).show()
                            } else {
                                updateAllWidgets(context, "Создано!", "")
                            }
                        } else {
                            val error = result.exceptionOrNull()?.message ?: "Ошибка"
                            updateAllWidgets(context, "Ошибка генерации", error.take(30))
                            Toast.makeText(context, "Ошибка: $error", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        updateAllWidgets(context, "Ошибка", e.message ?: "Сбой")
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
