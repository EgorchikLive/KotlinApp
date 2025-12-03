package com.example.kotlinapp.models

import java.util.Date

data class FavoriteItem(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val productPrice: Double = 0.0,
    val productImageUrl: String = "",
    val addedAt: Date = Date()
)
