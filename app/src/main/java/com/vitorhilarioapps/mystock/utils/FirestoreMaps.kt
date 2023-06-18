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

fun Product.previousInfo(amount: Int, price: Double) : String {
    return buildString {
        append(this@previousInfo.name.uppercase())
        append(" - ")
        append(amount);
        append("UN X ");
        append(String.format("%.2f", price));
        append("; ")
    }
}

fun getSubTransactionMap(code: Int, amount: Int, subPrice: Double): HashMap<String, Comparable<*>> {
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
    code: Int,
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

fun Map<String, Any>.toProduct(): Product {
    val timestamp = (this["registration_date"] as? Timestamp) ?: Timestamp.now()

    return Product(
        name = this["name"].toString(),
        code = this["code"].toString().toIntOrNull() ?: 0,
        amount = this["amount"].toString().toIntOrNull() ?: 0,
        salePrice = this["sale_price"].toString().toDoubleOrNull() ?: 0.0,
        purchasePrice = this["purchase_price"].toString().toDoubleOrNull() ?: 0.0,
        registrationDate = timestamp,
        category = this["category"].toString(),
        description = this["description"].toString(),
        weight = this["weight"].toString().toDoubleOrNull() ?: 0.0,
        weightType = this["weight_type"].toString(),
    )
}

fun Map<String, Any>.toExit(): Transaction.Exit {
    return Transaction.Exit(
        id = this["id"].toString().toIntOrNull() ?: 0,
        date = this["date"] as? Timestamp ?: Timestamp.now(),
        info = this["info"] as? ArrayList<String> ?: arrayListOf(),
        resultPrice = this["result_price"].toString().toDoubleOrNull() ?: 0.0
    )
}

fun Map<String, Any>.toUserDb(email: String, photoUrl: Uri?): UserDb {
    return UserDb(
        name = this["name"].toString(),
        email = email,
        photoUrl = photoUrl,
        id = this["id"].toString().toIntOrNull() ?: 0,
        since = this["date"] as? Timestamp ?: Timestamp.now(),
        premium = this["premium"].toString(),
    )
}

fun Map<String, Any>.toEntry(): Transaction.Entry {
    return Transaction.Entry(
        id = this["id"].toString().toIntOrNull() ?: 0,
        date = this["date"] as? Timestamp ?: Timestamp.now(),
        resultPrice = this["result_price"].toString().toDoubleOrNull() ?: 0.0,
        info = this["info"] as? ArrayList<String> ?: arrayListOf(),
        profitOnSale = this["profit_on_sale"].toString().toDoubleOrNull() ?: 0.0
    )
}