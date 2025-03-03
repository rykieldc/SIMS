package com.example.sims

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class LocalUser(
    val name: String,
    @PrimaryKey val username: String,
    val role: String,
    val enabled: Boolean = true,  // Provide a default value if needed
    val password: String = ""     // Provide a default value if needed
)

