package com.vitorhilarioapps.mystock.ui.home.view.transactions.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class TransactionType() : Parcelable {
    @Parcelize
    object Sale : TransactionType()
    @Parcelize
    object Purchase : TransactionType()
}