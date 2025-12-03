package com.example.kotlinapp

data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val description: String = "",  // Добавляем поле description
    val category: String = "",     // Добавляем поле category
    val inStock: Boolean = true,
    val rating: Double = 0.0
)