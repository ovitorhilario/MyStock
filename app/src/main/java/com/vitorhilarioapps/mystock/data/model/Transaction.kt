package com.vitorhilarioapps.mystock.data.model

import com.google.firebase.Timestamp

sealed class Transaction {
    data class Exit(
        val id: Int,
        val date: Timestamp,
        val info: ArrayList<String>,
        val resultPrice: Double
    ) : Transaction()
    data class Entry(
        val id: Int,
        val date: Timestamp,
        val info: ArrayList<String>,
        val resultPrice: Double,
        val profitOnSale: Double
    ) : Transaction()
}

data class TransactionItem(
    val data: Transaction,
    var isSelected: Boolean = false,
)