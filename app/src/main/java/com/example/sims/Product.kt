package com.example.sims

data class Product(
    val itemCode: String,
    val itemName: String,
    val itemCategory: String,
    val location: String,
    val supplier: String,
    val stocksLeft: String,
    val dateAdded: String,
    val lastRestocked: String,
    val imageUrl: String
)
