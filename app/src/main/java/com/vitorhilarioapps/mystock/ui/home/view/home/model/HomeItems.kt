package com.vitorhilarioapps.mystock.ui.home.view.home.model

import android.net.Uri
import com.vitorhilarioapps.mystock.data.model.Transaction

sealed class HomeItems {
    data class ToolBar(
        val name: String?,
        val imageUri: Uri?
    ) : HomeItems()

    data class Info(
        val profitOnSales: Double = 0.0,
        val transactionBalance: Double = 0.0
    ) : HomeItems()

    data class RecentlyTransactions(
        val transactions: List<Transaction>
    ) : HomeItems()

    data class ShortCut(
        val stockValue: Double = 0.0,
        val entryValue: Double = 0.0,
        val exitValue: Double = 0.0,
        val amountProducts: Int = 0
    ) : HomeItems()

    data class MainActions(
        val tittle: String
    ) : HomeItems()
}