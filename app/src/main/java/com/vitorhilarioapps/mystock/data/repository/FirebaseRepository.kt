package com.vitorhilarioapps.mystock.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.firestore
import com.vitorhilarioapps.mystock.data.model.Product
import com.vitorhilarioapps.mystock.utils.getEntryTransactionMap
import com.vitorhilarioapps.mystock.utils.getExitTransactionMap
import com.vitorhilarioapps.mystock.utils.getMap
import com.vitorhilarioapps.mystock.utils.getSubTransactionMap
import com.vitorhilarioapps.mystock.utils.previousInfo
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val user = auth.currentUser

    fun getUser(): FirebaseUser? = user

    fun getAuth(): FirebaseAuth = auth

    /*---------------------
    |    Firebase Auth    |
    ---------------------*/

    fun isAuthenticated(): Boolean = user != null && user.isEmailVerified

    fun createUser(email: String, password: String): Task<AuthResult> {
        val createUserTask = auth.createUserWithEmailAndPassword(email, password)
        return createUserTask
    }
    fun signIn(email: String, password: String): Task<AuthResult> {
        val signInTask = auth.signInWithEmailAndPassword(email, password)
        return signInTask
    }

    fun sendPasswordResetEmail(email: String): Task<Void> {
        val restoreTask = auth.sendPasswordResetEmail(email)
        return restoreTask
    }

    fun sendEmailVerification(user: FirebaseUser): Task<Void> {
        val sendEmailTask = user.sendEmailVerification()
        return sendEmailTask
    }

    fun signInWithCredential(credential: AuthCredential): Task<AuthResult> {
        val signInTask = auth.signInWithCredential(credential)
        return signInTask
    }

    /*-----------------
    |    Firestore    |
    -----------------*/

    fun createDbForUser(user: FirebaseUser): Task<Void> {
        val createTask = db.collection("users")
            .document(user.uid).set(user.getMap())

        return createTask
    }

    fun hasUserInDb(userId: String): Task<QuerySnapshot> {
        val getUserTask = db.collection("users")
            .whereEqualTo("id", userId).get()

        return getUserTask
    }

    fun getUserFromDb(): Task<DocumentSnapshot>? {
        val getUserTask = user?.let {
            db.collection("users")
                .document(it.uid).get()
        }

        return getUserTask
    }

    suspend fun clearUserFirestoreData(): Boolean {
        val uid = user?.uid ?: return false

        return try {
            val batch = db.batch()
            val userDocRef = db.collection("users").document(uid)

            // 1. Coletar e agendar deleção de PRODUTOS
            val productsSnapshot = userDocRef.collection("products").get().await()
            for (doc in productsSnapshot.documents) {
                batch.delete(doc.reference)
            }

            // 2. Coletar e agendar deleção de ENTRADAS
            val entrysSnapshot = userDocRef.collection("entrys").get().await()
            for (doc in entrysSnapshot.documents) {
                batch.delete(doc.reference)
            }

            // 3. Coletar e agendar deleção de SAÍDAS
            val exitsSnapshot = userDocRef.collection("exits").get().await()
            for (doc in exitsSnapshot.documents) {
                batch.delete(doc.reference)
            }

            // 4. Agendar deleção do DOCUMENTO DO USUÁRIO
            batch.delete(userDocRef)

            // 5. Executar tudo de uma vez
            batch.commit().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteUserAuth() {
        user?.delete()?.await()
    }

    suspend fun reauthenticateWithCredential(credential: AuthCredential) {
        val user = auth.currentUser ?: return
        user.reauthenticate(credential).await()
    }

    fun isGoogleAccount(): Boolean {
        // Verifica se algum dos providers vinculados é google.com
        return user?.providerData?.any { it.providerId == "google.com" } ?: false
    }

    fun getProducts(): Task<QuerySnapshot>? {
        val getProductsTask = user?.let {
            db.collection("users").document(it.uid)
                .collection("products").get()
        }

        return getProductsTask
    }

    suspend fun getProducts(codes: List<String>): List<DocumentSnapshot> {
        val uid = user?.uid ?: return emptyList()
        
        if (codes.isEmpty()) {
            return emptyList()
        }
        
        val productsRef = db.collection("users")
            .document(uid)
            .collection("products")
        
        return try {
            val results = mutableListOf<DocumentSnapshot>()

            codes.chunked(10).forEach { chunk ->
                val snapshot = productsRef
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()
                results.addAll(snapshot.documents)
            }
            
            results
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getProduct(code: String): Task<DocumentSnapshot>? {
        val getProductTask = user?.let {
            db.collection("users").document(it.uid)
                .collection("products")
                .document(code).get()
        }

        return getProductTask
    }

    fun delProducts(codes: List<String>): Task<Void>? {
        val delProductsRef = user?.let {
            db.collection("users").document(it.uid)
                .collection("products")
        }

        val delProductsTask = delProductsRef?.run {
            db.runBatch {
                codes.forEach { code ->
                    delProductsRef.document(code).delete()
                }
            }
        }

        return delProductsTask
    }

    fun delEntrys(ids: List<Int>): Task<Void>? {
        val entrysRef = user?.let {
            db.collection("users").document(it.uid)
                .collection("entrys")
        }

        val delEntrysTask = entrysRef?.run {
            db.runBatch {
                ids.forEach { id ->
                    entrysRef.document(id.toString()).delete()
                }
            }
        }

        return delEntrysTask
    }

    fun deleteExits(ids: List<Int>): Task<Void>? {
        val exitsRef = user?.let {
            db.collection("users").document(it.uid)
                .collection("exits")
        }

        val delExitsTask = exitsRef?.run {
            db.runBatch {
                ids.forEach { id ->
                    exitsRef.document(id.toString()).delete()
                }
            }
        }

        return delExitsTask
    }

    fun getEntrys(): Task<QuerySnapshot>? {
        val getEntrysTask = user?.let {
            db.collection("users").document(it.uid)
                .collection("entrys").get()
        }

        return getEntrysTask
    }

    fun getExits(): Task<QuerySnapshot>? {
        val getExitsTask = user?.let {
            db.collection("users").document(it.uid)
                .collection("exits").get()
        }

        return getExitsTask
    }

    fun hasProduct(code: String): Task<QuerySnapshot>? {
        val hasProductTask = user?.let {
            db.collection("users").document(it.uid)
                .collection("products")
                .whereEqualTo("code", code).get()
        }

        return hasProductTask
    }

    fun addProduct(code: String, product: HashMap<String, Comparable<*>>): Task<Void>? {
        val addProductTask = user?.let {
            db.collection("users").document(it.uid)
                .collection("products").document(code).set(product)
        }

        return addProductTask
    }

    fun updateProduct(code: String, product: HashMap<String, Comparable<*>>): Task<Void>? {
        val updateProductTask = user?.let {
            db.collection("users").document(it.uid)
                .collection("products").document(code).update(product as Map<String, Any>)
        }

        return updateProductTask
    }

    fun addEntryTransaction(
        products: List<Pair<String, Int>>,
        selectedProducts: List<Product>
    ): Task<Void>? {

        val currentDate = Timestamp.now()
        var resultPrice = 0.0
        var sumPurchasePrice = 0.0
        val infoList = arrayListOf<String>()

        val transactionTask = user?.run {
            val transactionPath = db.collection("users").document(uid)
                .collection("entrys").document(currentDate.seconds.toString())

            val productsPath = db.collection("users").document(uid)
                .collection("products")

            db.runBatch { batch ->
                products.forEach { (code, amount) ->
                    selectedProducts.firstOrNull { it.code == code }?.let { product ->
                        infoList.add(product.previousInfo(amount, product.salePrice))

                        val subPrice = product.salePrice * amount
                        sumPurchasePrice += product.purchasePrice * amount
                        resultPrice += subPrice

                        transactionPath.collection("products").document(code)
                            .set(getSubTransactionMap(code, amount, subPrice))

                        productsPath.document(code)
                            .update("amount", FieldValue.increment(-amount.toLong()))
                    }
                }

                val profitOnSale = resultPrice - sumPurchasePrice


                transactionPath.set(
                    getEntryTransactionMap(currentDate.seconds.toInt(), currentDate, infoList, resultPrice, profitOnSale)
                )
            }
        }

        return transactionTask
    }

    fun addExitTransaction(
        products: List<Pair<String, Int>>,
        selectedProducts: List<Product>
    ): Task<Void>? {

        val currentDate = Timestamp.now()
        var resultPrice = 0.0
        val infoList = arrayListOf<String>()

        val transactionTask = user?.run {
            val transactionPath = db.collection("users").document(uid)
                .collection("exits").document(currentDate.seconds.toString())

            val productsPath = db.collection("users").document(uid)
                .collection("products")

            db.runBatch { batch ->
                products.forEach { (code, amount) ->
                    selectedProducts.firstOrNull { it.code == code }?.let { product ->
                        infoList.add(product.previousInfo(amount, product.purchasePrice))

                        val subPrice = product.purchasePrice * amount
                        resultPrice += subPrice

                        transactionPath.collection("products").document(code)
                            .set(getSubTransactionMap(code, amount, subPrice))

                        productsPath.document(code)
                            .update("amount", FieldValue.increment(amount.toLong()))
                    }
                }

                transactionPath.set(
                    getExitTransactionMap(currentDate.seconds.toInt(), currentDate, infoList, resultPrice)
                )
            }
        }

        return transactionTask
    }
}