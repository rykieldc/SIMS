package com.example.sims

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class LocalNotification(
    @PrimaryKey val itemCode: String,
    val date: String,
    val icon: String,
    val details: String,
    val enabled: Boolean
)

fun LocalNotification.toNotification(): Notification {
    return Notification(
        itemCode = this.itemCode,
        date = this.date,
        icon = this.icon,
        details = this.details,
        enabled = this.enabled
    )
}

fun Notification.toLocalNotification(): LocalNotification {
    return LocalNotification(
        itemCode = this.itemCode,
        date = this.date,
        icon = this.icon,
        details = this.details,
        enabled = this.enabled
    )
}