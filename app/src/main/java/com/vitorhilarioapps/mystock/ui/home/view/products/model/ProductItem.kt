package com.vitorhilarioapps.mystock.ui.home.view.products.model


import com.vitorhilarioapps.mystock.data.model.Product

data class ProductItem(
    val data: Product,
    var isSelected: Boolean = false
)
