package com.quicktimer.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerDao {

    @Query("SELECT * FROM timers ORDER BY isFavorite DESC, orderIndex ASC, id ASC")
    fun getAllTimersFlow(): Flow<List<TimerEntity>>

    @Query("SELECT * FROM timers WHERE isFavorite = 1 ORDER BY orderIndex ASC, id ASC LIMIT 3")
    fun getFavoriteTimersFlow(): Flow<List<TimerEntity>>

    @Query("SELECT * FROM timers WHERE isFavorite = 1 ORDER BY orderIndex ASC, id ASC LIMIT 3")
    suspend fun getFavoriteTimersSync(): List<TimerEntity>

    @Query("SELECT * FROM timers WHERE id = :id LIMIT 1")
    suspend fun getTimerById(id: Long): TimerEntity?

    @Query("SELECT COUNT(*) FROM timers WHERE isFavorite = 1")
    suspend fun getFavoriteCount(): Int

    @Query("SELECT COUNT(*) FROM timers")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(timer: TimerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(timers: List<TimerEntity>)

    @Update
    suspend fun update(timer: TimerEntity)

    @Delete
    suspend fun delete(timer: TimerEntity)
}
