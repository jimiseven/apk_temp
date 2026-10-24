package com.quicktimer.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object SoundHelper {

    private fun getToneUri(): Uri {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    }

    fun playStartSound(context: Context) {
        try {
            val toneUri = getToneUri()
            val ringtone: Ringtone? = RingtoneManager.getRingtone(context.applicationContext, toneUri)
            ringtone?.let {
                it.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                it.play()
            }
            vibrateShort(context, 80)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Plays the EXACT same tone as the start sound for 3 seconds continuously.
     */
    fun playEndSound(context: Context) {
        val appContext = context.applicationContext
        try {
            val toneUri = getToneUri()
            val player = MediaPlayer().apply {
                setDataSource(appContext, toneUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }

            // Stop playback after exactly 3000ms (3 seconds)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    if (player.isPlaying) {
                        player.stop()
                    }
                    player.release()
                } catch (ignored: Exception) {}
            }, 3000)

            vibrateShort(appContext, 500)
        } catch (e: Exception) {
            // Fallback to Ringtone for 3 seconds
            try {
                val toneUri = getToneUri()
                val ringtone = RingtoneManager.getRingtone(appContext, toneUri)
                ringtone?.play()
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        if (ringtone?.isPlaying == true) {
                            ringtone.stop()
                        }
                    } catch (ignored: Exception) {}
                }, 3000)
            } catch (ignored: Exception) {}
        }
    }

    private fun vibrateShort(context: Context, milliseconds: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(milliseconds)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
