package com.example.sims

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class LocalHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val name: String,
    val action: String,
    val itemCode: String?,
    val itemName: String?,
    val itemCategory: String?,
    val itemWeight: Float?,
    val rackNo: Int?,
    val location: String?,
    val supplier: String?,
    val stocksLeft: Int?,
    val dateAdded: String?,
    val lastRestocked: String?,
    val enabled: Boolean?,
    val imageUrl: String?,
    val itemDetails: String?,
    val userName: String?,
    val userUsername: String?,
    val userRole: String?
)
