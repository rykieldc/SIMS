package com.example.sims

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "items")
data class LocalItem(
    @PrimaryKey val itemCode: String,
    val itemName: String,
    val itemCategory: String,
    val itemWeight: Float,
    val rackNo: Int,
    val location: String,
    val supplier: String,
    val stocksLeft: Int,
    val dateAdded: String,
    val lastRestocked: String,
    val enabled: Boolean,
    val imageUrl: String
) : Parcelable
