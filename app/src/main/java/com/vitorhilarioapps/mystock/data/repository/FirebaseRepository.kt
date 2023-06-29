package com.vitorhilarioapps.mystock.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.vitorhilarioapps.mystock.data.model.Product
import com.vitorhilarioapps.mystock.utils.getEntryTransactionMap
import com.vitorhilarioapps.mystock.utils.getExitTransactionMap
import com.vitorhilarioapps.mystock.utils.getMap
import com.vitorhilarioapps.mystock.utils.getSubTransactionMap
import com.vitorhilarioapps.mystock.utils.previousInfo

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

    fun getProducts(): Task<QuerySnapshot>? {
        val getProductsTask = user?.let {
            db.collection("users").document(it.uid)
                .collection("products").get()
        }

        return getProductsTask
    }

    fun getProducts(codes: List<Int>): Task<QuerySnapshot>? {
        val getProductsTask = user?.let {
            db.collection("users").document(it.uid)
                .collection("products")
                .whereIn("code", codes).get()
        }

        return getProductsTask
    }

    fun delProducts(codes: List<Int>): Task<Void>? {
        val delProductsRef = user?.let {
            db.collection("users").document(it.uid)
                .collection("products")
        }

        val delProductsTask = delProductsRef?.run {
            db.runBatch {
                codes.forEach { code ->
                    delProductsRef.document(code.toString()).delete()
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

    fun hasProduct(code: Int): Task<QuerySnapshot>? {
        val hasProductTask = user?.let {
            db.collection("users").document(it.uid)
                .collection("products")
                .whereEqualTo("code", code).get()
        }

        return hasProductTask
    }

    fun addProduct(code: Int, product: HashMap<String, Comparable<*>>): Task<Void>? {
        val addProductTask = user?.let {
            db.collection("users").document(it.uid)
                .collection("products").document(code.toString()).set(product)
        }

        return addProductTask
    }

    fun addEntryTransaction(
        products: List<Pair<Int, Int>>,
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

                        transactionPath.collection("products").document(code.toString())
                            .set(getSubTransactionMap(code, amount, subPrice))

                        productsPath.document(code.toString())
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
        products: List<Pair<Int, Int>>,
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

                        transactionPath.collection("products").document(code.toString())
                            .set(getSubTransactionMap(code, amount, subPrice))

                        productsPath.document(code.toString())
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