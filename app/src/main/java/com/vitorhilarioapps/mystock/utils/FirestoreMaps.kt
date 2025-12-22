package com.vitorhilarioapps.mystock.utils

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.vitorhilarioapps.mystock.data.model.Product
import com.vitorhilarioapps.mystock.data.model.Transaction
import com.vitorhilarioapps.mystock.data.model.UserDb
import java.util.Date

fun getExitTransactionMap(
    id: Int,
    date: Timestamp,
    info: ArrayList<String>,
    resultPrice: Double
): HashMap<String, Any> {
    return hashMapOf(
        "id" to id,
        "date" to date,
        "info" to info,
        "result_price" to resultPrice,
    )
}

fun getEntryTransactionMap(
    id: Int,
    date: Timestamp,
    info: ArrayList<String>,
    resultPrice: Double,
    profitOnSale: Double
): HashMap<String, Any> {
    return hashMapOf(
        "id" to id,
        "date" to date,
        "info" to info,
        "result_price" to resultPrice,
        "profit_on_sale" to profitOnSale
    )
}

fun Product.previousInfo(amount: Int, price: Double): String {
    return buildString {
        append(this@previousInfo.name.uppercase())
        append(" - ")
        append(amount)
        append("UN X ")
        append(String.format("%.2f", price))
        append("; ")
    }
}

fun getSubTransactionMap(code: String, amount: Int, subPrice: Double): HashMap<String, Comparable<*>> {
    return hashMapOf(
        "product_code" to code,
        "amount" to amount,
        "sub_price" to subPrice
    )
}

fun FirebaseUser.getMap(): HashMap<String, Comparable<*>?> {
    return hashMapOf(
        "id" to this.uid,
        "name" to this.displayName,
        "premium" to false,
        "balance" to 0.0,
        "since" to Timestamp(Date())
    )
}

fun getProductMap(
    code: String,
    name: String,
    amount: Int,
    purchasePrice: Double,
    salePrice: Double,
    description: String,
    category: String,
    weight: Double,
    weightType: String
): HashMap<String, Comparable<*>> {
    return hashMapOf(
        "code" to code,
        "name" to name,
        "registration_date" to Timestamp.now(),
        "category" to category,
        "amount" to amount,
        "description" to description,
        "purchase_price" to purchasePrice,
        "sale_price" to salePrice,
        "weight" to weight,
        "weight_type" to weightType,
    )
}

fun Map<String, Any>.toProduct(id: String): Product {
    return Product(
        name = this["name"] as? String ?: "",
        code = id,
        amount = (this["amount"] as? Number)?.toInt() ?: 0,
        salePrice = (this["sale_price"] as? Number)?.toDouble() ?: 0.0,
        purchasePrice = (this["purchase_price"] as? Number)?.toDouble() ?: 0.0,
        registrationDate = (this["registration_date"] as? Timestamp) ?: Timestamp.now(),
        category = this["category"] as? String ?: "",
        description = this["description"] as? String ?: "",
        weight = (this["weight"] as? Number)?.toDouble() ?: 0.0,
        weightType = this["weight_type"] as? String ?: "",
    )
}

fun Map<String, Any>.toExit(): Transaction.Exit {
    return Transaction.Exit(
        id = (this["id"] as? Number)?.toInt() ?: 0,
        date = (this["date"] as? Timestamp) ?: Timestamp.now(),
        info = (this["info"] as? ArrayList<*>)?.filterIsInstance<String>() as? ArrayList<String> ?: arrayListOf(),
        resultPrice = (this["result_price"] as? Number)?.toDouble() ?: 0.0
    )
}

fun Map<String, Any>.toUserDb(email: String, photoUrl: Uri?): UserDb {
    return UserDb(
        name = this["name"] as? String ?: "",
        email = email,
        photoUrl = photoUrl,
        id = this["id"] as? String ?: "",
        since = (this["since"] as? Timestamp) ?: Timestamp.now(),
        premium = (this["premium"] as? Boolean ?: false).toString(),
    )
}

fun Map<String, Any>.toEntry(): Transaction.Entry {
    return Transaction.Entry(
        id = (this["id"] as? Number)?.toInt() ?: 0,
        date = (this["date"] as? Timestamp) ?: Timestamp.now(),
        resultPrice = (this["result_price"] as? Number)?.toDouble() ?: 0.0,
        info = (this["info"] as? ArrayList<*>)?.filterIsInstance<String>() as? ArrayList<String> ?: arrayListOf(),
        profitOnSale = (this["profit_on_sale"] as? Number)?.toDouble() ?: 0.0
    )
}
