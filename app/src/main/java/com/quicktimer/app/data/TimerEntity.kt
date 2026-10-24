package com.quicktimer.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "timers")
data class TimerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val durationSeconds: Int,
    val isFavorite: Boolean = false,
    val orderIndex: Int = 0
) {
    val minutes: Int
        get() = durationSeconds / 60

    val seconds: Int
        get() = durationSeconds % 60

    val formattedDuration: String
        get() = if (minutes >= 60) {
            val hours = minutes / 60
            val remMinutes = minutes % 60
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, remMinutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
}
