package com.example.sims

import androidx.room.*

@Dao
interface ItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: LocalItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<LocalItem>)

    @Query("SELECT * FROM items")
    suspend fun getAllItems(): List<LocalItem>

    @Query("DELETE FROM items")
    suspend fun clearItems()
}
