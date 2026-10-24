package com.quicktimer.app

import android.app.Application
import com.quicktimer.app.data.TimerDatabase
import com.quicktimer.app.data.TimerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class QuickTimerApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { TimerDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { TimerRepository(database.timerDao()) }

    override fun onCreate() {
        super.onCreate()
        // Ensure initial defaults exist on first launch
        applicationScope.launch {
            if (database.timerDao().getCount() == 0) {
                database.timerDao().insertAll(
                    listOf(
                        com.quicktimer.app.data.TimerEntity(name = "5 Minutos", durationSeconds = 300, isFavorite = true, orderIndex = 0),
                        com.quicktimer.app.data.TimerEntity(name = "10 Minutos", durationSeconds = 600, isFavorite = true, orderIndex = 1),
                        com.quicktimer.app.data.TimerEntity(name = "15 Minutos", durationSeconds = 900, isFavorite = true, orderIndex = 2),
                        com.quicktimer.app.data.TimerEntity(name = "25 Minutos (Pomodoro)", durationSeconds = 1500, isFavorite = false, orderIndex = 3),
                        com.quicktimer.app.data.TimerEntity(name = "1 Minuto Express", durationSeconds = 60, isFavorite = false, orderIndex = 4)
                    )
                )
            }
        }
    }
}
