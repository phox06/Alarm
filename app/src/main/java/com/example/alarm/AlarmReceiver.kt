package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val soundUri = intent.getStringExtra("SOUND_URI")

        val serviceIntent = Intent(context, RingService::class.java).apply {
            putExtra("SOUND_URI", soundUri)
        }

        // 2. Vibrate
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        // Vibrate pattern: Wait 0ms, vibrate 1000ms, sleep 1000ms
        vibrator.vibrate(longArrayOf(0, 1000, 1000), 0)

        // 3. Show Notification
        // In a real app, you would use a FullScreenIntent here to show an Activity to dismiss the alarm
        val builder = NotificationCompat.Builder(context, "ALARM_CHANNEL_ID")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Wake Up!")
            .setContentText("Your alarm is ringing.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(123, builder.build())
        } catch (e: SecurityException) {
            // Handle missing notification permission
        }

        // Note: You need a way to stop the MediaPlayer and Vibrator.
        // Typically, tapping the notification opens an Activity where you call .stop()
    }
}