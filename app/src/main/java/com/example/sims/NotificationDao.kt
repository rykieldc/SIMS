package com.example.sims

import androidx.room.*

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: LocalNotification)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<LocalNotification>)

    @Query("SELECT * FROM notifications ORDER BY date DESC")
    suspend fun getAllNotifications(): List<LocalNotification>

    @Query("DELETE FROM notifications")
    suspend fun clearNotifications()
}

