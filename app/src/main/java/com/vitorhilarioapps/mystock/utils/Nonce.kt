package com.vitorhilarioapps.mystock.utils

import android.util.Base64
import java.security.SecureRandom

fun generateNonce(length: Int = 32): String {
    val random = SecureRandom()
    val bytes = ByteArray(length)
    random.nextBytes(bytes)
    return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
}
