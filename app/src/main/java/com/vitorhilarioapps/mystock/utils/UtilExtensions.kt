package com.vitorhilarioapps.mystock.utils

import android.app.Activity
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.tasks.await

/*-----------------------------
|    Coroutines & Firebase    |
-----------------------------*/

suspend fun runVoidTask(task: Task<Void>?): Boolean {
    return if (task != null) {
        try {
            task.await()
            task.isSuccessful
        } catch (e: Exception) {
            false
        }
    } else false
}

suspend fun runAuthTask(task: Task<AuthResult>?): Task<AuthResult>? {
    return task?.let {
        try {
            it.await()
            it
        } catch (e: Exception) {
            null
        }
    }
}

/*-------------------------
|    Extensions for UI    |
-------------------------*/

fun Double.moneyType() : String {
    return buildString {
        if (this@moneyType < 0.0) {
            append("-")
            append("R$");
            append(String.format("%.2f", -this@moneyType))
        } else {
            append("R$");
            append(String.format("%.2f",  this@moneyType))
        }
    }
}

fun Activity.showInfoToast(info: String) {
    Toast.makeText(this, info, Toast.LENGTH_SHORT).show()
}

fun Activity.showSuccessToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Activity.showErrorToast(error: String) {
    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
}