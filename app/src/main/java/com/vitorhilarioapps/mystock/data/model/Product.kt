package com.vitorhilarioapps.mystock.data.model

import com.google.firebase.Timestamp

data class Product(
    val name: String,
    val code: Int,
    val amount: Int,
    val salePrice: Double,
    val purchasePrice: Double,
    val registrationDate : Timestamp,
    val category: String,
    val description: String,
    val weight: Double,
    val weightType: String,
)
