package com.example.kotlinapp

import java.util.Date

data class CartItem(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val productPrice: Double = 0.0,
    val productImageUrl: String = "",
    val quantity: Int = 1,
    val addedAt: Date = Date()
)