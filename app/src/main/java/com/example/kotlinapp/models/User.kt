package com.example.kotlinapp

import java.util.Date

data class User(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val photoUrl: String = "",
    val provider: String = "email", // "email" или "google"
    val createdAt: Date = Date(),
    val lastLogin: Date = Date(),
    val emailVerified: Boolean = false
)