package com.quicktimer.app.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.quicktimer.app.ui.QuickTimerFloatingActivity

class VolumeAccessibilityService : AccessibilityService() {

    private val targetSequence = intArrayOf(
        KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_VOLUME_DOWN
    )

    private var currentStep = 0
    private var lastKeyTime = 0L
    private val timeoutWindowMs = 2000L

    private var partialWakeLock: PowerManager.WakeLock? = null
    private var screenWakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val TAG = "VolumeAccessService"
        var isServiceRunning = false
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        Log.d(TAG, "VolumeAccessibilityService conectado exitosamente")

        // Keep CPU awake for listening to hardware buttons even when display dims
        acquirePartialWakeLock()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used, we only filter key events
    }

    override fun onInterrupt() {
        Log.d(TAG, "VolumeAccessibilityService interrumpido")
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false

        // Only process key down events to avoid double counting
        if (event.action != KeyEvent.ACTION_DOWN) {
            return false
        }

        val keyCode = event.keyCode
        if (keyCode != KeyEvent.KEYCODE_VOLUME_DOWN && keyCode != KeyEvent.KEYCODE_VOLUME_UP) {
            return false
        }

        val currentTime = SystemClock.uptimeMillis()

        // Reset if exceeded buffer time
        if (currentTime - lastKeyTime > timeoutWindowMs) {
            currentStep = 0
        }
        lastKeyTime = currentTime

        if (keyCode == targetSequence[currentStep]) {
            currentStep++
            Log.d(TAG, "Paso de secuencia detectado: $currentStep/${targetSequence.size}")

            if (currentStep == targetSequence.size) {
                // Complete sequence detected!
                currentStep = 0
                Log.d(TAG, "¡Secuencia completa detectada! Despertando pantalla y lanzando temporizador...")
                triggerQuickTimer()
                return true // Consume key event
            }
            return true // Consume key event while matching to prevent volume slider popup
        } else {
            // Check if key pressed matches the first step to restart sequence immediately
            currentStep = if (keyCode == targetSequence[0]) 1 else 0
        }

        return false
    }

    private fun triggerQuickTimer() {
        wakeUpScreen()

        val intent = Intent(this, QuickTimerFloatingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
        }
        startActivity(intent)
    }

    @SuppressLint("WakelockTimeout")
    private fun wakeUpScreen() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            screenWakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "quicktimer:screentrigger"
            )
            screenWakeLock?.acquire(3000L) // Release automatically after 3 seconds
        } catch (e: Exception) {
            Log.e(TAG, "Error adquiriendo WakeLock para encender pantalla: ${e.message}")
        }
    }

    @SuppressLint("WakelockTimeout")
    private fun acquirePartialWakeLock() {
        try {
            if (partialWakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                partialWakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "quicktimer:partialwakelock"
                )
                partialWakeLock?.acquire()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adquiriendo PartialWakeLock: ${e.message}")
        }
    }

    override fun onDestroy() {
        isServiceRunning = false
        try {
            if (partialWakeLock?.isHeld == true) {
                partialWakeLock?.release()
            }
            if (screenWakeLock?.isHeld == true) {
                screenWakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error liberando WakeLocks: ${e.message}")
        }
        super.onDestroy()
    }
}
