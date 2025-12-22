package com.vitorhilarioapps.mystock.ui.home.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.vitorhilarioapps.mystock.data.model.Product
import com.vitorhilarioapps.mystock.data.model.Transaction.Entry
import com.vitorhilarioapps.mystock.data.model.Transaction.Exit
import com.vitorhilarioapps.mystock.data.model.UserDb
import com.vitorhilarioapps.mystock.data.repository.FirebaseRepository
import com.vitorhilarioapps.mystock.utils.runVoidTask
import com.vitorhilarioapps.mystock.utils.toEntry
import com.vitorhilarioapps.mystock.utils.toExit
import com.vitorhilarioapps.mystock.utils.toProduct
import com.vitorhilarioapps.mystock.utils.toUserDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreViewModel : ViewModel() {

    private var firebaseRepository: FirebaseRepository = FirebaseRepository()

    private val _products = MutableLiveData<List<Product>>()
    val products get() = _products as LiveData<List<Product>>

    private val _userDb = MutableLiveData<UserDb>()
    val userDb = _userDb as LiveData<UserDb>


    fun getUser(): FirebaseUser? = firebaseRepository.getUser()

    fun getAuth(): FirebaseAuth = firebaseRepository.getAuth()

    sealed class DeleteAccountState {
        object Idle : DeleteAccountState()
        object Success : DeleteAccountState()
        object Loading : DeleteAccountState()
        object Failure : DeleteAccountState()
        object ReLoginRequired : DeleteAccountState()
    }

    private val _deleteState = MutableLiveData<DeleteAccountState>(DeleteAccountState.Idle)
    val deleteState: LiveData<DeleteAccountState> = _deleteState
    
    fun resetDeleteState() {
        _deleteState.value = DeleteAccountState.Idle
    }

    fun isGoogleAccount() = firebaseRepository.isGoogleAccount()

    fun deleteAccount() {
        _deleteState.postValue(DeleteAccountState.ReLoginRequired)
    }

    fun reauthenticateAndDelete(credential: AuthCredential) {
        _deleteState.value = DeleteAccountState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Reautentica o usuário com a credencial fornecida
                firebaseRepository.reauthenticateWithCredential(credential)

                // 2. Após a reautenticação bem-sucedida, limpa os dados do usuário no Firestore
                val dbDeleted = firebaseRepository.clearUserFirestoreData()

                if (dbDeleted) {
                    // 3. Se a limpeza dos dados for bem-sucedida, exclui a conta de autenticação
                    firebaseRepository.deleteUserAuth()
                    // 4. Força o logout para invalidar qualquer sessão local
                    getAuth().signOut()
                    _deleteState.postValue(DeleteAccountState.Success)
                } else {
                    // Se a limpeza do banco de dados falhar, não prosseguimos com a exclusão da conta
                    Log.e("reauthAndDelete", "Falha ao limpar os dados do usuário no Firestore.")
                    _deleteState.postValue(DeleteAccountState.Failure)
                }

            } catch (e: Exception) {
                Log.e("reauthAndDelete", "Erro durante a reautenticação e exclusão: ${e.message}", e)
                _deleteState.postValue(DeleteAccountState.Failure)
            }
        }
    }

    fun reauthenticateAndDelete(password: String) {
        val user = getUser() ?: return
        // Cria a credencial de email aqui
        val credential = EmailAuthProvider.getCredential(user.email!!, password)
        reauthenticateAndDelete(credential)
    }

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

    suspend fun getProducts(codes: List<String>): List<Product>? {
        var response : List<Product>? = null
        val productResponse = firebaseRepository.getProducts(codes)

        response = productResponse.mapNotNull { it.data?.toProduct(it.id) }

        return response
    }

    suspend fun getProduct(code: String): Product? {
        var product: Product? = null
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = firebaseRepository.getProduct(code)?.await()
                withContext(Dispatchers.Main) {
                    response?.let {
                        product = it.data?.toProduct(it.id)
                    }
                }
            } catch (e: Exception) {
                Log.e("getProduct", "Error: ${e.message}")
            }
        }.join()
        
        return product
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

    suspend fun delProducts(codes: List<String>): Boolean =
        runVoidTask(firebaseRepository.delProducts(codes))

    suspend fun delEntrys(ids: List<Int>): Boolean =
        runVoidTask(firebaseRepository.delEntrys(ids))

    suspend fun delExits(ids: List<Int>): Boolean =
        runVoidTask(firebaseRepository.deleteExits(ids))

    suspend fun addProduct(
        code: String, product: HashMap<String, Comparable<*>>
    ): Boolean = runVoidTask(firebaseRepository.addProduct(code, product))

    suspend fun updateProduct(
        code: String, product: HashMap<String, Comparable<*>>
    ): Boolean = runVoidTask(firebaseRepository.updateProduct(code, product))

    suspend fun addExitTransaction(
        products: List<Pair<String, Int>>,
        selectedProducts: List<Product>
    ): Boolean = runVoidTask(firebaseRepository.addExitTransaction(products, selectedProducts))

    suspend fun addEntryTransaction(
        products: List<Pair<String, Int>>,
        selectedProducts: List<Product>
    ): Boolean = runVoidTask(firebaseRepository.addEntryTransaction(products, selectedProducts))

    suspend fun hasProduct(code: String): Boolean {
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
            this.data.mapNotNull {
                it.data?.toProduct(it.id)
            }
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
