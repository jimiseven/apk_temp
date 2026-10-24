package com.quicktimer.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [TimerEntity::class], version = 1, exportSchema = false)
abstract class TimerDatabase : RoomDatabase() {

    abstract fun timerDao(): TimerDao

    companion object {
        @Volatile
        private var INSTANCE: TimerDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): TimerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TimerDatabase::class.java,
                    "quick_timer_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        populateInitialData(database.timerDao())
                    }
                }
            }

            suspend fun populateInitialData(dao: TimerDao) {
                if (dao.getCount() == 0) {
                    val defaultTimers = listOf(
                        TimerEntity(
                            name = "5 Minutos",
                            durationSeconds = 5 * 60,
                            isFavorite = true,
                            orderIndex = 0
                        ),
                        TimerEntity(
                            name = "10 Minutos",
                            durationSeconds = 10 * 60,
                            isFavorite = true,
                            orderIndex = 1
                        ),
                        TimerEntity(
                            name = "15 Minutos",
                            durationSeconds = 15 * 60,
                            isFavorite = true,
                            orderIndex = 2
                        ),
                        TimerEntity(
                            name = "25 Minutos (Pomodoro)",
                            durationSeconds = 25 * 60,
                            isFavorite = false,
                            orderIndex = 3
                        ),
                        TimerEntity(
                            name = "1 Minuto Express",
                            durationSeconds = 60,
                            isFavorite = false,
                            orderIndex = 4
                        )
                    )
                    dao.insertAll(defaultTimers)
                }
            }
        }
    }
}
