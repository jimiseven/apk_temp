package com.quicktimer.app.data

object TimerCache {
    @Volatile
    var favorites: List<TimerEntity> = listOf(
        TimerEntity(id = 1, name = "5 Minutos", durationSeconds = 300, isFavorite = true, orderIndex = 0),
        TimerEntity(id = 2, name = "10 Minutos", durationSeconds = 600, isFavorite = true, orderIndex = 1),
        TimerEntity(id = 3, name = "15 Minutos", durationSeconds = 900, isFavorite = true, orderIndex = 2)
    )
}
