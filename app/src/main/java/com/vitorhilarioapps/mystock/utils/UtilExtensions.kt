package com.vitorhilarioapps.mystock.utils

import android.app.Activity
import androidx.core.content.res.ResourcesCompat
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.vitorhilarioapps.mystock.R
import kotlinx.coroutines.tasks.await
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle

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

fun Activity.showInfoToast(tittle: String = "Info", info: String, duration: Long = MotionToast.LONG_DURATION) {
    MotionToast.createToast(this,
        tittle,
        info,
        MotionToastStyle.INFO,
        MotionToast.GRAVITY_BOTTOM,
        duration,
        ResourcesCompat.getFont(this, R.font.dmsans))
}

fun Activity.showSuccessToast(tittle: String = "Success", message: String, duration: Long = MotionToast.LONG_DURATION) {
    MotionToast.createToast(this,
        tittle,
        message,
        MotionToastStyle.SUCCESS,
        MotionToast.GRAVITY_BOTTOM,
        duration,
        ResourcesCompat.getFont(this, R.font.dmsans))
}

fun Activity.showErrorToast(tittle: String = "Error", error: String, duration: Long = MotionToast.LONG_DURATION) {
    MotionToast.createToast(this,
        tittle,
        error,
        MotionToastStyle.ERROR,
        MotionToast.GRAVITY_BOTTOM,
        duration,
        ResourcesCompat.getFont(this, R.font.dmsans))
}