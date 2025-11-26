package com.example.kotlinapp

import java.util.Date

data class User(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val photoUrl: String = "",
    val provider: String = "email",
    val role: String = "user", // "user" или "admin"
    val createdAt: Date = Date(),
    val lastLogin: Date = Date(),
    val emailVerified: Boolean = false
)