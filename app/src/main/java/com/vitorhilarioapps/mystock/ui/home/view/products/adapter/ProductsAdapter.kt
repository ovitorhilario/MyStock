package com.vitorhilarioapps.mystock.ui.home.view.products.adapter

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.vitorhilarioapps.mystock.R
import com.vitorhilarioapps.mystock.data.model.Product
import com.vitorhilarioapps.mystock.databinding.ProductItemBinding
import com.vitorhilarioapps.mystock.ui.home.view.products.model.ProductItem
import com.vitorhilarioapps.mystock.utils.moneyType

class ProductsAdapter(
    private val data: List<ProductItem>,
    private val addProduct: (Int) -> Boolean,
    private val removeProduct: (Int) -> Boolean,
    private val hasProduct: (Int) -> Boolean,
    private val listSize: () -> Int,
    private val resources: Resources
) : Adapter<ProductsAdapter.ProductHolder>() {

    private var expandedPosition: Int = -1
    private var previousExpandedPosition: Int = -1

    inner class ProductHolder(binding: ProductItemBinding) : ViewHolder(binding.root) {
        private val layoutProductItem = binding.root
        private val tvProductsName = binding.tvProductName
        private val tvProductSalePrice = binding.tvProductSalePrice
        private val tvProductPurchasePrice = binding.tvProductPurchasePrice
        private val tvProductAmount = binding.tvProductAmount

        private val tvProductCode = binding.tvProductCode
        private val tvProductDescription = binding.tvProductDescription
        private val tvProductWeight = binding.tvProductWeight
        private val tvProductCategory = binding.tvProductCategory

        private val ivProductDescription = binding.ivProductDescription
        private val ivProductWeight = binding.ivProductWeight
        private val ivProductCategory = binding.ivProductCategory

        private val ivProductIcon = binding.ivProductIcon
        private val containerProductSubInfo = binding.containerProductSubInfo
        private val btnProductShowSubInfo = binding.btnProductShowSubInfo

        private fun setupProduct(product: Product) {
            tvProductsName.text = product.name
            tvProductAmount.text = product.amount.toString()
            tvProductPurchasePrice.text = product.purchasePrice.moneyType()
            tvProductSalePrice.text = product.salePrice.moneyType()

            // DropDown Info

            tvProductCode.text = product.code.toString()
            tvProductDescription.text = product.description
            tvProductWeight.text = buildString { append(product.weight); append(product.weightType) }
            tvProductCategory.text = product.category

            if (product.category.isBlank() || product.category.isEmpty()) {
                ivProductCategory.visibility = View.GONE
                tvProductCategory.visibility = View.GONE
            } else {
                ivProductCategory.visibility = View.VISIBLE
                tvProductCategory.visibility = View.VISIBLE
            }

            if (product.weight.isNaN() || product.weight == 0.0) {
                ivProductWeight.visibility = View.GONE
                tvProductWeight.visibility = View.GONE
            } else {
                ivProductWeight.visibility = View.VISIBLE
                tvProductWeight.visibility = View.VISIBLE
            }

            if (product.description.isBlank() || product.description.isEmpty()) {
                tvProductDescription.visibility = View.GONE
                ivProductDescription.visibility = View.GONE
            } else {
                tvProductDescription.visibility = View.VISIBLE
                ivProductDescription.visibility = View.VISIBLE
            }
        }

        private fun setupProductClickListeners(
            productItem: ProductItem,
            position: Int,
            isExpanded: Boolean
        ) {
            val product = productItem.data

            btnProductShowSubInfo.setOnClickListener {
                expandedPosition = when (isExpanded) {
                    true -> -1
                    false -> position
                }

                notifyItemChanged(position)
                notifyItemChanged(previousExpandedPosition)
            }

            layoutProductItem.setOnClickListener {
                if (listSize() >= 1) {
                    if (hasProduct(product.code)) {
                        val success = removeProduct(product.code)

                        if (success) {
                            setUnselected()
                            productItem.isSelected = false
                        }
                    } else {
                        val success = addProduct(product.code)

                        if (success) {
                            setSelected()
                            productItem.isSelected = true
                        }
                    }
                }
            }

            layoutProductItem.setOnLongClickListener {
                if (listSize() == 0) {
                    val success = addProduct(product.code)

                    if (success) {
                        setSelected()
                        productItem.isSelected = true
                    }
                }

                true
            }
        }

        private fun setSelected() {
            ivProductIcon.setImageDrawable(resources.getDrawable(R.drawable.ic_confirm))
            ivProductIcon.setBackgroundDrawable(resources.getDrawable(R.drawable.short_cut_green_bg))
        }

        private fun setUnselected() {
            ivProductIcon.setImageDrawable(resources.getDrawable(R.drawable.ic_product))
            ivProductIcon.setBackgroundDrawable(resources.getDrawable(R.drawable.short_cut_yellow_bg))
        }

        private fun dropDownEnable() {
            containerProductSubInfo.visibility = View.VISIBLE
            btnProductShowSubInfo.setImageDrawable(resources.getDrawable(R.drawable.ic_arrow_up))
        }

        private fun dropDrownDisable() {
            containerProductSubInfo.visibility = View.GONE
            btnProductShowSubInfo.setImageDrawable(resources.getDrawable(R.drawable.ic_arrow_down))
        }

        fun bind(productItem: ProductItem, position: Int) {
            val product = productItem.data
            val isExpanded = expandedPosition == position
            setupProduct(product)
            setupProductClickListeners(productItem, position, isExpanded)

            if (productItem.isSelected) {
                setSelected()
            } else {
                setUnselected()
            }

            if (isExpanded) {
                dropDownEnable()
                previousExpandedPosition = position
            } else {
                dropDrownDisable()
            }
        }
    }

    override fun onCreateViewHolder(group: ViewGroup, viewType: Int): ProductHolder {
        return ProductHolder(
            ProductItemBinding.inflate(LayoutInflater.from(group.context), group, false)
        )
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ProductHolder, position: Int) {
        holder.bind(data[position], position)
    }
}