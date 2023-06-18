package com.vitorhilarioapps.mystock.ui.auth.viewmodel

import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.vitorhilarioapps.mystock.data.repository.FirebaseRepository
import com.vitorhilarioapps.mystock.utils.runAuthTask
import com.vitorhilarioapps.mystock.utils.runVoidTask
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val firebaseRepository: FirebaseRepository = FirebaseRepository()

    fun getUser() = firebaseRepository.getUser()

    suspend fun createUser(email: String, password: String): Task<AuthResult>? =
        runAuthTask(firebaseRepository.createUser(email, password))

    suspend fun createDbForUser(user: FirebaseUser) =
        runVoidTask(firebaseRepository.createDbForUser(user))

    suspend fun signIn(email: String, password: String): Task<AuthResult>? =
        runAuthTask(firebaseRepository.signIn(email, password))

    suspend fun signInWithCredential(idToken: String): Task<AuthResult>? =
        runAuthTask(firebaseRepository.signInWithCredential(idToken))

    suspend fun sendPasswordResetEmail(email: String): Boolean =
        runVoidTask(firebaseRepository.sendPasswordResetEmail(email))

    suspend fun sendEmailVerification(): Boolean =
        runVoidTask(firebaseRepository.sendEmailVerification())

    fun isAuthenticated(): Boolean = firebaseRepository.isAuthenticated()

    suspend fun hasUserInDb(userId: String): Boolean {
        val task = firebaseRepository.hasUserInDb()
        return task?.let {
            try {
                val docs = task.await().documents
                docs.any { it.id == userId }
            } catch (e: Exception) {
                false
            }
        } ?: false
    }
}