package com.quicktimer.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.quicktimer.app.ui.MainActivity
import com.quicktimer.app.util.SoundHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class TimerForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var countdownJob: Job? = null
    private var remainingSeconds: Int = 0
    private var initialSeconds: Int = 0
    private var timerName: String = "Temporizador"

    private lateinit var notificationManager: NotificationManager

    companion object {
        const val CHANNEL_ID = "quick_timer_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.quicktimer.app.START_TIMER"
        const val ACTION_STOP = "com.quicktimer.app.STOP_TIMER"

        const val EXTRA_SECONDS = "extra_seconds"
        const val EXTRA_NAME = "extra_name"

        var isRunning: Boolean = false
            private set

        fun startTimer(context: Context, seconds: Int, name: String) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SECONDS, seconds)
                putExtra(EXTRA_NAME, name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopTimer(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val seconds = intent.getIntExtra(EXTRA_SECONDS, 60)
                val name = intent.getStringExtra(EXTRA_NAME) ?: "Temporizador"
                startCountdown(seconds, name)
            }
            ACTION_STOP -> {
                stopCountdown()
            }
        }
        return START_NOT_STICKY
    }

    private fun startCountdown(seconds: Int, name: String) {
        // Cancel existing job if running
        countdownJob?.cancel()

        initialSeconds = seconds
        remainingSeconds = seconds
        timerName = name
        isRunning = true

        // Play start sound
        SoundHelper.playStartSound(this)

        val notification = buildOngoingNotification(remainingSeconds, timerName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        countdownJob = serviceScope.launch {
            while (isActive && remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
                notificationManager.notify(NOTIFICATION_ID, buildOngoingNotification(remainingSeconds, timerName))
            }

            if (remainingSeconds <= 0 && isActive) {
                isRunning = false
                SoundHelper.playEndSound(this@TimerForegroundService)
                showFinishedNotification(timerName)
                stopSelf()
            }
        }
    }

    private fun stopCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        isRunning = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun buildOngoingNotification(secondsLeft: Int, name: String): Notification {
        val stopIntent = Intent(this, TimerForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            1,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val minutes = secondsLeft / 60
        val seconds = secondsLeft % 60
        val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(name)
            .setContentText("Restante: $timeFormatted")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(mainPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_delete, "Detener", stopPendingIntent)
            .setColor(0xFFFF7A00.toInt())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }

    private fun showFinishedNotification(name: String) {
        val finishedNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("¡Tiempo terminado!")
            .setContentText("El temporizador '$name' ha finalizado")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setAutoCancel(true)
            .setColor(0xFFFF7A00.toInt())
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, finishedNotification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Temporizadores Activos",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificaciones de la cuenta regresiva del temporizador"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        countdownJob?.cancel()
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
