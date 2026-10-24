package com.quicktimer.app.data

import kotlinx.coroutines.flow.Flow

class TimerRepository(private val timerDao: TimerDao) {

    val allTimers: Flow<List<TimerEntity>> = timerDao.getAllTimersFlow()
    val favoriteTimers: Flow<List<TimerEntity>> = timerDao.getFavoriteTimersFlow()

    suspend fun getFavoriteTimersSync(): List<TimerEntity> {
        val favorites = timerDao.getFavoriteTimersSync()
        return if (favorites.isNotEmpty()) {
            favorites
        } else {
            // Fallback default list if database was not yet populated
            listOf(
                TimerEntity(name = "5 Minutos", durationSeconds = 300, isFavorite = true, orderIndex = 0),
                TimerEntity(name = "10 Minutos", durationSeconds = 600, isFavorite = true, orderIndex = 1),
                TimerEntity(name = "15 Minutos", durationSeconds = 900, isFavorite = true, orderIndex = 2)
            )
        }
    }

    suspend fun insert(timer: TimerEntity): Long {
        return timerDao.insert(timer)
    }

    suspend fun update(timer: TimerEntity) {
        timerDao.update(timer)
    }

    suspend fun delete(timer: TimerEntity) {
        timerDao.delete(timer)
    }

    /**
     * Toggles the favorite status.
     * Allows a maximum of 3 favorites.
     * Returns true if changed successfully, false if already 3 favorites exist and user tried to add another.
     */
    suspend fun toggleFavorite(timer: TimerEntity): Boolean {
        if (!timer.isFavorite) {
            val count = timerDao.getFavoriteCount()
            if (count >= 3) {
                return false
            }
        }
        timerDao.update(timer.copy(isFavorite = !timer.isFavorite))
        return true
    }
}
