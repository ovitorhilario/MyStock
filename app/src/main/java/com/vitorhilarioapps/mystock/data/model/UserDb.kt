package com.vitorhilarioapps.mystock.data.model

import android.net.Uri
import com.google.firebase.Timestamp

data class UserDb (
    val name: String,
    val email: String,
    val photoUrl: Uri?,
    val id: Int,
    val premium: String,
    val since: Timestamp
)

