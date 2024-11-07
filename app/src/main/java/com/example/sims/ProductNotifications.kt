package com.example.sims

data class ProductNotifications(
    val icon: Int,
    val text: String,
    val stockLevel: String,
    var isRead: Boolean = false
)
