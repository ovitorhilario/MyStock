package com.vitorhilarioapps.mystock.ui.home.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.vitorhilarioapps.mystock.data.repository.FirebaseRepository
import com.vitorhilarioapps.mystock.data.model.Product
import com.vitorhilarioapps.mystock.data.model.UserDb
import com.vitorhilarioapps.mystock.utils.toEntry
import com.vitorhilarioapps.mystock.utils.toProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

import com.vitorhilarioapps.mystock.data.model.Transaction.Entry
import com.vitorhilarioapps.mystock.data.model.Transaction.Exit
import com.vitorhilarioapps.mystock.utils.runVoidTask
import com.vitorhilarioapps.mystock.utils.toExit
import com.vitorhilarioapps.mystock.utils.toUserDb

class FirestoreViewModel : ViewModel() {

    private var firebaseRepository: FirebaseRepository = FirebaseRepository()

    private val _products = MutableLiveData<List<Product>>()
    val products get() = _products as LiveData<List<Product>>

    private val _userDb = MutableLiveData<UserDb>()
    val userDb = _userDb as LiveData<UserDb>

    fun getUser(): FirebaseUser? = firebaseRepository.getUser()

    fun getAuth(): FirebaseAuth = firebaseRepository.getAuth()

    fun getUserFromDb() {
        viewModelScope.launch(Dispatchers.IO) {
            val userResponse = firebaseRepository.getUserFromDb()?.await()

            withContext(Dispatchers.Main) {
                userResponse?.data?.takeIf { it.isNotEmpty() }?.let { userMap ->
                    _userDb.value = userMap.toUserDb(getUser()?.email ?: "Anonymous", getUser()?.photoUrl)
                }
            }
        }
    }

    suspend fun getProducts(): List<Product>? {
        var response : List<Product>? = null
        val productResponse = getResponse(firebaseRepository.getProducts())

        if (productResponse is Response.Success) {
            response = productResponse.products()
        }

        return response
    }

    suspend fun getProducts(codes: List<Int>): List<Product>? {
        var response : List<Product>? = null
        val productResponse = getResponse(firebaseRepository.getProducts(codes))

        if (productResponse is Response.Success) {
            response = productResponse.products()
        }

        return response
    }

    suspend fun getExits(): List<Exit>? {
        var response : List<Exit>? = null
        val exitsResponse = getResponse(firebaseRepository.getExits())

        if (exitsResponse is Response.Success) {
            response = exitsResponse.exits()
        }

        return response
    }

    suspend fun getEntrys(): List<Entry>? {
        var response : List<Entry>? = null
        val exitsResponse = getResponse(firebaseRepository.getEntrys())

        if (exitsResponse is Response.Success) {
            response = exitsResponse.entrys()
        }

        return response
    }

    suspend fun delProducts(codes: List<Int>): Boolean =
        runVoidTask(firebaseRepository.delProducts(codes))

    suspend fun delEntrys(ids: List<Int>): Boolean =
        runVoidTask(firebaseRepository.delEntrys(ids))

    suspend fun delExits(ids: List<Int>): Boolean =
        runVoidTask(firebaseRepository.deleteExits(ids))

    suspend fun addProduct(
        code: Int, product: HashMap<String, Comparable<*>>
    ): Boolean = runVoidTask(firebaseRepository.addProduct(code, product))

    suspend fun addExitTransaction(
        products: List<Pair<Int, Int>>,
        selectedProducts: List<Product>
    ): Boolean = runVoidTask(firebaseRepository.addExitTransaction(products, selectedProducts))

    suspend fun addEntryTransaction(
        products: List<Pair<Int, Int>>,
        selectedProducts: List<Product>
    ): Boolean = runVoidTask(firebaseRepository.addEntryTransaction(products, selectedProducts))

    suspend fun hasProduct(code: Int): Boolean {
        val response = getResponse(firebaseRepository.hasProduct(code))

        return when (response) {
            is Response.Success -> true
            is Response.Failure -> false
            else -> false
        }
    }

    suspend fun getResponse(task: Task<QuerySnapshot>?): Response {
        var finalResponse : Response = Response.Loading

        val taskJob = viewModelScope.launch (Dispatchers.IO) {
            if (task != null) {
                try {
                    val response = task.await().documents

                    withContext(Dispatchers.Main) {
                        finalResponse = when (response.isEmpty()) {
                            true -> Response.Failure
                            false -> Response.Success(response)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        finalResponse = Response.Failure
                    }
                }
            } else {
                finalResponse = Response.Failure
            }
        }

        taskJob.join()

        return finalResponse
    }

    sealed class Response {
        data class Success(val data: List<DocumentSnapshot>) : Response()
        object Failure : Response()
        object Loading : Response()
    }

    private fun Response.products() : List<Product> {
        return if (this is Response.Success) {
            this.data.mapNotNull { it.data?.toProduct() }
        } else emptyList()
    }

    private fun Response.entrys() : List<Entry> {
        return if (this is Response.Success) {
            this.data.mapNotNull { it.data?.toEntry() }
        } else emptyList()
    }

    private fun Response.exits() : List<Exit> {
        return if (this is Response.Success) {
            this.data.mapNotNull { it.data?.toExit() }
        } else emptyList()
    }
}