package com.example.sims

data class UserLogs(
    val date: String = "",
    val name: String = "",
    val action: String = "",
    val itemCode: String? = null,
    val itemName: String? = null,
    val itemCategory: String? = null,
    val itemWeight: Float? = null,
    val location: String? = null,
    val supplier: String? = null,
    val stocksLeft: Int? = null,
    val dateAdded: String? = null,
    val lastRestocked: String? = null,
    val enabled: Boolean? = null,
    val imageUrl: String? = null,
    val itemDetails: String? = null,
    val userName: String? = null,
    val userUsername: String? = null,
    val userRole: String? = null
)
