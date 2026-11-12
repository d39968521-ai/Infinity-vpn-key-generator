package com.example.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.text.SimpleDateFormat
import java.util.Locale

class ExpiryNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!AppPreferences.areNotificationsEnabled(context)) return

        val keyId = intent.getStringExtra("KEY_ID") ?: return
        val timeLeftText = intent.getStringExtra("TIME_LEFT_TEXT") ?: "30 минут"

        showNotification(context, keyId, timeLeftText)
    }

    private fun showNotification(context: Context, keyId: String, timeLeftText: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "key_expiry_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Истечение срока подписки",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о скором окончании срока действия подписки"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Open app when clicked
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_KEY_ID", keyId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            keyId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Generate new subscription directly
        val generateIntent = Intent(context, ExpiryActionReceiver::class.java).apply {
            action = "ACTION_GENERATE_NEW_KEY"
        }
        val generatePendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            generateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Подписка скоро закончится!")
            .setContentText("Срок действия подписки истекает примерно через $timeLeftText. Нажмите, чтобы создать новую.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(R.mipmap.ic_launcher, "Создать новую в 1 клик", generatePendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        notificationManager.notify(keyId.hashCode(), notification)
    }

    companion object {
        fun scheduleExpiryReminder(context: Context, keyId: String, expiresAt: String?) {
            if (expiresAt == null || !AppPreferences.areNotificationsEnabled(context)) return
            try {
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val expireDate = format.parse(expiresAt) ?: return
                
                // Reminder 30 minutes before expiration
                val triggerAtMillis = expireDate.time - (30 * 60 * 1000)
                val now = System.currentTimeMillis()

                if (triggerAtMillis > now) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val intent = Intent(context, ExpiryNotificationReceiver::class.java).apply {
                        putExtra("KEY_ID", keyId)
                        putExtra("TIME_LEFT_TEXT", "30 минут")
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        keyId.hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    }
                }
            } catch (_: Exception) {}
        }

        fun cancelExpiryReminder(context: Context, keyId: String) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, ExpiryNotificationReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    keyId.hashCode(),
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                }
            } catch (_: Exception) {}
        }
    }
}
