package com.example.kotlinapp

data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val description: String = "",
    val category: String = "",
    val inStock: Boolean = true,
    val rating: Double = 0.0
)