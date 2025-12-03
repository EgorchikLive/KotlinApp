package com.example.kotlinapp.models

import com.example.kotlinapp.CartItem
import java.util.Date

data class Order(
    val id: String = "",
    val userId: String = "",
    val items: List<CartItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val status: String = "pending", // pending, processing, shipped, delivered, cancelled
    val shippingAddress: ShippingAddress? = null,
    val paymentMethod: String = "",
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)