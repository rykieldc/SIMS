package com.example.sims

import androidx.room.*

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: LocalHistory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(historyList: List<LocalHistory>)

    @Query("SELECT * FROM history ORDER BY date DESC")
    suspend fun getAllHistory(): List<LocalHistory>

    @Query("DELETE FROM history")
    suspend fun clearHistory()
}
