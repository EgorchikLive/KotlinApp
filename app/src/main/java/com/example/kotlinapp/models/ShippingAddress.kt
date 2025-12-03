package com.example.kotlinapp.models

data class ShippingAddress(
    val fullName: String = "",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val city: String = "",
    val state: String = "",
    val postalCode: String = "",
    val country: String = "",
    val phoneNumber: String = ""
)