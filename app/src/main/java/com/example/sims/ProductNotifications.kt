package com.example.sims

data class ProductNotifications(
    val itemCode: String = "",
    val date: String = "",
    val icon: String = "",
    val details: String = "",
    var enabled: Boolean = true
)