package com.example.sims

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: LocalUser)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<LocalUser>)

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<LocalUser>

    // ✅ Add this method to retrieve a user by username
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): LocalUser?
}
