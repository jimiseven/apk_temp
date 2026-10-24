package com.quicktimer.app

import android.app.Application
import com.quicktimer.app.data.TimerCache
import com.quicktimer.app.data.TimerDatabase
import com.quicktimer.app.data.TimerEntity
import com.quicktimer.app.data.TimerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class QuickTimerApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { TimerDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { TimerRepository(database.timerDao()) }

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch {
            if (database.timerDao().getCount() == 0) {
                database.timerDao().insertAll(
                    listOf(
                        TimerEntity(name = "5 Minutos", durationSeconds = 300, isFavorite = true, orderIndex = 0),
                        TimerEntity(name = "10 Minutos", durationSeconds = 600, isFavorite = true, orderIndex = 1),
                        TimerEntity(name = "15 Minutos", durationSeconds = 900, isFavorite = true, orderIndex = 2),
                        TimerEntity(name = "25 Minutos (Pomodoro)", durationSeconds = 1500, isFavorite = false, orderIndex = 3),
                        TimerEntity(name = "1 Minuto Express", durationSeconds = 60, isFavorite = false, orderIndex = 4)
                    )
                )
            }

            // Keep in-memory cache synchronized with Room for instantaneous access
            repository.favoriteTimers.collectLatest { favorites ->
                if (favorites.isNotEmpty()) {
                    TimerCache.favorites = favorites.take(3)
                }
            }
        }
    }
}
